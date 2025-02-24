package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.settings.miscs.ResponseType
import cc.unitmesh.devti.settings.coder.AutoDevCoderSettingService
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.time.Duration

//TODO: refactor, this provider copy from CustomLLMProvider
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

    override fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
        if (!keepHistory || project.coderSetting.state.noChatHistory) {
            clearMessage()
        }

        messages += Message("user", promptText)

        val customRequest = CustomRequest(messages)
        val requestContent = customRequest.updateCustomFormat(requestFormat)

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), requestContent.toByteArray())

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
}
