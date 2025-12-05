package cc.unitmesh.devti.agent.extention

import kotlinx.serialization.Serializable

@Serializable
data class McpConfig(
    val mcpServers: Map<String, McpServer> = emptyMap(),
    val a2aServers: Map<String, A2aServer> = emptyMap()
)
