package cc.unitmesh.devti.llms

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llms.custom.Message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    val defaultTimeout: Long get() = 600

    @OptIn(ExperimentalCoroutinesApi::class)
    fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean = true, usePlanForFirst: Boolean = false): Flow<String>

    /**
     * Clear all messages
     */
    fun clearMessage() {}

    fun getAllMessages() = emptyList<Message>()

    fun appendLocalMessage(msg: String, role: ChatRole) {}
}
