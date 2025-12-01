package cc.unitmesh.agent.render

import cc.unitmesh.agent.Platform

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

