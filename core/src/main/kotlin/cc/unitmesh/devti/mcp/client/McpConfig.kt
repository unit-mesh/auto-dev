package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.a2a.A2aServer
import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class McpConfig(
    val mcpServers: Map<String, McpServer> = emptyMap(),
    val a2aServers: Map<String, A2aServer> = emptyMap()
)

@Serializable
data class McpServer(
    val command: String? = null,
    val url: String? = null,
    val args: List<String> = emptyList(),
    val disabled: Boolean? = null,
    val autoApprove: List<String>? = null,
    val env: Map<String, String>? = null,
    val requiresConfirmation: List<String>? = null
) {

    companion object {
        fun load(mcpServerConfig: String): McpConfig? {
            return tryParse(mcpServerConfig)
        }

        fun tryParse(configs: String?): McpConfig? {
            if (configs.isNullOrEmpty()) {
                return null
            }

            try {
                return Json.Default.decodeFromString(configs)
            } catch (e: Exception) {
                logger<McpServer>().warn("Not found mcp config: $e")
            }

            return null
        }
    }
}