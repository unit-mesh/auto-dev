package cc.unitmesh.devti.prompt.openai

import cc.unitmesh.devti.prompt.AiAction
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI

class OpenAIAction(val token: String, val version: String) : AiAction {
    private val openAI: OpenAI = OpenAI(token)

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
}