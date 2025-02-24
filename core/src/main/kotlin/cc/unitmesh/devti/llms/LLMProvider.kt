package cc.unitmesh.devti.llms

import cc.unitmesh.devti.gui.chat.message.ChatRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    val defaultTimeout: Long get() = 600

    @OptIn(ExperimentalCoroutinesApi::class)
    fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean = true): Flow<String>

    /**
     * Clear all messages
     */
    fun clearMessage() {}

    fun appendLocalMessage(msg: String, role: ChatRole) {}
}
