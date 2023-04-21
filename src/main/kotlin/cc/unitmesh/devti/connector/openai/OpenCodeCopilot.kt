package cc.unitmesh.devti.connector.openai

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.connector.CodeCopilot
import cc.unitmesh.devti.connector.DevtiFlowAction
import cc.unitmesh.devti.kanban.SimpleProjectInfo
import cc.unitmesh.devti.prompt.parseCodeFromString
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import kotlinx.coroutines.runBlocking
import java.time.Duration


class OpenCodeCopilot(val openAIKey: String, val version: String) : CodeCopilot, DevtiFlowAction {
    private val promptGenerator = PromptGenerator()
    private lateinit var service: OpenAiService

    init {
        service = OpenAiService(openAIKey, Duration.ofSeconds(600))
    }

    private fun prompt(instruction: String): String {
        val messages: MutableList<ChatMessage> = ArrayList()
        val systemMessage = ChatMessage(ChatMessageRole.USER.value(), instruction)
        messages.add(systemMessage)

        val completionRequest = ChatCompletionRequest.builder()
            .model(version)
            .temperature(0.0)
            .messages(messages)
            .build()

        val completion = service.createChatCompletion(completionRequest)
        val output = completion
            .choices[0].message.content

        logger.warn("output: $output")

        return output
    }

    override fun fillStoryDetail(project: SimpleProjectInfo, story: String): String {
        val promptText = promptGenerator.storyDetail(project, story)
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking prompt
        }
    }

    override fun analysisEndpoint(storyDetail: String, classes: List<DtClass>): String {
        val promptText = promptGenerator.createEndpoint(storyDetail, classes)
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking prompt
        }
    }

    override fun needUpdateMethodOfController(targetEndpoint: String, clazz: DtClass, storyDetail: String): String {
        val promptText = promptGenerator.updateControllerMethod(clazz, storyDetail)
        logger.warn("needUpdateMethodForController prompt text: $promptText")
        return runBlocking {
            return@runBlocking prompt(promptText)
        }
    }

    override fun codeCompleteFor(text: @NlsSafe String, className: @NlsSafe String): String {
        val promptText = promptGenerator.codeComplete(text, className)
        logger.warn("codeCompleteFor prompt text: $promptText")
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking parseCodeFromString(prompt)
        }
    }

    override fun autoComment(text: @NlsSafe String): String {
        val promptText = promptGenerator.autoComment(text)
        logger.warn("autoComponent prompt text: $promptText")
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking parseCodeFromString(prompt)
        }
    }

    companion object {
        private val logger: Logger = logger<OpenCodeCopilot>()
    }
}