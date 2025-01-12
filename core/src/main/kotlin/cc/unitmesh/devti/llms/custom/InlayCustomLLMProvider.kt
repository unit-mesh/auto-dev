package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.settings.ResponseType
import cc.unitmesh.devti.settings.coder.AutoDevCoderSettingService
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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

//TODO: refactor, this provider copy from CustomLLMProvider
@Service(Service.Level.PROJECT)
class InlayCustomLLMProvider(val project: Project) : LLMProvider, CustomSSEProcessor(project) {
    private val autoDevSettingsState = project.service<AutoDevCoderSettingService>().state
    private val url get() = autoDevSettingsState.customEngineServerParam
    private val key get() = autoDevSettingsState.customEngineTokenParam

    override val requestFormat: String get() = autoDevSettingsState.customEngineRequestBodyFormatParam
    override val responseFormat get() = autoDevSettingsState.customEngineResponseFormatParam

    private var client = OkHttpClient()
    private val timeout = Duration.ofSeconds(defaultTimeout)
    private val messages: MutableList<Message> = mutableListOf()
    private val logger = logger<InlayCustomLLMProvider>()

    override fun clearMessage() = messages.clear()

    override fun appendLocalMessage(msg: String, role: ChatRole) {
        messages += Message(role.roleName(), msg)
    }

    override fun prompt(promptText: String): String = this.prompt(promptText, "")

    override fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
        if (!keepHistory || project.coderSetting.state.noChatHistory) {
            clearMessage()
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

        return if (autoDevSettingsState.customEngineResponseTypeParam == ResponseType.SSE.name) {
            streamSSE(call, promptText, messages = messages)
        } else {
            streamJson(call, promptText, messages)
        }
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
