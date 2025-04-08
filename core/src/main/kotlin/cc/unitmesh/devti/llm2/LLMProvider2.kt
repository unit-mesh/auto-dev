package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llms.custom.CustomRequest
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.llms.custom.appendCustomHeaders
import cc.unitmesh.devti.llms.custom.updateCustomFormat
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.util.AutoDevAppScope
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.time.Duration

/**
 * LLMProvider provide only session-free interfaces
 *
 * It's LLMProvider's responsibility to maintain the network connection
 * But the chat session is maintained by the client
 *
 * [LLMProvider2] provides a factory companion object to create different providers
 *
 * for now, we only support text completion, see [DefaultLLMTextProvider].
 * you can implement your own provider by extending [LLMProvider2]
 * and override [textComplete] method
 *
 * ```kotlin
 * val provider = LLMProvider2()
 * val session = ChatSession("sessionName")
 * // if you don't need to maintain the history, you can ignore the session
 * // stream is default to true
 * provider.request("text", session = session, stream = true).catch {
 *   // handle errors
 * }.collect {
 *    // incoming new message without the original history messages
 * }
 * ```
 *
 * @property project if not null means this is a project level provider, will be disposed when project closed
 */
abstract class LLMProvider2 protected constructor(
    protected val project: Project?,
    protected val logger: Logger = logger<LLMProvider2>(),
    protected val httpClient: OkHttpClient = OkHttpClient(),
) {
    /**
     * The job that is sending the request
     */
    protected var _sendingJob: Job? = null

    /**
     * 为会话创建一个 CoroutineScope
     *
     * TODO: 支持同一 session 同时发送多个请求
     */
    protected fun sessionScope(session: ChatSession<Message>): CoroutineScope {
        return if (project != null) {
            AutoDevCoroutineScope.scope(project)
        } else {
            AutoDevAppScope.scope()
        }
    }


    /**
     * Send a text to the LLM
     * response will be emitted to [response]
     *
     * return a job that can be used to cancel the request
     */
    suspend fun request(
        text: Message,
        stream: Boolean = true,
        session: ChatSession<Message> = ChatSession("ignoreHistorySession"),
    ): Flow<SessionMessageItem<Message>> {
        cancelCurrentRequest(session)
        return textComplete(session.appendMessage(text), stream)
    }

    /**
     * 根据 session 中的 chatHistory 发送请求，在数据触发过程中通过 [response] 发送接收的消息，并在最终完成后返回完整的 [SessionMessageItem]
     */
    protected abstract fun textComplete(
        session: ChatSession<Message>,
        stream: Boolean = true,
    ): Flow<SessionMessageItem<Message>>

    /**
     * 同步取消当前请求，并将等待请求完成
     */
    suspend fun cancelCurrentRequest(session: ChatSession<Message>) {
        _sendingJob?.cancelAndJoin()
    }

    /**
     * 取消当前请求，本 api 不会等待请求完成
     */
    fun cancelCurrentRequestSync() {
        _sendingJob?.cancel()
    }

    companion object {

        /**
         * 返回在配置中设置的 provider
         */
        operator fun invoke(autoDevSettingsState: AutoDevSettingsState = AutoDevSettingsState.getInstance()): LLMProvider2 =
            LLMProvider2(
                requestUrl = autoDevSettingsState.customEngineServer,
                authorizationKey = autoDevSettingsState.customEngineToken,
                responseResolver = autoDevSettingsState.customEngineResponseFormat,
                requestCostomize = autoDevSettingsState.customEngineRequestFormat,
            )

        /**
         * 默认返回兼容 OpenAI 的 provider
         */
        operator fun invoke(
            requestUrl: String,
            authorizationKey: String,
            responseResolver: String = "\$.choices[0].delta.content",
            requestCostomize: String = "",
        ): LLMProvider2 = DefaultLLMTextProvider(
            requestUrl = requestUrl,
            authorizationKey = authorizationKey,
            responseResolver = responseResolver,
            requestCustomize = requestCostomize,
        )

        /**
         * Ollama Provider，不需提供 authorizationKey
         */
        fun Ollama(
            modelName: String,
            requestUrl: String = "http://localhost:11434/v1/chat/completions",
            responseResolver: String = "\$.choices[0].delta.content",
        ): LLMProvider2 = LLMProvider2(
            requestUrl = requestUrl,
            authorizationKey = "",
            responseResolver = responseResolver,
            requestCostomize = """{ "customFields": {"model": "$modelName", "temperature": 0.7 } }""",
        )
    }
}

