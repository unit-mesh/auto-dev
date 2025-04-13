package cc.unitmesh.devti.mcp.ui.model

import java.time.LocalDateTime

enum class MessageType {
    REQUEST,
    RESPONSE
}

data class McpMessage(
    val type: MessageType,
    val method: String,
    val timestamp: LocalDateTime,
    val duration: Long? = null,
    val content: String
)
