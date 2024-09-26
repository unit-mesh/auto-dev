package cc.unitmesh.devti.llms.azure

import com.theokanning.openai.completion.chat.ChatMessage
import kotlinx.serialization.Serializable

@Serializable
data class SimpleOpenAIFormat(val role: String, val content: String) {
    companion object {
        fun fromChatMessage(message: ChatMessage): SimpleOpenAIFormat {
            return SimpleOpenAIFormat(message.role, message.content)
        }
    }
}