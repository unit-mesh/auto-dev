package cc.unitmesh.devti.language.compiler.exec.agents

import kotlinx.serialization.Serializable

/**
 * Request format for agents command
 */
@Serializable
data class AgentRequest(
    val agent: String,
    val message: String
)