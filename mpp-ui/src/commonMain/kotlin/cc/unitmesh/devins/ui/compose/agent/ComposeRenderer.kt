package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.*
import cc.unitmesh.agent.render.BaseRenderer
import cc.unitmesh.agent.render.RendererUtils
import cc.unitmesh.agent.render.TaskInfo
import cc.unitmesh.agent.render.TaskStatus
import cc.unitmesh.agent.render.TimelineItem
import cc.unitmesh.agent.render.TimelineItem.*
import cc.unitmesh.agent.render.ToolCallInfo
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.impl.docql.DocQLSearchStats
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.llm.TimelineItemType
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

        // Create a tool call item with only call information (result will be added later)
        _timeline.add(
            ToolCallItem(
                toolName = toolInfo.toolName,
                description = toolInfo.description,
                params = toolInfo.details ?: "",
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
                // For non-live sessions, replace the tool call item with terminal output
                val lastItem = _timeline.lastOrNull()
                if (lastItem is ToolCallItem && lastItem.toolType == ToolType.Shell) {
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
            // Update the last ToolCallItem with result information
            val lastItem = _timeline.lastOrNull()
            if (lastItem is ToolCallItem && lastItem.success == null) {
                // Remove the incomplete item
                _timeline.removeAt(_timeline.size - 1)

                // Add the complete item with result
                val executionTime = metadata["execution_time_ms"]?.toLongOrNull()

                // Extract DocQL search stats if available
                val docqlStats = DocQLSearchStats.fromMetadata(metadata)

                // For DocQL, use detailedResults from stats as fullOutput if available
                // output should be the compact summary, fullOutput should be the detailed results
                val finalFullOutput = when {
                    // If fullOutput is explicitly provided, use it
                    !fullOutput.isNullOrBlank() -> fullOutput
                    // For DocQL, use detailedResults from stats if available
                    toolName.lowercase() == "docql" && docqlStats?.detailedResults != null -> docqlStats.detailedResults
                    // Otherwise, use output as fallback
                    else -> output
                }

                _timeline.add(
                    lastItem.copy(
                        success = success,
                        summary = summary,
                        output = if (success && output != null) {
                            // For file search tools, keep full output; for others, limit to 2000 chars for direct display
                            when (toolName) {
                                "glob", "grep" -> output
                                // For DocQL, output is already the compact summary, so use it as-is
                                "docql" -> output
                                else -> if (output.length <= 2000) output else "${output.take(2000)}...\n[Output truncated - click to view full]"
                            }
                        } else {
                            null
                        },
                        fullOutput = finalFullOutput,
                        executionTimeMs = executionTime,
                        docqlStats = docqlStats
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
        _timeline.add(ErrorItem(message = message))
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

    /**
     * Update the status of a live terminal session when it completes.
     * This is called from the background monitoring coroutine in ToolOrchestrator.
     */
    override fun updateLiveTerminalStatus(
        sessionId: String,
        exitCode: Int,
        executionTimeMs: Long,
        output: String?,
        cancelledByUser: Boolean
    ) {
        // Find and update the LiveTerminalItem in the timeline
        val index = _timeline.indexOfFirst {
            it is TimelineItem.LiveTerminalItem && it.sessionId == sessionId
        }

        if (index >= 0) {
            val existingItem = _timeline[index] as TimelineItem.LiveTerminalItem
            // Replace with updated item containing exit code and execution time
            _timeline[index] = existingItem.copy(
                exitCode = exitCode,
                executionTimeMs = executionTimeMs
            )
        }

        // Also notify any waiting coroutines via the session result channel
        sessionResultChannels[sessionId]?.let { channel ->
            val result = if (exitCode == 0) {
                cc.unitmesh.agent.tool.ToolResult.Success(
                    content = output ?: "",
                    metadata = mapOf(
                        "exit_code" to exitCode.toString(),
                        "execution_time_ms" to executionTimeMs.toString()
                    )
                )
            } else {
                // Distinguish between user cancellation and other failures
                val errorMessage = if (cancelledByUser) {
                    "Command cancelled by user"
                } else {
                    "Command failed with exit code: $exitCode"
                }
                cc.unitmesh.agent.tool.ToolResult.Error(
                    message = errorMessage,
                    metadata = mapOf(
                        "exit_code" to exitCode.toString(),
                        "execution_time_ms" to executionTimeMs.toString(),
                        "output" to (output ?: ""),
                        "cancelled" to cancelledByUser.toString()
                    )
                )
            }
            channel.trySend(result)
            sessionResultChannels.remove(sessionId)
        }
    }

    // Channel map for awaiting session results
    private val sessionResultChannels = mutableMapOf<String, kotlinx.coroutines.channels.Channel<cc.unitmesh.agent.tool.ToolResult>>()

    /**
     * Await the result of an async shell session.
     * Used when the Agent needs to wait for a shell command to complete before proceeding.
     */
    override suspend fun awaitSessionResult(sessionId: String, timeoutMs: Long): cc.unitmesh.agent.tool.ToolResult {
        // Check if the session is already completed
        val existingItem = _timeline.find {
            it is TimelineItem.LiveTerminalItem && it.sessionId == sessionId
        } as? TimelineItem.LiveTerminalItem

        if (existingItem?.exitCode != null) {
            // Session already completed
            return if (existingItem.exitCode == 0) {
                cc.unitmesh.agent.tool.ToolResult.Success(
                    content = "",
                    metadata = mapOf(
                        "exit_code" to existingItem.exitCode.toString(),
                        "execution_time_ms" to (existingItem.executionTimeMs ?: 0L).toString()
                    )
                )
            } else {
                cc.unitmesh.agent.tool.ToolResult.Error(
                    message = "Command failed with exit code: ${existingItem.exitCode}",
                    metadata = mapOf(
                        "exit_code" to existingItem.exitCode.toString(),
                        "execution_time_ms" to (existingItem.executionTimeMs ?: 0L).toString()
                    )
                )
            }
        }

        // Create a channel to wait for the result
        val channel = kotlinx.coroutines.channels.Channel<cc.unitmesh.agent.tool.ToolResult>(1)
        sessionResultChannels[sessionId] = channel

        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                channel.receive()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            sessionResultChannels.remove(sessionId)
            cc.unitmesh.agent.tool.ToolResult.Error("Session timed out after ${timeoutMs}ms")
        }
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

    private fun formatToolCallDisplay(toolName: String, paramsStr: String): ToolCallInfo {
        return RendererUtils.toToolCallInfo(RendererUtils.formatToolCallDisplay(toolName, paramsStr))
    }

    private fun formatToolResultSummary(toolName: String, success: Boolean, output: String?): String {
        return RendererUtils.formatToolResultSummary(toolName, success, output)
    }

    private fun parseParamsString(paramsStr: String): Map<String, String> {
        return RendererUtils.parseParamsString(paramsStr)
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

            is ToolCallItem -> {
                val stats = item.docqlStats
                cc.unitmesh.devins.llm.MessageMetadata(
                    itemType = cc.unitmesh.devins.llm.TimelineItemType.COMBINED_TOOL,
                    toolName = item.toolName,
                    description = item.description,
                    details = item.params,
                    fullParams = item.fullParams,
                    filePath = item.filePath,
                    toolType = item.toolType?.name,
                    success = item.success,
                    summary = item.summary,
                    output = item.output,
                    fullOutput = item.fullOutput,
                    executionTimeMs = item.executionTimeMs,
                    // DocQL stats
                    docqlSearchType = stats?.searchType?.name,
                    docqlQuery = stats?.query,
                    docqlDocumentPath = stats?.documentPath,
                    docqlChannels = stats?.channels?.joinToString(","),
                    docqlDocsSearched = stats?.documentsSearched,
                    docqlRawResults = stats?.totalRawResults,
                    docqlRerankedResults = stats?.resultsAfterRerank,
                    docqlTruncated = stats?.truncated,
                    docqlUsedFallback = stats?.usedFallback,
                    docqlDetailedResults = stats?.detailedResults,
                    docqlSmartSummary = stats?.smartSummary
                )
            }

            is ErrorItem -> {
                cc.unitmesh.devins.llm.MessageMetadata(
                    itemType = cc.unitmesh.devins.llm.TimelineItemType.TOOL_ERROR,
                    taskMessage = item.message
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

                MessageItem(
                    message = message,
                    tokenInfo = tokenInfo,
                    timestamp = message.timestamp
                )
            }

            TimelineItemType.COMBINED_TOOL -> {
                // Restore DocQL stats if available
                val searchTypeStr = metadata.docqlSearchType
                val docqlStats = if (searchTypeStr != null) {
                    val searchType = try {
                        DocQLSearchStats.SearchType.valueOf(searchTypeStr)
                    } catch (_: IllegalArgumentException) {
                        null
                    }

                    searchType?.let {
                        DocQLSearchStats(
                            searchType = it,
                            query = metadata.docqlQuery ?: "",
                            documentPath = metadata.docqlDocumentPath,
                            channels = metadata.docqlChannels?.split(",")?.filter { ch -> ch.isNotBlank() } ?: emptyList(),
                            documentsSearched = metadata.docqlDocsSearched ?: 0,
                            totalRawResults = metadata.docqlRawResults ?: 0,
                            resultsAfterRerank = metadata.docqlRerankedResults ?: 0,
                            truncated = metadata.docqlTruncated ?: false,
                            usedFallback = metadata.docqlUsedFallback ?: false,
                            detailedResults = metadata.docqlDetailedResults ?: "",
                            smartSummary = metadata.docqlSmartSummary
                        )
                    }
                } else null

                ToolCallItem(
                    toolName = metadata.toolName ?: "",
                    description = metadata.description ?: "",
                    params = metadata.details ?: "",
                    fullParams = metadata.fullParams,
                    filePath = metadata.filePath,
                    toolType = metadata.toolType?.toToolType(),
                    success = metadata.success,
                    summary = metadata.summary,
                    output = metadata.output,
                    fullOutput = metadata.fullOutput,
                    executionTimeMs = metadata.executionTimeMs,
                    docqlStats = docqlStats,
                    timestamp = message.timestamp
                )
            }

            TimelineItemType.TOOL_RESULT -> {
                // Legacy support: convert old ToolResultItem to ToolCallItem
                ToolCallItem(
                    toolName = metadata.toolName ?: "",
                    description = "",
                    params = "",
                    success = metadata.success,
                    summary = metadata.summary,
                    output = metadata.output,
                    fullOutput = metadata.fullOutput,
                    timestamp = message.timestamp
                )
            }

            TimelineItemType.TOOL_ERROR -> {
                ErrorItem(
                    message = metadata.taskMessage ?: "Unknown error",
                    timestamp = message.timestamp
                )
            }

            TimelineItemType.TASK_COMPLETE -> {
                TaskCompleteItem(
                    success = metadata.taskSuccess ?: false,
                    message = metadata.taskMessage ?: "",
                    timestamp = message.timestamp
                )
            }

            TimelineItemType.TERMINAL_OUTPUT -> {
                TerminalOutputItem(
                    command = metadata.command ?: "",
                    output = message.content,
                    exitCode = metadata.exitCode ?: 0,
                    executionTimeMs = metadata.executionTimeMs ?: 0,
                    timestamp = message.timestamp
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
                MessageItem(
                    message = message,
                    tokenInfo = null,
                    timestamp = message.timestamp
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
                is MessageItem -> {
                    // Return the original message with metadata
                    item.message?.copy(
                        metadata = toMessageMetadata(item)
                    ) ?: cc.unitmesh.devins.llm.Message(
                        role = item.role,
                        content = item.content,
                        timestamp = item.timestamp,
                        metadata = toMessageMetadata(item)
                    )
                }

                is ToolCallItem -> {
                    // Create a message representing the tool call and result
                    val content = buildString {
                        append("[${item.toolName}] ")
                        append(item.description)
                        if (item.summary != null) {
                            append(" -> ${item.summary}")
                        }
                    }
                    cc.unitmesh.devins.llm.Message(
                        role = MessageRole.ASSISTANT,
                        content = content,
                        timestamp = item.timestamp,
                        metadata = toMessageMetadata(item)
                    )
                }

                is TerminalOutputItem -> {
                    cc.unitmesh.devins.llm.Message(
                        role = MessageRole.ASSISTANT,
                        content = item.output,
                        timestamp = item.timestamp,
                        metadata = toMessageMetadata(item)
                    )
                }

                is TaskCompleteItem -> {
                    cc.unitmesh.devins.llm.Message(
                        role = MessageRole.ASSISTANT,
                        content = item.message,
                        timestamp = item.timestamp,
                        metadata = toMessageMetadata(item)
                    )
                }

                is ErrorItem -> {
                    cc.unitmesh.devins.llm.Message(
                        role = MessageRole.ASSISTANT,
                        content = item.message,
                        timestamp = item.timestamp,
                        metadata = toMessageMetadata(item)
                    )
                }

                is LiveTerminalItem -> null
            }
        }
    }
}

