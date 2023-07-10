package cc.unitmesh.devti.connector.azure

import cc.unitmesh.devti.connector.CodeCopilot
import cc.unitmesh.devti.connector.custom.PromptConfig
import cc.unitmesh.devti.connector.custom.PromptItem
import cc.unitmesh.devti.settings.DevtiSettingsState
import cc.unitmesh.devti.settings.OPENAI_MODEL
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.openapi.diagnostic.Logger
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request


class AzureConnector : CodeCopilot {
    private val logger = Logger.getInstance(AzureConnector::class.java)

    private val devtiSettingsState = DevtiSettingsState.getInstance()
    private val url = devtiSettingsState?.customEngineServer ?: ""
    private var promptConfig: PromptConfig? = null
    private var client = OkHttpClient()
    private val openAiVersion: String

    init {
        val prompts = devtiSettingsState?.customEnginePrompts
        openAiVersion = DevtiSettingsState.getInstance()?.openAiModel ?: OPENAI_MODEL[0]
        promptConfig = PromptConfig.tryParse(prompts)
    }

    override fun prompt(promptText: String): String {
        return this.prompt(promptText, "")
    }

    val messages: MutableList<ChatMessage> = ArrayList()
    var historyMessageLength: Int = 0

    fun prompt(instruction: String, input: String): String {
        val promptText = "$instruction\n$input"
        val systemMessage = ChatMessage(ChatMessageRole.USER.value(), promptText)

        if (historyMessageLength > 8192) {
            messages.clear()
        }

        messages.add(systemMessage)

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .model(openAiVersion)
            .temperature(0.0)
            .messages(messages)
            .build()

        val builder = Request.Builder()

        val mapper = ObjectMapper().registerKotlinModule()
        val requestText = mapper.writeValueAsString(chatCompletionRequest)
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

    override fun codeCompleteFor(text: String, className: String): String {
        val complete = promptConfig!!.autoComplete
        return prompt(complete.instruction, complete.input.replace("{code}", text))
    }

    override fun autoComment(text: String): String {
        val comment = promptConfig!!.autoComment
        return prompt(comment.instruction, comment.input.replace("{code}", text))
    }

    override fun codeReviewFor(text: String): String {
        val review = promptConfig!!.codeReview
        return prompt(review.instruction, review.input.replace("{code}", text))
    }

    override fun findBug(text: String): String {
        val bug = promptConfig!!.refactor
        return prompt(bug.instruction, bug.input.replace("{code}", text))
    }
}