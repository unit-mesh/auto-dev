package cc.unitmesh.server.render

import cc.unitmesh.agent.AgentEditInfo
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.AgentEvent
import cc.unitmesh.agent.AgentStepInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class ServerSideRenderer : CodingAgentRenderer {
    private val eventChannel = Channel<AgentEvent>(Channel.UNLIMITED)
    
    val events: Flow<AgentEvent> = eventChannel.receiveAsFlow()
    
    override fun renderIterationHeader(current: Int, max: Int) {
        eventChannel.trySend(AgentEvent.IterationStart(current, max))
    }
    
    override fun renderLLMResponseStart() {
        // No-op for server-side
    }
    
    override fun renderLLMResponseChunk(chunk: String) {
        eventChannel.trySend(AgentEvent.LLMResponseChunk(chunk))
    }
    
    override fun renderLLMResponseEnd() {
        // No-op for server-side
    }
    
    override fun renderToolCall(toolName: String, paramsStr: String) {
        eventChannel.trySend(AgentEvent.ToolCall(toolName, paramsStr))
    }
    
    override fun renderToolResult(
        toolName: String,
        success: Boolean,
        output: String?,
        fullOutput: String?,
        metadata: Map<String, String>
    ) {
        eventChannel.trySend(AgentEvent.ToolResult(toolName, success, output))
    }
    
    override fun renderTaskComplete(executionTimeMs: Long) {
        // Will be handled by renderFinalResult
    }
    
    override fun renderFinalResult(success: Boolean, message: String, iterations: Int) {
        // Will be sent by AgentService after collecting steps and edits
    }
    
    override fun renderError(message: String) {
        eventChannel.trySend(AgentEvent.Error(message))
    }
    
    override fun renderRepeatWarning(toolName: String, count: Int) {
        eventChannel.trySend(AgentEvent.Error("Warning: Tool '$toolName' called $count times"))
    }
    
    override fun renderRecoveryAdvice(recoveryAdvice: String) {
        eventChannel.trySend(AgentEvent.LLMResponseChunk("\n[Recovery Advice] $recoveryAdvice\n"))
    }
    
    override fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>) {
        eventChannel.trySend(AgentEvent.Error("User confirmation required for tool: $toolName"))
    }
    
    fun sendComplete(
        success: Boolean,
        message: String,
        iterations: Int,
        steps: List<AgentStepInfo>,
        edits: List<AgentEditInfo>
    ) {
        eventChannel.trySend(AgentEvent.Complete(success, message, iterations, steps, edits))
        eventChannel.close()
    }
    
    fun sendError(message: String) {
        eventChannel.trySend(AgentEvent.Error(message))
        eventChannel.close()
    }
    
    fun close() {
        eventChannel.close()
    }
}

