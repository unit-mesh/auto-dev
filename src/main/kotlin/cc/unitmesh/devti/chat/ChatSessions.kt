package cc.unitmesh.devti.chat

import cc.unitmesh.devti.gui.chat.ChatRole


class ChatMessage(
    val id: String,
    val message: String,
    val role: ChatRole,
    val displayText: String
) {
}

class ChatSessions(
    val id: String,
    val metadata: Any,
    val messages: List<ChatMessage>
) {
}