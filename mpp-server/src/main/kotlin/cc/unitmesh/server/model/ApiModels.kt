package cc.unitmesh.server.model

import kotlinx.serialization.Serializable

// Health Check
@Serializable
data class HealthResponse(
    val status: String,
    val version: String = "1.0.0"
)

// Project Management
@Serializable
data class ProjectInfo(
    val id: String,
    val name: String,
    val path: String,
    val description: String? = null
)

@Serializable
data class ProjectListResponse(
    val projects: List<ProjectInfo>
)

// Agent Execution
@Serializable
data class AgentRequest(
    val projectId: String,
    val task: String
)

@Serializable
data class AgentResponse(
    val success: Boolean,
    val message: String,
    val output: String? = null,
    val iterations: Int = 0,
    val steps: List<AgentStepInfo> = emptyList(),
    val edits: List<AgentEditInfo> = emptyList()
)

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

// SSE Events
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

