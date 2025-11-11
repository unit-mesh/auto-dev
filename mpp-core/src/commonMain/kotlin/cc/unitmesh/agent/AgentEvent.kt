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

    companion object {
        private val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun from(eventType: String, data: String): RemoteAgentEvent? {
            return try {
                when (eventType) {
                    "clone_progress" -> {
                        val parsed = json.decodeFromString<CloneProgressData>(data)
                        RemoteAgentEvent.CloneProgress(parsed.stage, parsed.progress)
                    }
                    "clone_log" -> {
                        val parsed = json.decodeFromString<CloneLogData>(data)
                        RemoteAgentEvent.CloneLog(parsed.message, parsed.isError ?: false)
                    }
                    "iteration" -> {
                        val parsed = json.decodeFromString<IterationData>(data)
                        RemoteAgentEvent.Iteration(parsed.current, parsed.max)
                    }
                    "llm_chunk" -> {
                        val parsed = json.decodeFromString<LLMChunkData>(data)
                        RemoteAgentEvent.LLMChunk(parsed.chunk)
                    }
                    "tool_call" -> {
                        val parsed = json.decodeFromString<ToolCallData>(data)
                        RemoteAgentEvent.ToolCall(parsed.toolName, parsed.params)
                    }
                    "tool_result" -> {
                        val parsed = json.decodeFromString<ToolResultData>(data)
                        RemoteAgentEvent.ToolResult(parsed.toolName, parsed.success, parsed.output)
                    }
                    "error" -> {
                        val parsed = json.decodeFromString<ErrorData>(data)
                        RemoteAgentEvent.Error(parsed.message)
                    }
                    "complete" -> {
                        val parsed = json.decodeFromString<CompleteData>(data)
                        RemoteAgentEvent.Complete(
                            parsed.success,
                            parsed.message,
                            parsed.iterations,
                            parsed.steps,
                            parsed.edits
                        )
                    }
                    else -> {
                        println("Unknown SSE event type: $eventType")
                        null
                    }
                }
            } catch (e: Exception) {
                println("Failed to parse SSE event: $e")
                null
            }
        }

    }
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