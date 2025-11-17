package cc.unitmesh.agent.state

import cc.unitmesh.agent.tool.ToolResult
import kotlinx.serialization.Serializable

/**
 * Represents the execution state of a tool call
 * Used for tracking and debugging tool execution
 */
@Serializable
sealed class ToolExecutionState {
    abstract val callId: String
    
    @Serializable
    data class Pending(
        override val callId: String, 
        val toolCall: ToolCall
    ) : ToolExecutionState()
    
    @Serializable
    data class Executing(
        override val callId: String, 
        val startTime: Long
    ) : ToolExecutionState()
    
    @Serializable
    data class Success(
        override val callId: String, 
        val result: ToolResult, 
        val duration: Long
    ) : ToolExecutionState()
    
    @Serializable
    data class Failed(
        override val callId: String, 
        val error: String, 
        val duration: Long
    ) : ToolExecutionState()
    
    @Serializable
    data class Cancelled(
        override val callId: String, 
        val reason: String, 
        val duration: Long
    ) : ToolExecutionState()
    
    @Serializable
    data class Retrying(
        override val callId: String, 
        val attempt: Int, 
        val maxAttempts: Int, 
        val lastError: String
    ) : ToolExecutionState()
}

