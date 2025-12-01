package cc.unitmesh.devins.idea.renderer

import cc.unitmesh.agent.render.BaseRenderer
import cc.unitmesh.agent.render.MessageRole
import cc.unitmesh.agent.render.RendererUtils
import cc.unitmesh.agent.render.TaskInfo
import cc.unitmesh.agent.render.TaskStatus
import cc.unitmesh.agent.render.TimelineItem
import cc.unitmesh.agent.render.ToolCallInfo
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.llm.compression.TokenInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Jewel-compatible Renderer for IntelliJ IDEA plugin.
 *
 * Uses Kotlin StateFlow instead of Compose mutableStateOf to avoid
 * ClassLoader conflicts with IntelliJ's bundled Compose runtime.
 *
 * Implements CodingAgentRenderer interface from mpp-core.
 *
 * This renderer is designed for maximum reusability and can be used with
 * any Compose-based UI (Jewel, Material Design, etc.) as it only exposes
 * StateFlow-based state.
 */
class JewelRenderer : BaseRenderer() {

    // Timeline of all events (messages, tool calls, results)
    private val _timeline = MutableStateFlow<List<TimelineItem>>(emptyList())
    val timeline: StateFlow<List<TimelineItem>> = _timeline.asStateFlow()

    // Current streaming output from LLM
    private val _currentStreamingOutput = MutableStateFlow("")
    val currentStreamingOutput: StateFlow<String> = _currentStreamingOutput.asStateFlow()

    // Processing state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Iteration tracking
    private val _currentIteration = MutableStateFlow(0)
    val currentIteration: StateFlow<Int> = _currentIteration.asStateFlow()

    private val _maxIterations = MutableStateFlow(100)
    val maxIterations: StateFlow<Int> = _maxIterations.asStateFlow()

    // Current active tool call
    private val _currentToolCall = MutableStateFlow<ToolCallInfo?>(null)
    val currentToolCall: StateFlow<ToolCallInfo?> = _currentToolCall.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Task completion state
    private val _taskCompleted = MutableStateFlow(false)
    val taskCompleted: StateFlow<Boolean> = _taskCompleted.asStateFlow()

    // Token tracking
    private val _totalTokenInfo = MutableStateFlow(TokenInfo())
    val totalTokenInfo: StateFlow<TokenInfo> = _totalTokenInfo.asStateFlow()

    private var _lastMessageTokenInfo: TokenInfo? = null

    // Execution timing
    private var executionStartTime = 0L

    private val _currentExecutionTime = MutableStateFlow(0L)
    val currentExecutionTime: StateFlow<Long> = _currentExecutionTime.asStateFlow()

    // Task tracking (from task-boundary tool)
    private val _tasks = MutableStateFlow<List<TaskInfo>>(emptyList())
    val tasks: StateFlow<List<TaskInfo>> = _tasks.asStateFlow()

    // BaseRenderer implementation

    override fun renderIterationHeader(current: Int, max: Int) {
        _currentIteration.value = current
        _maxIterations.value = max
    }

    override fun renderLLMResponseStart() {
        super.renderLLMResponseStart()
        _currentStreamingOutput.value = ""
        _isProcessing.value = true

        if (executionStartTime == 0L) {
            executionStartTime = System.currentTimeMillis()
        }
        _currentExecutionTime.value = System.currentTimeMillis() - executionStartTime
    }

    override fun renderLLMResponseChunk(chunk: String) {
        reasoningBuffer.append(chunk)

        // Wait for more content if we detect an incomplete devin block
        if (hasIncompleteDevinBlock(reasoningBuffer.toString())) {
            return
        }

        // Filter devin blocks and output clean content
        val processedContent = filterDevinBlocks(reasoningBuffer.toString())
        val cleanContent = cleanNewlines(processedContent)

        _currentStreamingOutput.value = cleanContent
    }

