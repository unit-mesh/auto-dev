package cc.unitmesh.devins.idea.model

/**
 * Represents a chat message in the agent interface.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * The role of a message sender.
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

