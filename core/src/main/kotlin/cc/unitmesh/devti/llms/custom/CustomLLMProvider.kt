package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.prompting.optimizer.PromptOptimizer
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.time.Duration

/**
 * LLMProvider 不应该是单例 Service，它有多个并发场景的可能性
 */
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

    override fun stream(originPrompt: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
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

        val prompt = if (project.coderSetting.state.trimCodeBeforeSend) {
            PromptOptimizer.trimCodeSpace(originPrompt)
        } else {
            originPrompt
        }

        messages += Message("user", prompt)

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

        if (!keepHistory || project.coderSetting.state.noChatHistory) {
            clearMessage()
        }

        return streamSSE(call, prompt, keepHistory, messages)
    }
}