    override fun renderLLMResponseEnd() {
        super.renderLLMResponseEnd()

        val content = _currentStreamingOutput.value.trim()
        if (content.isNotEmpty()) {
            addTimelineItem(
                TimelineItem.MessageItem(
                    role = MessageRole.ASSISTANT,
                    content = content,
                    tokenInfo = _lastMessageTokenInfo
                )
            )
        }

        _currentStreamingOutput.value = ""
        _isProcessing.value = false
        _lastMessageTokenInfo = null
    }

    override fun renderToolCall(toolName: String, paramsStr: String) {
        val toolInfo = formatToolCallDisplay(toolName, paramsStr)
        val params = parseParamsString(paramsStr)
        val toolType = toolName.toToolType()

        // Handle task-boundary tool - update task list
        if (toolName == "task-boundary") {
            updateTaskFromToolCall(params)
        }

        // Extract file path for read/write operations
        val filePath = when (toolType) {
            ToolType.ReadFile, ToolType.WriteFile -> params["path"]
            else -> null
        }

        _currentToolCall.value = ToolCallInfo(
            toolName = toolInfo.toolName,
            description = toolInfo.description,
            details = toolInfo.details
        )

        addTimelineItem(
            TimelineItem.ToolCallItem(
                toolName = toolInfo.toolName,
                description = toolInfo.description,
                params = toolInfo.details ?: paramsStr,
                fullParams = paramsStr,
                filePath = filePath,
                toolType = toolType,
                success = null,
                summary = null,
                output = null,
                fullOutput = null,
                executionTimeMs = null
            )
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

        _tasks.update { tasks ->
            val existingIndex = tasks.indexOfFirst { it.taskName == taskName }
            if (existingIndex >= 0) {
                tasks.toMutableList().apply {
                    this[existingIndex] = tasks[existingIndex].copy(
                        status = status,
                        summary = summary,
                        timestamp = System.currentTimeMillis()
                    )
                }
            } else {
                tasks + TaskInfo(
                    taskName = taskName,
                    status = status,
                    summary = summary
                )
            }
        }
    }

    override fun renderToolResult(
        toolName: String,
        success: Boolean,
        output: String?,
        fullOutput: String?,
        metadata: Map<String, String>
    ) {
        // Capture current tool call info before clearing
        val currentToolCallInfo = _currentToolCall.value
        _currentToolCall.value = null

        val summary = formatToolResultSummary(toolName, success, output)
        val toolType = toolName.toToolType()
        val executionTime = metadata["execution_time_ms"]?.toLongOrNull()

        // For shell commands, check if it's a live session
        val isLiveSession = metadata["isLiveSession"] == "true"
        val liveExitCode = metadata["live_exit_code"]?.toIntOrNull()

        if (toolType == ToolType.Shell && output != null) {
            val exitCode = liveExitCode ?: (if (success) 0 else 1)
            val executionTimeMs = executionTime ?: 0L
            val command = currentToolCallInfo?.details?.removePrefix("Executing: ") ?: "unknown"

            if (isLiveSession) {
                // Add terminal output after live terminal
                addTimelineItem(
                    TimelineItem.TerminalOutputItem(
                        command = command,
                        output = fullOutput ?: output,
                        exitCode = exitCode,
                        executionTimeMs = executionTimeMs
                    )
                )
            } else {
                // Replace the last tool call with terminal output
                _timeline.update { items ->
                    val lastItem = items.lastOrNull()
                    if (lastItem is TimelineItem.ToolCallItem && lastItem.toolType == ToolType.Shell) {
                        items.dropLast(1)
                    } else {
                        items
                    }
                }
                addTimelineItem(
                    TimelineItem.TerminalOutputItem(
                        command = command,
                        output = fullOutput ?: output,
                        exitCode = exitCode,
                        executionTimeMs = executionTimeMs
                    )
                )
            }
        } else {
            // Update the last tool call item with result
            _timeline.update { items ->
                items.mapIndexed { index, item ->
                    if (index == items.lastIndex && item is TimelineItem.ToolCallItem && item.success == null) {
                        item.copy(
                            success = success,
                            summary = summary,
                            output = if (success && output != null) {
                                when (toolName) {
                                    "glob", "grep" -> output
                                    "docql" -> output
                                    else -> if (output.length <= 2000) output else "${output.take(2000)}...\n[Output truncated]"
                                }
                            } else null,
                            fullOutput = fullOutput ?: output,
                            executionTimeMs = executionTime
                        )
                    } else {
                        item
                    }
                }
            }
        }
    }

    override fun renderTaskComplete() {
        _taskCompleted.value = true
        _isProcessing.value = false
    }

    override fun renderFinalResult(success: Boolean, message: String, iterations: Int) {
        addTimelineItem(
            TimelineItem.TaskCompleteItem(
                success = success,
                message = message,
                iterations = iterations
            )
        )
        _isProcessing.value = false
        executionStartTime = 0L
    }

    override fun renderError(message: String) {
        _errorMessage.value = message
        addTimelineItem(TimelineItem.ErrorItem(message))
        _isProcessing.value = false
    }

    override fun renderRepeatWarning(toolName: String, count: Int) {
        val warning = "Tool '$toolName' called repeatedly ($count times)"
        addTimelineItem(TimelineItem.ErrorItem(warning))
    }

    override fun renderRecoveryAdvice(recoveryAdvice: String) {
        addTimelineItem(
            TimelineItem.MessageItem(
                role = MessageRole.ASSISTANT,
                content = "Recovery Advice:\n$recoveryAdvice"
            )
        )
    }

    override fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>) {
        addTimelineItem(
            TimelineItem.MessageItem(
                role = MessageRole.SYSTEM,
                content = "Confirmation required for tool: $toolName"
            )
        )
    }

