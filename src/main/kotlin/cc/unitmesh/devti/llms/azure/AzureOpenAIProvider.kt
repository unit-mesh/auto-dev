package cc.unitmesh.devti.llms.azure

import cc.unitmesh.devti.llms.CodeCopilotProvider
import cc.unitmesh.devti.prompting.model.CustomPromptConfig
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class SimpleOpenAIFormat(val role: String, val content: String) {
    companion object {
        fun fromChatMessage(message: ChatMessage): SimpleOpenAIFormat {
            return SimpleOpenAIFormat(message.role, message.content)
        }
    }
}

@Service(Service.Level.PROJECT)
class AzureOpenAIProvider(val project: Project) : CodeCopilotProvider {
    private val logger = logger<AzureOpenAIProvider>()

    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private val url = autoDevSettingsState.customEngineServer
    private var customPromptConfig: CustomPromptConfig? = null
    private var client = OkHttpClient()
    private val openAiVersion: String

    init {
        val prompts = autoDevSettingsState.customEnginePrompts
        openAiVersion = AutoDevSettingsState.getInstance().openAiModel
        customPromptConfig = CustomPromptConfig.tryParse(prompts)
    }

    override fun prompt(promptText: String): String {
        return this.prompt(promptText, "")
    }

    private val messages: MutableList<SimpleOpenAIFormat> = ArrayList()
    private var historyMessageLength: Int = 0

    private val mapper = ObjectMapper().registerKotlinModule()

    fun prompt(instruction: String, input: String): String {
        val promptText = "$instruction\n$input"
        val systemMessage = ChatMessage(ChatMessageRole.USER.value(), promptText)

        if (historyMessageLength > 8192) {
            messages.clear()
        }

        messages.add(SimpleOpenAIFormat.fromChatMessage(systemMessage))

        val builder = Request.Builder()
        val requestText = """{
            |"messages": ${Json.encodeToString(messages)},
            |"temperature": 0.0
            }""".trimMargin()

        val body = okhttp3.RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            requestText
        )

        val request = builder
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            logger.error("$response")
            return ""
        }

        val objectMapper = ObjectMapper()
        val completion: ChatCompletionResult =
            objectMapper.readValue(response.body?.string(), ChatCompletionResult::class.java)

        return completion.choices[0].message.content
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