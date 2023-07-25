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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request


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

    private val messages: MutableList<ChatMessage> = ArrayList()
    private var historyMessageLength: Int = 0

    private val mapper = ObjectMapper().registerKotlinModule()

    fun prompt(instruction: String, input: String): String {
        val promptText = "$instruction\n$input"
        val systemMessage = ChatMessage(ChatMessageRole.USER.value(), promptText)

        if (historyMessageLength > 8192) {
            messages.clear()
        }

        messages.add(systemMessage)

        val builder = Request.Builder()
        val requestText = """{
            |"messages": ${mapper.writeValueAsString(messages)},
            |"temperature": 0.0
            }""".trimMargin()

        logger.warn("requestText: $requestText")
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

        val output = completion
            .choices[0].message.content

        logger.warn("output: $output")

        return output
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