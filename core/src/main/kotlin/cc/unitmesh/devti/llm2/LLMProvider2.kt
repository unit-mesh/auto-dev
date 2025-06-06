package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llms.custom.CustomRequest
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.llms.custom.Usage
import cc.unitmesh.devti.llms.custom.appendCustomHeaders
import cc.unitmesh.devti.llms.custom.updateCustomFormat
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.util.AutoDevAppScope
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.*
import java.time.Duration

/**
 * LLMProvider2 is an abstract class that provides a base implementation for LLM (Large Language Model) providers.
 * It handles the communication with LLM services and manages the streaming of responses.
 *
 * @property project if not null means this is a project level provider,
 *    will be disposed when project closed
 */
abstract class LLMProvider2 protected constructor(
    protected val project: Project?,
    protected val requestCustomize: String = "{}",
    protected val responseResolver: String = "\$.choices[0].delta.content",
    protected val logger: Logger = logger<LLMProvider2>(),
    protected val httpClient: OkHttpClient = OkHttpClient(),
) {
    /** The job that is sending the request */
    protected var _sendingJob: Job? = null

    /** The current EventSource for SSE streaming */
    protected var _currentEventSource: EventSource? = null

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

    protected fun sseStream(
        client: OkHttpClient,
        request: Request,
        onEvent: (SessionMessageItem<Message>) -> Unit,
        onFailure: (Throwable) -> Unit,
        onClosed: () -> Unit,
        onOpen: () -> Unit = {},
    ) {
        val factory = EventSources.createFactory(client)
        var result = ""
        var sessionId: String? = null

        val eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                super.onEvent(eventSource, id, type, data)
                if (data == "[DONE]") {
                    return
                }

                if (sessionId == null) {
                    sessionId = tryExtractSessionId(data)
                }

                tryParseAndNotifyTokenUsage(data, sessionId)

                val chunk: String = runCatching {
                    val result: String? = JsonPath.parse(data)?.read(responseResolver)
                    result ?: ""
                }.getOrElse {
                    logger.warn(IllegalStateException("cannot parse with responseResolver: ${responseResolver}, ori data: $data"))
                    ""
                }

                result += chunk
                onEvent(SessionMessageItem(Message("system", result)))
            }

            override fun onClosed(eventSource: EventSource) {
                _currentEventSource = null
                if (result.isEmpty()) {
                    onFailure(IllegalStateException("response is empty"))
                }
                onClosed()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                _currentEventSource = null
                onFailure(
                    t ?: RuntimeException("error: ${response?.code} ${response?.message} ${response?.body?.string()}")
                )
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                onOpen()
            }
        })

        // Store the EventSource reference so we can cancel it later
        _currentEventSource = eventSource
    }

    /**
     * Try to parse token usage data from SSE response and notify listeners
     *
     * @param data The raw SSE data string
     * @param sessionId The session ID if available
     */
    private fun tryParseAndNotifyTokenUsage(data: String, sessionId: String?) {
        try {
            val usageData: Usage? = runCatching {
                JsonPath.parse(data)?.read<Map<String, Any>>("\$.usage")?.let { usageMap ->
                    Usage(
                        promptTokens = (usageMap["prompt_tokens"] as? Number)?.toLong() ?: 0,
                        completionTokens = (usageMap["completion_tokens"] as? Number)?.toLong() ?: 0,
                        totalTokens = (usageMap["total_tokens"] as? Number)?.toLong() ?: 0
                    )
                }
            }.getOrNull()

            val model: String? = runCatching {
                JsonPath.parse(data)?.read<String>("\$.model")
            }.getOrNull()

            usageData?.let { usage ->
                val tokenUsageEvent = TokenUsageEvent(
                    usage = usage,
                    model = model,
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis()
                )
                ApplicationManager.getApplication().messageBus
                    .syncPublisher(TokenUsageListener.TOPIC)
                    .onTokenUsage(tokenUsageEvent)
                logger.info("Token usage event published: prompt=${usage.promptTokens}, completion=${usage.completionTokens}, total=${usage.totalTokens}")
            }
        } catch (e: Exception) {
            // Silently ignore parsing errors for usage data since it's optional
            logger.debug("Failed to parse token usage from response data", e)
        }
    }

    /**
     * Try to extract session ID from response data
     *
     * @param data The raw SSE data string
     * @return The session ID if found, null otherwise
     */
    private fun tryExtractSessionId(data: String): String? {
        return runCatching {
            JsonPath.parse(data)?.read<String>("\$.id")
        }.getOrNull()
    }

    protected fun directResult(client: OkHttpClient, request: Request): SessionMessageItem<Message> {
        client.newCall(request).execute().use {
            val body = it.body
            if (!it.isSuccessful) {
                throw IllegalStateException("request failed ${it.code} ${it.body?.string()}")
            }

            if (body == null) {
                logger.error("response body is null")
                return SessionMessageItem(Message("system", "response body is null"))
            } else {
                val content = body.string()
                logger.info("response: $content")
                val result: String = runCatching<String> {
                    val result: String? = JsonPath.parse(content)?.read(responseResolver)
                    result
                        ?: throw java.lang.IllegalStateException("cannot parse with responseResolver: ${responseResolver}, ori data: $content")
                }.getOrElse {
                    throw IllegalStateException("cannot parse with responseResolver: ${responseResolver}, ori data: $content")
                }

                return SessionMessageItem(Message("system", result))
            }
        }
    }

    /**
     * Send a text to the LLM response will be emitted to [response]
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
     * 根据 session 中的 chatHistory 发送请求，在数据触发过程中通过 [response]
     * 发送接收的消息，并在最终完成后返回完整的 [SessionMessageItem]
     */
    protected abstract fun textComplete(
        session: ChatSession<Message>,
        stream: Boolean = true,
    ): Flow<SessionMessageItem<Message>>

    /** 同步取消当前请求，并将等待请求完成 */
    suspend fun cancelCurrentRequest(session: ChatSession<Message>) {
        _sendingJob?.cancelAndJoin()
        _currentEventSource?.cancel()
        _currentEventSource = null
    }

    /** 取消当前请求，本 api 不会等待请求完成 */
    fun cancelCurrentRequestSync() {
        _sendingJob?.cancel()
        _currentEventSource?.cancel()
        _currentEventSource = null
    }

    companion object {
        /** 返回在配置中设置的 provider */
        operator fun invoke(autoDevSettingsState: AutoDevSettingsState = AutoDevSettingsState.getInstance()): LLMProvider2 =
            LLMProvider2(
                requestUrl = autoDevSettingsState.customEngineServer,
                authorizationKey = autoDevSettingsState.customEngineToken,
                responseResolver = autoDevSettingsState.customEngineResponseFormat,
                requestCustomize = autoDevSettingsState.customEngineRequestFormat,
            )

        /** 默认返回兼容 OpenAI 的 provider */
        operator fun invoke(
            requestUrl: String,
            authorizationKey: String,
            responseResolver: String = "\$.choices[0].delta.content",
            requestCustomize: String = "",
        ): LLMProvider2 = DefaultLLMTextProvider(
            requestUrl = requestUrl,
            authorizationKey = authorizationKey,
            responseResolver = responseResolver,
            requestCustomize = requestCustomize,
        )

        /** Ollama Provider，不需提供 authorizationKey */
        fun Ollama(
            modelName: String,
            requestUrl: String = "http://localhost:11434/v1/chat/completions",
            responseResolver: String = "\$.choices[0].delta.content",
        ): LLMProvider2 = LLMProvider2(
            requestUrl = requestUrl,
            authorizationKey = "",
            responseResolver = responseResolver,
            requestCustomize = """{ "customFields": {"model": "$modelName", "temperature": 0.7 } }""",
        )

        fun GithubCopilot(
            modelName: String? = null,
            stream: Boolean = true,
            project: Project? = null,
        ): LLMProvider2 {
            // Use the provided model name or get the selected model ID from the settings state
            val settings = AutoDevSettingsState.getInstance()
            val actualModelName = modelName ?: settings.selectedCompletionModelId.takeIf { it.isNotEmpty() } ?: "gpt-4"
            println("Using model: $actualModelName")

            return GithubCopilotProvider(
                responseResolver = if (stream) "\$.choices[0].delta.content" else "\$.choices[0].message.content",
                requestCustomize = """{"customFields": {
                    "model": "$actualModelName",
                    "intent": false,
                    "n": 1,
                    "temperature": 0.1,
                    "stream": ${if (stream) "true" else "false"}
                }}
                """.trimIndent(),
                project = project,
            )
        }

        /**
         * Create a provider based on LlmConfig
         * Automatically detects if it's a GitHub model and creates the appropriate provider
         */
        fun fromConfig(llmConfig: cc.unitmesh.devti.llm2.model.LlmConfig, project: Project? = null): LLMProvider2 {
            // Check if this is a GitHub Copilot model
            if (llmConfig.url == "https://api.githubcopilot.com/chat/completions") {
                return GithubCopilotProvider(
                    requestCustomize = llmConfig.toLegacyRequestFormat(),
                    responseResolver = llmConfig.getResponseFormatByStream(),
                    project = project
                )
            }

            // For custom models, use the default provider
            return LLMProvider2(
                requestUrl = llmConfig.url,
                authorizationKey = llmConfig.auth.token,
                responseResolver = llmConfig.responseFormat,
                requestCustomize = llmConfig.toLegacyRequestFormat()
            )
        }
    }
}

private class DefaultLLMTextProvider(
    private val requestUrl: String,
    private val authorizationKey: String,
    requestCustomize: String = "{}",
    responseResolver: String = "\$.choices[0].delta.content",
    project: Project? = null,
) : LLMProvider2(project, requestCustomize, responseResolver) {

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
            Message(cm.role, cm.content)
        })

        val requestBodyText = customRequest.updateCustomFormat(requestCustomize)
        val content = requestBodyText.toByteArray()
        val requestBody = content.toRequestBody("application/json".toMediaTypeOrNull(), 0, content.size)
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
}