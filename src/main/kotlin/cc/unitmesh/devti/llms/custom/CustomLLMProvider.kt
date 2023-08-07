package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.llms.CodeCopilotProvider
import cc.unitmesh.devti.prompting.model.CustomPromptConfig
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.time.Duration


@Serializable
data class CustomRequest(val instruction: String, val input: String)


@Service(Service.Level.PROJECT)
class CustomLLMProvider(val project: Project) : CodeCopilotProvider {
    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private val url = autoDevSettingsState.customEngineServer
    private val key = autoDevSettingsState.customEngineToken
    private var customPromptConfig: CustomPromptConfig? = null
    private var client = OkHttpClient()
    private val timeout = Duration.ofSeconds(600)

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
        val requestContent = Json.encodeToString<CustomRequest>(CustomRequest(promptText, ""))
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

        return callbackFlow {
            val listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    println("onOpen")
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    println(data)
                    trySend(data)
                }

                override fun onClosed(eventSource: EventSource) {

                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    close()
                }
            }

            val eventSource = EventSources.createFactory(client).newEventSource(request, listener)

            awaitClose {
                eventSource.cancel()
            }
        }
    }

    fun prompt(instruction: String, input: String): String {
        // encode the request as JSON with kotlinx.serialization
        val requestContent = Json.encodeToString(CustomRequest(instruction, input))
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

    override fun autoComment(text: String): String {
        val comment = customPromptConfig!!.autoComment
        return prompt(comment.instruction, comment.input.replace("{code}", text))
    }

    override fun findBug(text: String): String {
        val bug = customPromptConfig!!.refactor
        return prompt(bug.instruction, bug.input.replace("{code}", text))
    }

}