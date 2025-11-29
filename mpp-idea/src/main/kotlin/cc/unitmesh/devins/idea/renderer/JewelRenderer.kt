package cc.unitmesh.devins.idea.renderer

import cc.unitmesh.agent.render.BaseRenderer
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

    // Execution timing
    private var executionStartTime = 0L

    // Data classes for timeline items
    sealed class TimelineItem(val timestamp: Long = System.currentTimeMillis()) {
        data class MessageItem(
            val role: MessageRole,
            val content: String,
            val tokenInfo: TokenInfo? = null,
            val itemTimestamp: Long = System.currentTimeMillis()
        ) : TimelineItem(itemTimestamp)

        data class ToolCallItem(
            val toolName: String,
            val params: String,
            val success: Boolean? = null,
            val output: String? = null,
            val executionTimeMs: Long? = null,
            val itemTimestamp: Long = System.currentTimeMillis()
        ) : TimelineItem(itemTimestamp)

        data class ErrorItem(
            val message: String,
            val itemTimestamp: Long = System.currentTimeMillis()
        ) : TimelineItem(itemTimestamp)

        data class TaskCompleteItem(
            val success: Boolean,
            val message: String,
            val iterations: Int,
            val itemTimestamp: Long = System.currentTimeMillis()
        ) : TimelineItem(itemTimestamp)
    }

    data class ToolCallInfo(
        val toolName: String,
        val params: String
    )

    enum class MessageRole {
        USER, ASSISTANT, SYSTEM
    }

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
                    tokenInfo = _totalTokenInfo.value
                )
            )
        }

        _currentStreamingOutput.value = ""
        _isProcessing.value = false
    }

    override fun renderToolCall(toolName: String, paramsStr: String) {
        _currentToolCall.value = ToolCallInfo(toolName, paramsStr)

        addTimelineItem(
            TimelineItem.ToolCallItem(
                toolName = toolName,
                params = paramsStr
            )
        )
    }

    override fun renderToolResult(
        toolName: String,
        success: Boolean,
        output: String?,
        fullOutput: String?,
        metadata: Map<String, String>
    ) {
        _currentToolCall.value = null

        // Update the last tool call item with result
        _timeline.update { items ->
            items.mapIndexed { index, item ->
                if (index == items.lastIndex && item is TimelineItem.ToolCallItem && item.toolName == toolName) {
                    item.copy(
                        success = success,
                        output = output ?: fullOutput
                    )
                } else {
                    item
                }
            }
        }
    }

    override fun renderTaskComplete() {
        _taskCompleted.value = true
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
    }

    override fun renderRepeatWarning(toolName: String, count: Int) {
        val warning = "Tool '$toolName' called repeatedly ($count times)"
        addTimelineItem(TimelineItem.ErrorItem(warning))
    }

    override fun renderRecoveryAdvice(recoveryAdvice: String) {
        addTimelineItem(
            TimelineItem.MessageItem(
                role = MessageRole.ASSISTANT,
                content = "🔧 Recovery Advice:\n$recoveryAdvice"
            )
        )
    }

    override fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>) {
        // For now, just render as a message
        addTimelineItem(
            TimelineItem.MessageItem(
                role = MessageRole.SYSTEM,
                content = "⚠️ Confirmation required for tool: $toolName"
            )
        )
    }

    override fun updateTokenInfo(tokenInfo: TokenInfo) {
        _totalTokenInfo.value = tokenInfo
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
        executionStartTime = 0L
    }

    fun reset() {
        clearTimeline()
    }

    private fun addTimelineItem(item: TimelineItem) {
        _timeline.update { it + item }
    }
}

