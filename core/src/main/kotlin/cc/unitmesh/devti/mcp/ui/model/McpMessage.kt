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
    val content: String,
    val toolName: String? = null,
    val parameters: String? = null
) {
    companion object {
        fun parseContent(content: String): Pair<String?, String?> {
            val toolNamePrefix = "Tool: "
            val paramsPrefix = "Parameters: "
            
            val toolName = content.lineSequence()
                .find { it.startsWith(toolNamePrefix) }
                ?.substringAfter(toolNamePrefix)
                ?.trim()
            
            val params = content.lineSequence()
                .find { it.startsWith(paramsPrefix) }
                ?.substringAfter(paramsPrefix)
                ?.trim()
            
            return Pair(toolName, params)
        }
    }
}
