package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.prompting.optimizer.PromptOptimizer
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
class CustomLLMProvider(val project: Project, var llmConfig: LlmConfig = LlmConfig.default()) : LLMProvider,
    CustomSSEProcessor(project) {
    private val url get() = llmConfig.url
    private val key get() = llmConfig.auth.token
    override val requestFormat: String get() = llmConfig.requestFormat
    override val responseFormat: String get() = llmConfig.responseFormat

    private var client = OkHttpClient()
    private val timeout = Duration.ofSeconds(defaultTimeout)
    private val messages: MutableList<Message> = mutableListOf()
    private val logger = logger<CustomLLMProvider>()

    override fun clearMessage() = messages.clear()

    override fun appendLocalMessage(msg: String, role: ChatRole) {
        if (msg.isEmpty()) return
        messages += Message(role.roleName(), msg)
    }

    var backupForReasonConfig: LlmConfig = llmConfig

    override fun stream(
        originPrompt: String,
        systemPrompt: String,
        keepHistory: Boolean,
        usePlanForSecondRound: Boolean
    ): Flow<String> {
        /// 如果第二轮使用 plan，且 plan 不为空，则使用 plan, 3 = System + User + Assistant
        if (usePlanForSecondRound && messages.size <= 3 && LlmConfig.load(ModelType.Plan).isNotEmpty()) {
            backupForReasonConfig = llmConfig
            llmConfig = LlmConfig.load(ModelType.Plan).first()
        } else {
            llmConfig = backupForReasonConfig
        }

        logger.info("Requesting to model: ${llmConfig.name}, $url")
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
