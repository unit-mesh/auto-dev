package cc.unitmesh.devti.connector.azure

import cc.unitmesh.devti.connector.CodeCopilot
import cc.unitmesh.devti.connector.custom.PromptConfig
import cc.unitmesh.devti.connector.custom.PromptItem
import cc.unitmesh.devti.settings.DevtiSettingsState
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.theokanning.openai.completion.chat.ChatCompletionResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request


class AzureConnector : CodeCopilot {
    private val logger = Logger.getInstance(AzureConnector::class.java)

    private val devtiSettingsState = DevtiSettingsState.getInstance()
    private val url = devtiSettingsState?.customEngineServer ?: ""
    private var promptConfig: PromptConfig? = null
    private var client = OkHttpClient()

    init {
        val prompts = devtiSettingsState?.customEnginePrompts
        try {
            if (prompts != null) {
                promptConfig = Json.decodeFromString(prompts)
            }
        } catch (e: Exception) {
            println("Error parsing prompts: $e")
        }

        if (promptConfig == null) {
            promptConfig = PromptConfig(
                PromptItem("Auto complete", "{code}"),
                PromptItem("Auto comment", "{code}"),
                PromptItem("Code review", "{code}"),
                PromptItem("Find bug", "{code}")
            )
        }
    }

    override fun prompt(promptText: String): String {
        return this.prompt(promptText, "")
    }

    fun prompt(instruction: String, input: String): String {
        val body = okhttp3.RequestBody.create(
            okhttp3.MediaType.parse("application/json; charset=utf-8"),
            """
                {
                    "instruction": "$instruction",
                    "input": "$input",
                }
            """.trimIndent()
        )

        val builder = Request.Builder()

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
            objectMapper.readValue(response.body()?.string(), ChatCompletionResult::class.java)

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
        val bug = promptConfig!!.findBug
        return prompt(bug.instruction, bug.input.replace("{code}", text))
    }
}