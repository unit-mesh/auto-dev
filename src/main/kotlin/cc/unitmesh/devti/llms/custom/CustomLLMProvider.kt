package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.custom.CustomPromptConfig
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.service.SSE
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.time.Duration

@Serializable
data class Message(val role: String, val content: String)

@Service(Service.Level.PROJECT)
class CustomLLMProvider(val project: Project) : LLMProvider {
    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private val url = autoDevSettingsState.customEngineServer
    private val key = autoDevSettingsState.customEngineToken
    private var customPromptConfig: CustomPromptConfig? = null
    private var client = OkHttpClient()
    private val timeout = Duration.ofSeconds(600)
    private val messages: MutableList<Message> = ArrayList()

    init {
        val prompts = autoDevSettingsState.customEnginePrompts
        customPromptConfig = CustomPromptConfig.tryParse(prompts)
    }

    private val logger = logger<CustomLLMProvider>()

    override fun prompt(promptText: String): String {
        return this.prompt(promptText, "")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun stream(promptText: String, systemPrompt: String): Flow<String> {
        messages += Message(promptText, systemPrompt)

        val requestContent = Json.encodeToString<List<Message>>(messages)
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent)
        logger.warn("Requesting from $body")

        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
        }
        client = client.newBuilder()
            .readTimeout(timeout)
            .build()
        val request = builder
            .url(url)
            .post(body)
            .build()

        val call = client.newCall(request)
        val emitDone = false

        val sseFlowable = Flowable
            .create({ emitter: FlowableEmitter<SSE> ->
                call.enqueue(cc.unitmesh.devti.llms.azure.ResponseBodyCallback(emitter, emitDone))
            }, BackpressureStrategy.BUFFER)

        try {
            return callbackFlow {
                withContext(Dispatchers.IO) {
                    sseFlowable
                        .doOnError(Throwable::printStackTrace)
                        .blockingForEach { sse ->
                            val result: ChatCompletionResult =
                                ObjectMapper().readValue(sse!!.data, ChatCompletionResult::class.java)
                            val completion = result.choices[0].message
                            if (completion != null && completion.content != null) {
                                trySend(completion.content)
                            }
                        }

                    close()
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to stream", e)
            return callbackFlow {
                close()
            }
        }
    }

    fun prompt(instruction: String, input: String): String {
        messages += Message(instruction, input)
        val requestContent = Json.encodeToString<List<Message>>(messages)
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent)

        logger.warn("Requesting from $body")
        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
        }

        try {
            client = client.newBuilder()
                .readTimeout(timeout)
                .build()

            val request = builder
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.error("$response")
                return ""
            }

            return response.body?.string() ?: ""
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to set timeout", e)
            return ""
        }
    }
}