package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.*
import cc.unitmesh.agent.render.BaseRenderer
import cc.unitmesh.agent.tool.ToolNames
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.datetime.Clock

/**
 * Compose UI Renderer that extends BaseRenderer
 * Integrates the BaseRenderer architecture with Compose state management
 */
class ComposeRenderer : BaseRenderer() {
    // Unified timeline for all events (messages, tool calls, results)
    private val _timeline = mutableStateListOf<TimelineItem>()
    val timeline: List<TimelineItem> = _timeline

    private var _currentStreamingOutput by mutableStateOf("")
    val currentStreamingOutput: String get() = _currentStreamingOutput

    private var _isProcessing by mutableStateOf(false)
    val isProcessing: Boolean get() = _isProcessing

    private var _currentIteration by mutableStateOf(0)
    val currentIteration: Int get() = _currentIteration

    private var _maxIterations by mutableStateOf(100)
    val maxIterations: Int get() = _maxIterations

    private var _currentToolCall by mutableStateOf<ToolCallInfo?>(null)
    val currentToolCall: ToolCallInfo? get() = _currentToolCall

    private var _errorMessage by mutableStateOf<String?>(null)
    val errorMessage: String? get() = _errorMessage

    private var _taskCompleted by mutableStateOf(false)
    val taskCompleted: Boolean get() = _taskCompleted

    private var _executionStartTime by mutableStateOf(0L)
    val executionStartTime: Long get() = _executionStartTime

    private var _currentExecutionTime by mutableStateOf(0L)
    val currentExecutionTime: Long get() = _currentExecutionTime

    // Timeline data structures for chronological rendering
    sealed class TimelineItem(val timestamp: Long = Clock.System.now().toEpochMilliseconds()) {
        data class MessageItem(
            val message: Message,
            val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
        ) : TimelineItem(itemTimestamp)

        data class ToolCallItem(
            val toolName: String,
            val description: String,
            val details: String? = null,
            val fullParams: String? = null, // 完整的原始参数，用于折叠展示
            val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
        ) : TimelineItem(itemTimestamp)

        data class ToolResultItem(
            val toolName: String,
            val success: Boolean,
            val summary: String,
            val output: String? = null, // 截断的输出用于直接展示
            val fullOutput: String? = null, // 完整的输出，用于折叠展示或错误诊断
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
    }

    // Legacy data classes for compatibility
    data class ToolCallInfo(
        val toolName: String,
        val description: String,
        val details: String? = null
    )

    // BaseRenderer implementation

    override fun renderIterationHeader(
        current: Int,
        max: Int
    ) {
        _currentIteration = current
        _maxIterations = max
        // Don't show iteration headers in Compose UI - they're handled by the UI components
    }

    override fun renderLLMResponseStart() {
        super.renderLLMResponseStart()
        _currentStreamingOutput = ""
        _isProcessing = true

        // Start timing if this is the first iteration
        if (_executionStartTime == 0L) {
            _executionStartTime = Clock.System.now().toEpochMilliseconds()
        }
        _currentExecutionTime = Clock.System.now().toEpochMilliseconds() - _executionStartTime
    }

    override fun renderLLMResponseChunk(chunk: String) {
        reasoningBuffer.append(chunk)

        // Wait for more content if we detect an incomplete devin block
        if (hasIncompleteDevinBlock(reasoningBuffer.toString())) {
            return
        }

        // Process the buffer to filter out devin blocks
        val processedContent = filterDevinBlocks(reasoningBuffer.toString())
        val cleanContent = cleanNewlines(processedContent)

        // Update streaming output for Compose UI
        _currentStreamingOutput = cleanContent
    }

    override fun renderLLMResponseEnd() {
        super.renderLLMResponseEnd()

        // Add the completed reasoning as a message to timeline
        val finalContent = _currentStreamingOutput.trim()
        if (finalContent.isNotEmpty()) {
            _timeline.add(
                TimelineItem.MessageItem(
                    message =
                        Message(
                            role = MessageRole.ASSISTANT,
                            content = finalContent
                        )
                )
            )
        }

        _currentStreamingOutput = ""
        _isProcessing = false
    }

    override fun renderToolCall(
        toolName: String,
        paramsStr: String
    ) {
        val toolInfo = formatToolCallDisplay(toolName, paramsStr)

        // Add tool call to timeline with full params for detailed inspection
        _timeline.add(
            TimelineItem.ToolCallItem(
                toolName = toolInfo.toolName,
                description = toolInfo.description,
                details = toolInfo.details,
                fullParams = paramsStr // 保存完整的原始参数
            )
        )

        // Keep current tool call for status display
        _currentToolCall =
            ToolCallInfo(
                toolName = toolInfo.toolName,
                description = toolInfo.description,
                details = toolInfo.details
            )
    }

    override fun renderToolResult(
        toolName: String,
        success: Boolean,
        output: String?,
        fullOutput: String?
    ) {
        val summary = formatToolResultSummary(toolName, success, output)
        _timeline.add(
            TimelineItem.ToolResultItem(
                toolName = toolName,
                success = success,
                summary = summary,
                output =
                    if (success && output != null) {
                        // For file search tools, keep full output; for others, limit to 2000 chars for direct display
                        when (toolName) {
                            "glob", "grep" -> output
                            else -> if (output.length <= 2000) output else "${output.take(2000)}...\n[Output truncated - click to view full]"
                        }
                    } else {
                        null
                    },
                fullOutput = fullOutput // 保存完整的输出，用于查看完整日志和错误诊断
            )
        )

        _currentToolCall = null
    }

