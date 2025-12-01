package cc.unitmesh.agent.render

import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.llm.compression.TokenInfo

/**
 * Shared data models for Renderer implementations.
 * Used by both ComposeRenderer and JewelRenderer.
 */

/**
 * Information about a tool call for display purposes.
 */
data class ToolCallInfo(
    val toolName: String,
    val description: String,
    val details: String? = null
)

/**
 * Internal display info for formatting tool calls.
 */
data class ToolCallDisplayInfo(
    val toolName: String,
    val description: String,
    val details: String?
)

/**
 * Task information from task-boundary tool.
 */
data class TaskInfo(
    val taskName: String,
    val status: TaskStatus,
    val summary: String = "",
    val timestamp: Long = Platform.getCurrentTimestamp(),
    val startTime: Long = Platform.getCurrentTimestamp()
)

/**
 * Task status enum with display names.
 */
enum class TaskStatus(val displayName: String) {
    PLANNING("Planning"),
    WORKING("Working"),
    COMPLETED("Completed"),
    BLOCKED("Blocked"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(status: String): TaskStatus {
            return entries.find { it.name.equals(status, ignoreCase = true) } ?: WORKING
        }
    }
}

/**
 * Message role for timeline messages.
 */
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

/**
 * Base timeline item for chronological rendering.
 * This is the shared base class for timeline items in both ComposeRenderer and JewelRenderer.
 */
sealed class TimelineItem(
    open val timestamp: Long = Platform.getCurrentTimestamp(),
    open val id: String = generateId()
) {
    /**
     * Message item for user/assistant/system messages.
     */
    data class MessageItem(
        val role: MessageRole,
        val content: String,
        val tokenInfo: TokenInfo? = null,
        override val timestamp: Long = Platform.getCurrentTimestamp(),
        override val id: String = generateId()
    ) : TimelineItem(timestamp, id)

    /**
     * Combined tool call and result item - displays both in a single compact row.
     * This is the primary way to display tool executions.
     */
    data class ToolCallItem(
        val toolName: String,
        val description: String = "",
        val params: String,
        val fullParams: String? = null,
        val filePath: String? = null,
        val toolType: ToolType? = null,
        val success: Boolean? = null, // null means still executing
        val summary: String? = null,
        val output: String? = null,
        val fullOutput: String? = null,
        val executionTimeMs: Long? = null,
        override val timestamp: Long = Platform.getCurrentTimestamp(),
        override val id: String = generateId()
    ) : TimelineItem(timestamp, id)

    /**
     * Error item for displaying errors.
     */
    data class ErrorItem(
        val message: String,
        override val timestamp: Long = Platform.getCurrentTimestamp(),
        override val id: String = generateId()
    ) : TimelineItem(timestamp, id)

    /**
     * Task completion item.
     */
    data class TaskCompleteItem(
        val success: Boolean,
        val message: String,
        val iterations: Int = 0,
        override val timestamp: Long = Platform.getCurrentTimestamp(),
        override val id: String = generateId()
    ) : TimelineItem(timestamp, id)

    /**
     * Terminal output item for shell command results.
     */
    data class TerminalOutputItem(
        val command: String,
        val output: String,
        val exitCode: Int,
        val executionTimeMs: Long,
        override val timestamp: Long = Platform.getCurrentTimestamp(),
        override val id: String = generateId()
    ) : TimelineItem(timestamp, id)

    companion object {
        private var idCounter = 0L
        fun generateId(): String = "${Platform.getCurrentTimestamp()}-${idCounter++}"
    }
}

