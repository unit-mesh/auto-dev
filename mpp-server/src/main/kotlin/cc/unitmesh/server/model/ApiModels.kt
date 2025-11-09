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
    val output: String? = null
)

