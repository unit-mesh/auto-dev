package cc.unitmesh.agent.orchestrator

import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.state.ToolExecutionState
import kotlinx.serialization.Serializable

/**
 * Enhanced result for tool orchestration
 * Contains execution metadata and timing information
 */
@Serializable
data class ToolExecutionResult(
    val executionId: String,
    val toolName: String,
    val result: ToolResult,
    val startTime: Long,
    val endTime: Long,
    val duration: Long = endTime - startTime,
    val retryCount: Int = 0,
    val state: ToolExecutionState,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if the execution was successful
     */
    val isSuccess: Boolean
        get() = when (result) {
            is ToolResult.Success -> true
            is ToolResult.AgentResult -> result.success
            is ToolResult.Error -> false
            is ToolResult.Pending -> false // Pending is not yet successful
        }

    /**
     * Check if the execution is pending (async)
     */
    val isPending: Boolean
        get() = result is ToolResult.Pending

    /**
     * Get the result content
     */
    val content: String
        get() = when (result) {
            is ToolResult.Success -> result.content
            is ToolResult.AgentResult -> result.content
            is ToolResult.Error -> result.message
            is ToolResult.Pending -> result.message
        }
    
    /**
     * Get error message if execution failed
     */
    val errorMessage: String?
        get() = when (result) {
            is ToolResult.Error -> result.message
            is ToolResult.AgentResult -> if (!result.success) result.content else null
            else -> null
        }
    
    companion object {
        /**
         * Create a successful result
         */
        fun success(
            executionId: String,
            toolName: String,
            content: String,
            startTime: Long,
            endTime: Long,
            retryCount: Int = 0,
            metadata: Map<String, String> = emptyMap()
        ): ToolExecutionResult {
            return ToolExecutionResult(
                executionId = executionId,
                toolName = toolName,
                result = ToolResult.Success(content, metadata),
                startTime = startTime,
                endTime = endTime,
                retryCount = retryCount,
                state = ToolExecutionState.Success(executionId, ToolResult.Success(content, metadata), endTime - startTime),
                metadata = metadata
            )
        }
        
        /**
         * Create a failed result
         */
        fun failure(
            executionId: String,
            toolName: String,
            error: String,
            startTime: Long,
            endTime: Long,
            retryCount: Int = 0,
            metadata: Map<String, String> = emptyMap()
        ): ToolExecutionResult {
            // Preserve metadata in ToolResult.Error to enable downstream checks (e.g., cancelled flag)
            return ToolExecutionResult(
                executionId = executionId,
                toolName = toolName,
                result = ToolResult.Error(error, metadata = metadata),
                startTime = startTime,
                endTime = endTime,
                retryCount = retryCount,
                state = ToolExecutionState.Failed(executionId, error, endTime - startTime),
                metadata = metadata
            )
        }

        /**
         * Create a pending result for async tool execution (e.g., Shell with PTY)
         */
        fun pending(
            executionId: String,
            toolName: String,
            sessionId: String,
            command: String,
            startTime: Long,
            metadata: Map<String, String> = emptyMap()
        ): ToolExecutionResult {
            return ToolExecutionResult(
                executionId = executionId,
                toolName = toolName,
                result = ToolResult.Pending(
                    sessionId = sessionId,
                    toolName = toolName,
                    command = command,
                    message = "Executing: $command"
                ),
                startTime = startTime,
                endTime = startTime, // Not yet completed
                retryCount = 0,
                state = ToolExecutionState.Executing(executionId, startTime),
                metadata = metadata + mapOf("sessionId" to sessionId, "isAsync" to "true")
            )
        }
    }
}
