package cc.unitmesh.agent.state

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Represents a tool call to be executed
 */
@Serializable
data class ToolCall(
    val id: String,
    val toolName: String,
    val params: Map<String, String>,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        private var counter = 0

        fun create(toolName: String, params: Map<String, Any>): ToolCall {
            return ToolCall(
                id = "call_${Clock.System.now().toEpochMilliseconds()}_${++counter}",
                toolName = toolName,
                params = params.mapValues { it.value.toString() }
            )
        }
    }
}