private class DefaultLLMTextProvider(
    private val requestUrl: String,
    private val authorizationKey: String,
    private val responseResolver: String = "\$.choices[0].delta.content",
    private val requestCustomize: String = "{}",
    project: Project? = null,
) : LLMProvider2(project) {

    override fun textComplete(session: ChatSession<Message>, stream: Boolean): Flow<SessionMessageItem<Message>> {
        val client = httpClient.newBuilder().readTimeout(Duration.ofSeconds(30)).build()
        val requestBuilder = Request.Builder().apply {
            if (authorizationKey.isNotEmpty()) {
                addHeader("Authorization", "Bearer $authorizationKey")
            }
            appendCustomHeaders(requestCustomize)
        }
        val customRequest = CustomRequest(session.chatHistory.map {
            val cm = it.chatMessage
            Message(cm.role, cm.content ?: "")
        })
        val requestBodyText = customRequest.updateCustomFormat(requestCustomize)
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), requestBodyText.toByteArray())
        println("requestUrl: $requestUrl")
        val request: Request = requestBuilder.url(requestUrl).post(requestBody).build()

        return callbackFlow {
            _sendingJob = sessionScope(session).launch {
                if (stream) {
                    sseStream(
                        client,
                        request,
                        onFailure = {
                            close(it)
                        },
                        onClosed = {
                            close()
                        },
                        onEvent = {
                            trySend(it)
                        }
                    )
                } else {
                    kotlin.runCatching {
                        val result = directResult(client, request)
                        trySend(result)
                        close()
                    }.onFailure {
                        close(it)
                    }
                }
            }
            awaitClose()
        }
    }

    private fun sseStream(
        client: OkHttpClient,
        request: Request,
        onEvent: (SessionMessageItem<Message>) -> Unit,
        onFailure: (Throwable) -> Unit,
        onClosed: () -> Unit,
        onOpen: () -> Unit = {},
    ) {
        val factory = EventSources.createFactory(client)
        var result = ""
        factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                super.onEvent(eventSource, id, type, data)
                if (data == "[DONE]") {
                    return
                }
                val chunk: String = JsonPath.parse(data)?.read(responseResolver) ?: run {
                    // in some case, the response maybe not equal to our response format, so we need to ignore it
                    // {"id":"cmpl-ac26a17e","object":"chat.completion.chunk","created":1858403,"model":"yi-34b-chat","choices":[{"delta":{"role":"assistant"},"index":0}],"content":"","lastOne":false}
                    logger.warn(IllegalStateException("cannot parse with responseResolver: ${responseResolver}, ori data: $data"))
                    ""
                }
                result += chunk
                onEvent(SessionMessageItem(Message("system", result)))
            }

            override fun onClosed(eventSource: EventSource) {
                if (result.isEmpty()) {
                    onFailure(IllegalStateException("response is empty"))
                }
                onClosed()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                onFailure(t ?: RuntimeException("${response?.code} ${response?.message} ${response?.body?.string()}"))
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                onOpen()
            }
        })
    }

    private fun directResult(client: OkHttpClient, request: Request): SessionMessageItem<Message> {
        client.newCall(request).execute().use {
            val body = it.body
            if (body == null) {
                logger.error("response body is null")
                return SessionMessageItem(Message("system", "response body is null"))
            } else {
                val content = body.string()
                val result: String = JsonPath.parse(content)?.read(responseResolver) ?: run {
                    throw IllegalStateException("cannot parse with responseResolver: ${responseResolver}, ori data: $content")
                }
                return SessionMessageItem(Message("system", result))
            }
        }
    }
}