    override fun updateTokenInfo(tokenInfo: TokenInfo) {
        _lastMessageTokenInfo = tokenInfo
        _totalTokenInfo.update { current ->
            TokenInfo(
                totalTokens = current.totalTokens + tokenInfo.totalTokens,
                inputTokens = current.inputTokens + tokenInfo.inputTokens,
                outputTokens = current.outputTokens + tokenInfo.outputTokens,
                timestamp = tokenInfo.timestamp
            )
        }
    }

    // Public methods for UI interaction

    fun addUserMessage(content: String) {
        addTimelineItem(
            TimelineItem.MessageItem(
                role = MessageRole.USER,
                content = content
            )
        )
    }

    fun clearTimeline() {
        _timeline.value = emptyList()
        _currentStreamingOutput.value = ""
        _isProcessing.value = false
        _currentIteration.value = 0
        _errorMessage.value = null
        _taskCompleted.value = false
        _totalTokenInfo.value = TokenInfo()
        _lastMessageTokenInfo = null
        _tasks.value = emptyList()
        executionStartTime = 0L
        _currentExecutionTime.value = 0L
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun reset() {
        clearTimeline()
    }

    /**
     * Force stop all processing states.
     * Used when user cancels a task.
     */
    fun forceStop() {
        val currentOutput = _currentStreamingOutput.value.trim()
        if (currentOutput.isNotEmpty()) {
            addTimelineItem(
                TimelineItem.MessageItem(
                    role = MessageRole.ASSISTANT,
                    content = "$currentOutput\n\n[Interrupted]"
                )
            )
        }

        _isProcessing.value = false
        _currentStreamingOutput.value = ""
        _currentToolCall.value = null
    }

    private fun addTimelineItem(item: TimelineItem) {
        _timeline.update { it + item }
    }

    private fun formatToolCallDisplay(toolName: String, paramsStr: String) =
        RendererUtils.formatToolCallDisplay(toolName, paramsStr)

    private fun formatToolResultSummary(toolName: String, success: Boolean, output: String?) =
        RendererUtils.formatToolResultSummary(toolName, success, output)

    private fun parseParamsString(paramsStr: String) =
        RendererUtils.parseParamsString(paramsStr)
}

