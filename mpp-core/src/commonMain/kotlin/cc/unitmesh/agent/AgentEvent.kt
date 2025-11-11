package cc.unitmesh.agent

import kotlinx.serialization.Serializable


@Serializable
data class AgentStepInfo(
    val step: Int,
    val action: String,
    val tool: String? = null,
    val success: Boolean
)

@Serializable
data class AgentEditInfo(
    val file: String,
    val operation: String,
    val content: String? = null
)

@Serializable
sealed interface AgentEvent {
    @Serializable
    data class IterationStart(val current: Int, val max: Int) : AgentEvent

    @Serializable
    data class LLMResponseChunk(val chunk: String) : AgentEvent

    @Serializable
    data class ToolCall(val toolName: String, val params: String) : AgentEvent

    @Serializable
    data class ToolResult(
        val toolName: String,
        val success: Boolean,
        val output: String?
    ) : AgentEvent

    @Serializable
    data class CloneLog(val message: String, val isError: Boolean = false) : AgentEvent

    @Serializable
    data class CloneProgress(val stage: String, val progress: Int? = null) : AgentEvent

    @Serializable
    data class Error(val message: String) : AgentEvent

    @Serializable
    data class Complete(
        val success: Boolean,
        val message: String,
        val iterations: Int,
        val steps: List<AgentStepInfo>,
        val edits: List<AgentEditInfo>
    ) : AgentEvent
}


/**
 * Remote Agent Events - mirrors AgentEvent from mpp-server
 */
sealed class RemoteAgentEvent {
    data class CloneProgress(val stage: String, val progress: Int?) : RemoteAgentEvent()
    data class CloneLog(val message: String, val isError: Boolean) : RemoteAgentEvent()
    data class Iteration(val current: Int, val max: Int) : RemoteAgentEvent()
    data class LLMChunk(val chunk: String) : RemoteAgentEvent()
    data class ToolCall(val toolName: String, val params: String) : RemoteAgentEvent()
    data class ToolResult(val toolName: String, val success: Boolean, val output: String?) : RemoteAgentEvent()
    data class Error(val message: String) : RemoteAgentEvent()
    data class Complete(
        val success: Boolean,
        val message: String,
        val iterations: Int,
        val steps: List<AgentStepInfo>,
        val edits: List<AgentEditInfo>
    ) : RemoteAgentEvent()
}

@Serializable
data class CloneProgressData(val stage: String, val progress: Int?)

@Serializable
data class CloneLogData(val message: String, val isError: Boolean? = false)

@Serializable
data class IterationData(val current: Int, val max: Int)

@Serializable
data class LLMChunkData(val chunk: String)

@Serializable
data class ToolCallData(val toolName: String, val params: String)

@Serializable
data class ToolResultData(val toolName: String, val success: Boolean, val output: String?)

@Serializable
data class ErrorData(val message: String)

@Serializable
data class CompleteData(
    val success: Boolean,
    val message: String,
    val iterations: Int,
    val steps: List<AgentStepInfo>,
    val edits: List<AgentEditInfo>
)