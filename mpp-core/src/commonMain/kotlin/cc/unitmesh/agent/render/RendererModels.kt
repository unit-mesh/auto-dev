package cc.unitmesh.agent.render

import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.impl.docql.DocQLSearchStats
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
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
 * Base timeline item for chronological rendering.
 * This is the shared base class for timeline items in both ComposeRenderer and JewelRenderer.
 */
sealed class TimelineItem(
    open val timestamp: Long = Platform.getCurrentTimestamp(),
    open val id: String = generateId()
) {
    /**
     * Message item for user/assistant/system messages.
     * Supports both simple role+content and full Message object.
     */
    data class MessageItem(
        val message: Message? = null,
        val role: MessageRole = message?.role ?: MessageRole.USER,
        val content: String = message?.content ?: "",
        val tokenInfo: TokenInfo? = null,
        override val timestamp: Long = message?.timestamp ?: Platform.getCurrentTimestamp(),
        override val id: String = generateId()
    ) : TimelineItem(timestamp, id) {
        /**
         * Secondary constructor for simple role+content usage (JewelRenderer).
         */
        constructor(
            role: MessageRole,
            content: String,
            tokenInfo: TokenInfo? = null,
            timestamp: Long = Platform.getCurrentTimestamp(),
            id: String = generateId()
        ) : this(
            message = null,
            role = role,
            content = content,
            tokenInfo = tokenInfo,
            timestamp = timestamp,
            id = id
        )
    }

    /**
     * Combined tool call and result item - displays both in a single compact row.
     * This is the primary way to display tool executions.
     */
    data class ToolCallItem(
        val toolName: String,
        val description: String = "",
        val params: String = "",
        val fullParams: String? = null,
        val filePath: String? = null,
        val toolType: ToolType? = null,
        val success: Boolean? = null, // null means still executing
        val summary: String? = null,
        val output: String? = null,
        val fullOutput: String? = null,
        val executionTimeMs: Long? = null,
        // DocQL-specific search statistics
        val docqlStats: DocQLSearchStats? = null,
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

    /**
     * Live terminal session - connected to a PTY process for real-time output.
     * This is only used on platforms that support PTY (JVM with JediTerm).
     */
    data class LiveTerminalItem(
        val sessionId: String,
        val command: String,
        val workingDirectory: String?,
        val ptyHandle: Any?, // Platform-specific: on JVM this is a PtyProcess
        override val timestamp: Long = Platform.getCurrentTimestamp(),
        override val id: String = generateId()
    ) : TimelineItem(timestamp, id)

    companion object {
        private var idCounter = 0L
        fun generateId(): String = "${Platform.getCurrentTimestamp()}-${idCounter++}"
    }
}

