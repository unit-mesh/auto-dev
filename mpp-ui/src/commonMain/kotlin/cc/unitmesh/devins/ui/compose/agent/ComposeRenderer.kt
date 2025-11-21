package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.*
import cc.unitmesh.agent.render.BaseRenderer
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.llm.TimelineItemType
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer.TimelineItem.*
import cc.unitmesh.llm.compression.TokenInfo
import kotlinx.datetime.Clock

/**
 * Compose UI Renderer that extends BaseRenderer
 *
 * Implements CodingAgentRenderer interface from mpp-core.
 * Integrates the BaseRenderer architecture with Compose state management.
 *
 * This renderer maintains a unified timeline of all agent activities (messages, tool calls, results)
 * and exposes them as Compose state for reactive UI updates.
 *
 * @see cc.unitmesh.agent.render.CodingAgentRenderer - The core interface
 * @see cc.unitmesh.agent.render.BaseRenderer - Common functionality
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

    // Token tracking
    private var _totalTokenInfo by mutableStateOf(TokenInfo())
    val totalTokenInfo: TokenInfo get() = _totalTokenInfo

    private var _lastMessageTokenInfo by mutableStateOf<TokenInfo?>(null)

    // File viewer state
    private var _currentViewingFile by mutableStateOf<String?>(null)
    val currentViewingFile: String? get() = _currentViewingFile

    // Task tracking from task-boundary tool
    private val _tasks = mutableStateListOf<TaskInfo>()
    val tasks: List<TaskInfo> = _tasks

    // Timeline data structures for chronological rendering
    sealed class TimelineItem(val timestamp: Long = Clock.System.now().toEpochMilliseconds()) {
        data class MessageItem(
            val message: Message,
            val tokenInfo: TokenInfo? = null,
            val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
        ) : TimelineItem(itemTimestamp)

        /**
         * Combined tool call and result item - displays both in a single compact row
         * This replaces the separate ToolCallItem and ToolResultItem for better space efficiency
         */
        data class CombinedToolItem(
            val toolName: String,
            val description: String,
            val details: String? = null,
            val fullParams: String? = null, // 完整的原始参数，用于折叠展示
            val filePath: String? = null, // 文件路径，用于点击查看
            val toolType: ToolType? = null, // 工具类型，用于判断是否可点击
            // Result fields
            val success: Boolean? = null, // null means still executing
            val summary: String? = null,
            val output: String? = null, // 截断的输出用于直接展示
            val fullOutput: String? = null, // 完整的输出，用于折叠展示或错误诊断
            val executionTimeMs: Long? = null, // 执行时间
            val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
        ) : TimelineItem(itemTimestamp)
        @Deprecated("Use CombinedToolItem instead")
        data class ToolResultItem(
            val toolName: String,
            val success: Boolean,
            val summary: String,
            val output: String? = null, // 截断的输出用于直接展示
            val fullOutput: String? = null, // 完整的输出，用于折叠展示或错误诊断
            val itemTimestamp: Long = Clock.System.now().toEpochMilliseconds()
        ) : TimelineItem(itemTimestamp)

        data class ToolErrorItem(
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

        /**
         * Live terminal session - connected to a PTY process for real-time output
         * This is only used on platforms that support PTY (JVM with JediTerm)
         */
        data class LiveTerminalItem(
            val sessionId: String,
            val command: String,
            val workingDirectory: String?,
            val ptyHandle: Any?, // Platform-specific: on JVM this is a PtyProcess
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
                        ),
                    tokenInfo = _lastMessageTokenInfo
                )
            )
        }

        _currentStreamingOutput = ""
        _isProcessing = false
        _lastMessageTokenInfo = null // Reset after use
    }

    override fun renderToolCall(
        toolName: String,
        paramsStr: String
    ) {
        val toolInfo = formatToolCallDisplay(toolName, paramsStr)
        val params = parseParamsString(paramsStr)
        val toolType = toolName.toToolType()

        // Handle task-boundary tool - update task list
        if (toolName == "task-boundary") {
            updateTaskFromToolCall(params)
        }

        // Extract file path for read/write operations
        val filePath =
            when (toolType) {
                ToolType.ReadFile, ToolType.WriteFile -> params["path"]
                else -> null
            }

        // Create a combined tool item with only call information (result will be added later)
        _timeline.add(
            TimelineItem.CombinedToolItem(
                toolName = toolInfo.toolName,
                description = toolInfo.description,
                details = toolInfo.details,
                fullParams = paramsStr, // 保存完整的原始参数
                filePath = filePath, // 保存文件路径
                toolType = toolType, // 保存工具类型
                success = null, // null indicates still executing
                summary = null,
                output = null,
                fullOutput = null,
                executionTimeMs = null
            )
        )

        _currentToolCall =
            ToolCallInfo(
                toolName = toolInfo.toolName,
                description = toolInfo.description,
                details = toolInfo.details
            )
    }

    /**
     * Update task list from task-boundary tool call
     */
    private fun updateTaskFromToolCall(params: Map<String, String>) {
        val taskName = params["taskName"] ?: return
        val statusStr = params["status"] ?: "WORKING"
        val summary = params["summary"] ?: ""
        val status = TaskStatus.fromString(statusStr)

        // Find existing task or create new one
        val existingIndex = _tasks.indexOfFirst { it.taskName == taskName }

        if (existingIndex >= 0) {
            // Update existing task
            val existingTask = _tasks[existingIndex]
            _tasks[existingIndex] = existingTask.copy(
                status = status,
                summary = summary,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        } else {
            // Add new task
            _tasks.add(
                TaskInfo(
                    taskName = taskName,
                    status = status,
                    summary = summary
                )
            )
        }

        // Remove completed or cancelled tasks after a delay (keep them visible briefly)
        if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
            // Keep completed tasks visible for review
            // You could add auto-removal logic here if desired
        }
    }

    override fun renderToolResult(
        toolName: String,
        success: Boolean,
        output: String?,
        fullOutput: String?,
        metadata: Map<String, String>
    ) {
        val summary = formatToolResultSummary(toolName, success, output)

        // Check if this was a live terminal session
        val isLiveSession = metadata["isLiveSession"] == "true"
        val liveExitCode = metadata["live_exit_code"]?.toIntOrNull()

        // For shell commands, use special terminal output rendering
        val toolType = toolName.toToolType()
        if (toolType == ToolType.Shell && output != null) {
            // Try to extract shell result information
            val exitCode = liveExitCode ?: (if (success) 0 else 1)
            val executionTime = metadata["execution_time_ms"]?.toLongOrNull() ?: 0L

            // Extract command from the last tool call if available
            val command = _currentToolCall?.details?.removePrefix("Executing: ") ?: "unknown"

            // For Live sessions, we show both the terminal widget and the result summary
            // Don't remove anything, just add a result item after the live terminal
            if (isLiveSession) {
                // Add a summary result item after the live terminal
                _timeline.add(
                    TimelineItem.TerminalOutputItem(
                        command = command,
                        output = fullOutput ?: output,
                        exitCode = exitCode,
                        executionTimeMs = executionTime
                    )
                )
            } else {
                // For non-live sessions, replace the combined tool item with terminal output
                val lastItem = _timeline.lastOrNull()
                if (lastItem is TimelineItem.CombinedToolItem && lastItem.toolType == ToolType.Shell) {
                    _timeline.removeAt(_timeline.size - 1)
                }

                _timeline.add(
                    TimelineItem.TerminalOutputItem(
                        command = command,
                        output = fullOutput ?: output,
                        exitCode = exitCode,
                        executionTimeMs = executionTime
                    )
                )
            }
        } else {
            // Update the last CombinedToolItem with result information
            val lastItem = _timeline.lastOrNull()
            if (lastItem is TimelineItem.CombinedToolItem && lastItem.success == null) {
                // Remove the incomplete item
                _timeline.removeAt(_timeline.size - 1)

                // Add the complete item with result
                val executionTime = metadata["execution_time_ms"]?.toLongOrNull()

                _timeline.add(
                    lastItem.copy(
                        success = success,
                        summary = summary,
                        output = if (success && output != null) {
                            // For file search tools, keep full output; for others, limit to 2000 chars for direct display
                            when (toolName) {
                                "glob", "grep" -> output
                                else -> if (output.length <= 2000) output else "${output.take(2000)}...\n[Output truncated - click to view full]"
                            }
                        } else {
                            null
                        },
                        fullOutput = fullOutput,
                        executionTimeMs = executionTime
                    )
                )
            }
        }

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
        _timeline.add(TimelineItem.ToolErrorItem(error = message))
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

    override fun renderRecoveryAdvice(recoveryAdvice: String) {
        _timeline.add(
            TimelineItem.MessageItem(
                message =
                    Message(
                        role = MessageRole.ASSISTANT,
                        content = "🔧 ERROR RECOVERY ADVICE:\n$recoveryAdvice"
                    )
            )
        )
    }

    override fun renderUserConfirmationRequest(
        toolName: String,
        params: Map<String, Any>
    ) {
        // For now, just use error rendering since JS renderer doesn't have this method yet
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
        _totalTokenInfo = TokenInfo()
        _lastMessageTokenInfo = null
    }

    fun clearError() {
        _errorMessage = null
    }

    fun openFileViewer(filePath: String) {
        _currentViewingFile = filePath
    }

    fun closeFileViewer() {
        _currentViewingFile = null
    }

    /**
     * Adds a live terminal session to the timeline.
     * This is called when a Shell tool is executed with PTY support.
     *
     * Note: We keep the ToolCallItem so the user can see both the command call
     * and the live terminal output side by side.
     */
    override fun addLiveTerminal(
        sessionId: String,
        command: String,
        workingDirectory: String?,
        ptyHandle: Any?
    ) {
        // Add the live terminal item to the timeline
        // We no longer remove the ToolCallItem - both should be shown for complete visibility
        _timeline.add(
            TimelineItem.LiveTerminalItem(
                sessionId = sessionId,
                command = command,
                workingDirectory = workingDirectory,
                ptyHandle = ptyHandle
            )
        )
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

    /**
     * Update token information from LLM response
     * Called when StreamFrame.End is received with token metadata
     */
    override fun updateTokenInfo(tokenInfo: TokenInfo) {
        _lastMessageTokenInfo = tokenInfo
        // Accumulate total tokens
        _totalTokenInfo = TokenInfo(
            totalTokens = _totalTokenInfo.totalTokens + tokenInfo.totalTokens,
            inputTokens = _totalTokenInfo.inputTokens + tokenInfo.inputTokens,
            outputTokens = _totalTokenInfo.outputTokens + tokenInfo.outputTokens,
            timestamp = tokenInfo.timestamp
        )
    }

    /**
     * Convert a TimelineItem to MessageMetadata for persistence
     */
    private fun toMessageMetadata(item: TimelineItem): cc.unitmesh.devins.llm.MessageMetadata? {
        return when (item) {
            is TimelineItem.MessageItem -> {
                cc.unitmesh.devins.llm.MessageMetadata(
                    itemType = cc.unitmesh.devins.llm.TimelineItemType.MESSAGE,
                    tokenInfoTotal = item.tokenInfo?.totalTokens,
                    tokenInfoInput = item.tokenInfo?.inputTokens,
                    tokenInfoOutput = item.tokenInfo?.outputTokens
                )
            }

            is TimelineItem.CombinedToolItem -> {
                cc.unitmesh.devins.llm.MessageMetadata(
                    itemType = cc.unitmesh.devins.llm.TimelineItemType.COMBINED_TOOL,
                    toolName = item.toolName,
                    description = item.description,
                    details = item.details,
                    fullParams = item.fullParams,
                    filePath = item.filePath,
                    toolType = item.toolType?.name,
                    success = item.success,
                    summary = item.summary,
                    output = item.output,
                    fullOutput = item.fullOutput,
                    executionTimeMs = item.executionTimeMs
                )
            }
//            is TimelineItem.ToolCallItem -> {
//                cc.unitmesh.devins.llm.MessageMetadata(
//                    itemType = cc.unitmesh.devins.llm.TimelineItemType.TOOL_CALL,
//                    toolName = item.toolName,
//                    description = item.description,
//                    details = item.details,
//                    fullParams = item.fullParams,
//                    filePath = item.filePath,
//                    toolType = item.toolType?.name
//                )
//            }
            is TimelineItem.ToolResultItem -> {
                cc.unitmesh.devins.llm.MessageMetadata(
                    itemType = cc.unitmesh.devins.llm.TimelineItemType.TOOL_RESULT,
                    toolName = item.toolName,
                    success = item.success,
                    summary = item.summary,
                    output = item.output,
                    fullOutput = item.fullOutput
                )
            }

            is TimelineItem.ToolErrorItem -> {
                cc.unitmesh.devins.llm.MessageMetadata(
                    itemType = cc.unitmesh.devins.llm.TimelineItemType.TOOL_ERROR,
                    taskMessage = item.error
                )
            }

            is TimelineItem.TaskCompleteItem -> {
                cc.unitmesh.devins.llm.MessageMetadata(
                    itemType = cc.unitmesh.devins.llm.TimelineItemType.TASK_COMPLETE,
                    taskSuccess = item.success,
                    taskMessage = item.message
                )
            }

            is TimelineItem.TerminalOutputItem -> {
                cc.unitmesh.devins.llm.MessageMetadata(
                    itemType = cc.unitmesh.devins.llm.TimelineItemType.TERMINAL_OUTPUT,
                    command = item.command,
                    output = item.output,
                    exitCode = item.exitCode,
                    executionTimeMs = item.executionTimeMs
                )
            }

            is TimelineItem.LiveTerminalItem -> {
                // Live terminal items are not persisted (they're runtime-only)
                null
            }
        }
    }

    /**
     * Convert MessageMetadata back to a TimelineItem
     */
    private fun fromMessageMetadata(
        metadata: cc.unitmesh.devins.llm.MessageMetadata,
        message: cc.unitmesh.devins.llm.Message
    ): TimelineItem? {
        return when (metadata.itemType) {
            TimelineItemType.MESSAGE -> {
                val totalTokens = metadata.tokenInfoTotal
                val tokenInfo = if (totalTokens != null) {
                    TokenInfo(
                        totalTokens = totalTokens,
                        inputTokens = metadata.tokenInfoInput ?: 0,
                        outputTokens = metadata.tokenInfoOutput ?: 0
                    )
                } else null

                TimelineItem.MessageItem(
                    message = message,
                    tokenInfo = tokenInfo,
                    itemTimestamp = message.timestamp
                )
            }

            TimelineItemType.COMBINED_TOOL -> {
                TimelineItem.CombinedToolItem(
                    toolName = metadata.toolName ?: "",
                    description = metadata.description ?: "",
                    details = metadata.details,
                    fullParams = metadata.fullParams,
                    filePath = metadata.filePath,
                    toolType = metadata.toolType?.toToolType(),
                    success = metadata.success,
                    summary = metadata.summary,
                    output = metadata.output,
                    fullOutput = metadata.fullOutput,
                    executionTimeMs = metadata.executionTimeMs,
                    itemTimestamp = message.timestamp
                )
            }

            TimelineItemType.TOOL_RESULT -> {
                TimelineItem.ToolResultItem(
                    toolName = metadata.toolName ?: "",
                    success = metadata.success ?: false,
                    summary = metadata.summary ?: "",
                    output = metadata.output,
                    fullOutput = metadata.fullOutput,
                    itemTimestamp = message.timestamp
                )
            }

            TimelineItemType.TOOL_ERROR -> {
                TimelineItem.ToolErrorItem(
                    error = metadata.taskMessage ?: "Unknown error",
                    itemTimestamp = message.timestamp
                )
            }

            TimelineItemType.TASK_COMPLETE -> {
                TaskCompleteItem(
                    success = metadata.taskSuccess ?: false,
                    message = metadata.taskMessage ?: "",
                    itemTimestamp = message.timestamp
                )
            }

            TimelineItemType.TERMINAL_OUTPUT -> {
                TimelineItem.TerminalOutputItem(
                    command = metadata.command ?: "",
                    output = message.content,
                    exitCode = metadata.exitCode ?: 0,
                    executionTimeMs = metadata.executionTimeMs ?: 0,
                    itemTimestamp = message.timestamp
                )
            }
            else -> null
        }
    }

    /**
     * Load timeline from a list of messages
     * This is used when switching sessions or loading history
     */
    fun loadFromMessages(messages: List<cc.unitmesh.devins.llm.Message>) {
        _timeline.clear()

        messages.forEach { message ->
            val messageMetadata = message.metadata
            val timelineItem = if (messageMetadata != null) {
                // Try to reconstruct from metadata
                fromMessageMetadata(messageMetadata, message)
            } else {
                // Fallback: create a simple MessageItem for messages without metadata
                TimelineItem.MessageItem(
                    message = message,
                    tokenInfo = null,
                    itemTimestamp = message.timestamp
                )
            }

            timelineItem?.let { _timeline.add(it) }
        }
    }

    /**
     * Get current timeline as messages with metadata
     * This is used when saving conversation history
     */
    fun getTimelineSnapshot(): List<cc.unitmesh.devins.llm.Message> {
        return _timeline.mapNotNull { item ->
            when (item) {
                is TimelineItem.MessageItem -> {
                    // Return the original message with metadata
                    item.message.copy(
                        metadata = toMessageMetadata(item)
                    )
                }

                is TimelineItem.CombinedToolItem -> {
                    // Create a message representing the tool call and result
                    val content = buildString {
                        append("[${item.toolName}] ")
                        append(item.description)
                        if (item.summary != null) {
                            append(" → ${item.summary}")
                        }
                    }
                    cc.unitmesh.devins.llm.Message(
                        role = MessageRole.ASSISTANT,
                        content = content,
                        timestamp = item.itemTimestamp,
                        metadata = toMessageMetadata(item)
                    )
                }

                is TimelineItem.TerminalOutputItem -> {
                    cc.unitmesh.devins.llm.Message(
                        role = MessageRole.ASSISTANT,
                        content = item.output,
                        timestamp = item.itemTimestamp,
                        metadata = toMessageMetadata(item)
                    )
                }

                is TimelineItem.TaskCompleteItem -> {
                    cc.unitmesh.devins.llm.Message(
                        role = MessageRole.ASSISTANT,
                        content = item.message,
                        timestamp = item.itemTimestamp,
                        metadata = toMessageMetadata(item)
                    )
                }

                is TimelineItem.ToolErrorItem -> {
                    cc.unitmesh.devins.llm.Message(
                        role = MessageRole.ASSISTANT,
                        content = item.error,
                        timestamp = item.itemTimestamp,
                        metadata = toMessageMetadata(item)
                    )
                }
                // Skip deprecated items and live terminal items
//                is TimelineItem.ToolCallItem,
                is TimelineItem.ToolResultItem,
                is TimelineItem.LiveTerminalItem -> null
            }
        }
    }
}

