package cc.unitmesh.devti.prompt.openai

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.kanban.SimpleProjectInfo
import cc.unitmesh.devti.prompt.AiAction
import cc.unitmesh.devti.prompt.DevtiFlowAction
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.runBlocking

class OpenAIAction(val openAIKey: String, val version: String) : AiAction, DevtiFlowAction {
    private val openAI: OpenAI = OpenAI(openAIKey)
    private val gptPromptText = GptPromptText()

    @OptIn(BetaOpenAI::class)
    override suspend fun prompt(prompt: String): String {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(version),
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

    override fun needUpdateMethodForController(targetEndpoint: String, clazz: DtClass): String {
        val promptText = gptPromptText.fillUpdateMethod(targetEndpoint, clazz)
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking prompt
        }
    }
}