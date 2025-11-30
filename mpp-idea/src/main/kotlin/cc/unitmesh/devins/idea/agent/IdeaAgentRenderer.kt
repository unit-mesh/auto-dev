package cc.unitmesh.devins.idea.agent

import androidx.compose.runtime.*
import kotlinx.datetime.Clock

/**
 * Timeline item types for the agent chat interface.
 * Simplified version for IntelliJ IDEA integration.
 */
sealed class TimelineItem(val timestamp: Long = Clock.System.now().toEpochMilliseconds()) {
    data class MessageItem(
        val content: String,
        val isUser: Boolean,
        val tokenInfo: TokenInfo? = null,
        val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : TimelineItem(itemTimestamp)

    data class ToolCallItem(
        val toolName: String,
        val details: String?,
        val fullParams: String? = null,
        val success: Boolean? = null, // null means still executing
        val summary: String? = null,
        val output: String? = null,
        val fullOutput: String? = null,
        val executionTimeMs: Long? = null,
        val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : TimelineItem(itemTimestamp)

    data class ErrorItem(
        val error: String,
        val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : TimelineItem(itemTimestamp)

    data class TaskCompleteItem(
        val success: Boolean,
        val message: String,
        val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : TimelineItem(itemTimestamp)

    data class TerminalOutputItem(
        val command: String,
        val output: String,
        val exitCode: Int,
        val executionTimeMs: Long,
        val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : TimelineItem(itemTimestamp)
}

/**
 * Token usage information for LLM responses.
 */
data class TokenInfo(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = inputTokens + outputTokens
)

/**
 * Current tool call information for displaying in-progress operations.
 */
data class ToolCallInfo(
    val toolName: String,
    val description: String,
    val details: String? = null
)

/**
 * Renderer for the IDEA CodingAgent interface.
 * Manages timeline state and provides reactive updates for Compose UI.
 */
class IdeaAgentRenderer {
    private val _timeline = mutableStateListOf<TimelineItem>()
    val timeline: List<TimelineItem> = _timeline

    var currentStreamingOutput by mutableStateOf("")
        private set

    var currentToolCall by mutableStateOf<ToolCallInfo?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun addUserMessage(content: String) {
        _timeline.add(TimelineItem.MessageItem(content = content, isUser = true))
    }

    fun addAssistantMessage(content: String, tokenInfo: TokenInfo? = null) {
        _timeline.add(TimelineItem.MessageItem(content = content, isUser = false, tokenInfo = tokenInfo))
    }

    fun startStreaming() {
        isProcessing = true
        currentStreamingOutput = ""
    }

    fun appendStreamingContent(content: String) {
        currentStreamingOutput += content
    }

    fun endStreaming() {
        if (currentStreamingOutput.isNotBlank()) {
            _timeline.add(TimelineItem.MessageItem(content = currentStreamingOutput.trim(), isUser = false))
        }
        currentStreamingOutput = ""
        isProcessing = false
    }

    fun startToolCall(toolName: String, description: String, details: String? = null) {
        currentToolCall = ToolCallInfo(toolName, description, details)
        _timeline.add(
            TimelineItem.ToolCallItem(
                toolName = toolName,
                details = details,
                success = null // Indicates in-progress
            )
        )
    }

    fun completeToolCall(success: Boolean, summary: String?, output: String? = null, executionTimeMs: Long? = null) {
        currentToolCall = null
        // Update the last tool call item with results
        val lastIndex = _timeline.indexOfLast { it is TimelineItem.ToolCallItem && it.success == null }
        if (lastIndex >= 0) {
            val item = _timeline[lastIndex] as TimelineItem.ToolCallItem
            _timeline[lastIndex] = item.copy(
                success = success,
                summary = summary,
                output = output,
                executionTimeMs = executionTimeMs
            )
        }
    }

    fun addError(message: String) {
        errorMessage = message
        _timeline.add(TimelineItem.ErrorItem(error = message))
    }

    fun clearError() {
        errorMessage = null
    }

    fun addTaskComplete(success: Boolean, message: String) {
        _timeline.add(TimelineItem.TaskCompleteItem(success = success, message = message))
        isProcessing = false
    }

    fun addTerminalOutput(command: String, output: String, exitCode: Int, executionTimeMs: Long) {
        _timeline.add(TimelineItem.TerminalOutputItem(command, output, exitCode, executionTimeMs))
    }

    fun clear() {
        _timeline.clear()
        currentStreamingOutput = ""
        currentToolCall = null
        errorMessage = null
        isProcessing = false
    }

    fun forceStop() {
        currentStreamingOutput = ""
        currentToolCall = null
        isProcessing = false
    }
}