    override fun renderTaskComplete() {
        _taskCompleted = true
        _isProcessing = false
    }

    override fun renderFinalResult(
        success: Boolean,
        message: String,
        iterations: Int
    ) {
        _timeline.add(
            TimelineItem.TaskCompleteItem(
                success = success,
                message = message
            )
        )
        _isProcessing = false
        _taskCompleted = true
    }

    override fun renderError(message: String) {
        _timeline.add(TimelineItem.ErrorItem(error = message))
        _errorMessage = message
        _isProcessing = false
    }

    override fun renderRepeatWarning(
        toolName: String,
        count: Int
    ) {
        _timeline.add(
            TimelineItem.MessageItem(
                message =
                    Message(
                        role = MessageRole.ASSISTANT,
                        content = "⚠️ Warning: Tool '$toolName' has been called $count times in a row"
                    )
            )
        )
    }

    override fun renderUserConfirmationRequest(
        toolName: String,
        params: Map<String, Any>
    ) {
        TODO("Not yet implemented")
    }

    // Public methods for UI interaction
    fun addUserMessage(content: String) {
        _timeline.add(
            TimelineItem.MessageItem(
                message =
                    Message(
                        role = MessageRole.USER,
                        content = content
                    )
            )
        )
    }

    fun clearMessages() {
        _timeline.clear()
        _currentStreamingOutput = ""
        _errorMessage = null
        _taskCompleted = false
        _isProcessing = false
        _executionStartTime = 0L
        _currentExecutionTime = 0L
    }

    fun clearError() {
        _errorMessage = null
    }

    fun forceStop() {
        // If there's streaming output, save it as a message first
        val currentOutput = _currentStreamingOutput.trim()
        if (currentOutput.isNotEmpty()) {
            _timeline.add(
                TimelineItem.MessageItem(
                    message =
                        Message(
                            role = MessageRole.ASSISTANT,
                            content = "$currentOutput\n\n[Interrupted]"
                        )
                )
            )
        }

        _isProcessing = false
        _currentStreamingOutput = ""
        _currentToolCall = null
    }

    private fun formatToolCallDisplay(
        toolName: String,
        paramsStr: String
    ): ToolCallInfo {
        val params = parseParamsString(paramsStr)
        val toolType = toolName.toToolType()

        return when (toolType) {
            ToolType.ReadFile ->
                ToolCallInfo(
                    toolName = "${params["path"] ?: "unknown"} - ${toolType.displayName}",
                    description = "file reader",
                    details = "Reading file: ${params["path"] ?: "unknown"}"
                )

            ToolType.WriteFile ->
                ToolCallInfo(
                    toolName = "${params["path"] ?: "unknown"} - ${toolType.displayName}",
                    description = "file writer",
                    details = "Writing to file: ${params["path"] ?: "unknown"}"
                )

            ToolType.Glob ->
                ToolCallInfo(
                    toolName = toolType.displayName,
                    description = "pattern matcher",
                    details = "Searching for files matching pattern: ${params["pattern"] ?: "*"}"
                )

            ToolType.Shell ->
                ToolCallInfo(
                    toolName = toolType.displayName,
                    description = "command executor",
                    details = "Executing: ${params["command"] ?: params["cmd"] ?: "unknown command"}"
                )

            else ->
                ToolCallInfo(
                    toolName = toolName,
                    description = "tool execution",
                    details = paramsStr
                )
        }
    }

    private fun formatToolResultSummary(
        toolName: String,
        success: Boolean,
        output: String?
    ): String {
        if (!success) return "Failed"

        val toolType = toolName.toToolType()
        return when (toolType) {
            ToolType.ReadFile -> {
                val lines = output?.lines()?.size ?: 0
                "Read $lines lines"
            }

            ToolType.WriteFile -> "File written successfully"
            ToolType.Glob -> {
                val firstLine = output?.lines()?.firstOrNull() ?: ""
                if (firstLine.contains("Found ") && firstLine.contains(" files matching")) {
                    val count = firstLine.substringAfter("Found ").substringBefore(" files").toIntOrNull() ?: 0
                    "Found $count files"
                } else if (output?.contains("No files found") == true) {
                    "No files found"
                } else {
                    "Search completed"
                }
            }

            ToolType.Shell -> {
                val lines = output?.lines()?.size ?: 0
                if (lines > 0) "Executed ($lines lines output)" else "Executed successfully"
            }

            else -> "Success"
        }
    }

    private fun parseParamsString(paramsStr: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        val regex = Regex("""(\w+)="([^"]*)"|\s*(\w+)=([^\s]+)""")
        regex.findAll(paramsStr).forEach { match ->
            val key = match.groups[1]?.value ?: match.groups[3]?.value
            val value = match.groups[2]?.value ?: match.groups[4]?.value
            if (key != null && value != null) {
                params[key] = value
            }
        }

        return params
    }
}
