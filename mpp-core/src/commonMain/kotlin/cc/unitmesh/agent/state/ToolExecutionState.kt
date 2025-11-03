package cc.unitmesh.agent.state

import cc.unitmesh.agent.tool.ToolResult
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

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

/**
 * Represents a tool call to be executed
 */
@Serializable
data class ToolCall(
    val id: String,
    val toolName: String,
    val params: Map<String, String>,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        private var counter = 0
        
        fun create(toolName: String, params: Map<String, Any>): ToolCall {
            return ToolCall(
                id = "call_${Clock.System.now().toEpochMilliseconds()}_${++counter}",
                toolName = toolName,
                params = params.mapValues { it.value.toString() }
            )
        }
    }
}

/**
 * State manager for tracking tool execution states
 */
class ToolStateManager {
    private val states = mutableMapOf<String, ToolExecutionState>()
    private val history = mutableListOf<ToolExecutionState>()
    
    /**
     * Update the state of a tool execution
     */
    fun updateState(state: ToolExecutionState) {
        states[state.callId] = state
        history.add(state)
    }
    
    /**
     * Get current state of a tool execution
     */
    fun getState(callId: String): ToolExecutionState? {
        return states[callId]
    }
    
    /**
     * Get all current states
     */
    fun getAllStates(): Map<String, ToolExecutionState> {
        return states.toMap()
    }
    
    /**
     * Get execution history
     */
    fun getHistory(): List<ToolExecutionState> {
        return history.toList()
    }
    
    /**
     * Clear all states and history
     */
    fun clear() {
        states.clear()
        history.clear()
    }
    
    /**
     * Get pending tool calls
     */
    fun getPendingCalls(): List<ToolCall> {
        return states.values
            .filterIsInstance<ToolExecutionState.Pending>()
            .map { it.toolCall }
    }
    
    /**
     * Get executing tool calls
     */
    fun getExecutingCalls(): List<String> {
        return states.values
            .filterIsInstance<ToolExecutionState.Executing>()
            .map { it.callId }
    }
}
