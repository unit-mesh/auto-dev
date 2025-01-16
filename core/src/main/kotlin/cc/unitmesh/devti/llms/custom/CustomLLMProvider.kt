package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.time.Duration

@Service(Service.Level.PROJECT)
class CustomLLMProvider(val project: Project) : LLMProvider, CustomSSEProcessor(project) {
    private val autoDevSettingsState = getSetting()
    private fun getSetting() = AutoDevSettingsState.getInstance()
    private val url get() = autoDevSettingsState.customEngineServer
    private val key get() = autoDevSettingsState.customEngineToken

    private val modelName: String
        get() = AutoDevSettingsState.getInstance().customModel

    override val requestFormat: String
        get() = autoDevSettingsState.customEngineRequestFormat.ifEmpty {
            """{ "customFields": {"model": "$modelName", "temperature": 0.0, "stream": true} }"""
        }
    override val responseFormat
        get() = autoDevSettingsState.customEngineResponseFormat.ifEmpty {
            "\$.choices[0].delta.content"
        }

    private var client = OkHttpClient()
    private val timeout = Duration.ofSeconds(defaultTimeout)
    private val messages: MutableList<Message> = mutableListOf()
    private val logger = logger<CustomLLMProvider>()

    override fun clearMessage() = messages.clear()

    override fun appendLocalMessage(msg: String, role: ChatRole) {
        if (msg.isEmpty()) return
        messages += Message(role.roleName(), msg)
    }

    override fun prompt(promptText: String): String = this.prompt(promptText, "")

    override fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
        if (!keepHistory || project.coderSetting.state.noChatHistory) {
            clearMessage()
        }

        if (systemPrompt.isNotEmpty()) {
            if (messages.isNotEmpty() && messages[0].role != "system") {
                messages.add(0, Message("system", systemPrompt))
            } else if (messages.isEmpty()) {
                messages.add(Message("system", systemPrompt))
            } else {
                messages[0] = Message("system", systemPrompt)
            }
        }

        messages += Message("user", promptText)

        val customRequest = CustomRequest(messages)
        val requestContent = customRequest.updateCustomFormat(requestFormat)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent)

        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
            builder.addHeader("Content-Type", "application/json")
        }
        builder.appendCustomHeaders(requestFormat)

        logger.info("Requesting form: $requestContent $body")

        client = client.newBuilder().readTimeout(timeout).build()
        val call = client.newCall(builder.url(url).post(body).build())

        if (!keepHistory || project.coderSetting.state.noChatHistory) {
            clearMessage()
        }

        return streamSSE(call, promptText, keepHistory, messages)
    }

    fun prompt(instruction: String, input: String): String {
        messages += Message("user", instruction)
        val customRequest = CustomRequest(messages)
        val requestContent = Json.encodeToString<CustomRequest>(customRequest)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent)

        logger.info("Requesting form: $requestContent $body")
        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
        }

        try {
            client = client.newBuilder().readTimeout(timeout).build()

            val request = builder.url(url).post(body).build()
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
