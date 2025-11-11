package cc.unitmesh.server.model

import cc.unitmesh.agent.AgentEditInfo
import cc.unitmesh.agent.AgentStepInfo
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
    val task: String,
    val llmConfig: LLMConfig? = null,
    val gitUrl: String? = null,
    val branch: String? = null,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class LLMConfig(
    val provider: String,
    val modelName: String,
    val apiKey: String,
    val baseUrl: String = ""
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
