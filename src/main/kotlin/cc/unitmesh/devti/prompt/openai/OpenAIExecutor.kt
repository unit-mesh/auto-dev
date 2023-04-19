package cc.unitmesh.devti.prompt.openai

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.kanban.SimpleProjectInfo
import cc.unitmesh.devti.prompt.AiExecutor
import cc.unitmesh.devti.prompt.DevtiFlowAction
import cc.unitmesh.devti.prompt.parseCodeFromString
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import kotlinx.coroutines.runBlocking

class OpenAIExecutor(val openAIKey: String, val version: String) : AiExecutor, DevtiFlowAction {
    private val openAI: OpenAI = OpenAI(openAIKey)
    private val gptPromptText = GptPromptText()

    @OptIn(BetaOpenAI::class)
    override suspend fun prompt(prompt: String): String {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(version),
            temperature = 0.0,
            messages = listOf(
                ChatMessage(
                    role = ChatRole.User,
                    content = prompt
                )
            )
        )

        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
        return completion.choices.first().message?.content ?: ""
    }

    override fun fillStoryDetail(project: SimpleProjectInfo, story: String): String {
        val promptText = gptPromptText.fillStoryDetail(project, story)
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking prompt
        }
    }

    override fun analysisEndpoint(storyDetail: String, classes: List<DtClass>): String {
        val promptText = gptPromptText.fillEndpoint(storyDetail, classes)
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking prompt
        }
    }

    override fun needUpdateMethodForController(targetEndpoint: String, clazz: DtClass, storyDetail: String): String {
        val promptText = gptPromptText.fillUpdateMethod(clazz, storyDetail)
        logger.warn("needUpdateMethodForController prompt text: $promptText")
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking prompt
        }
    }

    fun codeCompleteFor(text: @NlsSafe String, className: @NlsSafe String?): String {
        val promptText = gptPromptText.fillCodeComplete(text, className)
        logger.warn("codeCompleteFor prompt text: $promptText")
        return runBlocking {
            val prompt = prompt(promptText)
            val code = parseCodeFromString(prompt)
            return@runBlocking code
        }
    }

    fun autoComment(text: @NlsSafe String): String {
        val promptText = gptPromptText.autoComment(text)
        logger.warn("autoComponent prompt text: $promptText")
        return runBlocking {
            val prompt = prompt(promptText)
            val code = parseCodeFromString(prompt)
            return@runBlocking code
        }
    }

    companion object {
        private val logger: Logger = logger<OpenAIExecutor>()
    }
}