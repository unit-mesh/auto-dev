package cc.unitmesh.devti.mcp

import cc.unitmesh.devti.settings.customize.customizeSetting
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ProjectManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class McpConfig(
    val mcpServers: Map<String, McpServer>
)

@Serializable
data class McpServer(
    val command: String,
    val args: List<String>,
    val disabled: Boolean? = null,
    val autoApprove: List<String>? = null,
    val env: Map<String, String>? = null,
    val requiresConfirmation: List<String>? = null
) {

    companion object {
        fun load(): McpConfig? {
            val project = ProjectManager.getInstance().openProjects.first()
            return tryParse(project.customizeSetting.mcpServerConfig)
        }

        fun tryParse(configs: String?): McpConfig? {
            if (configs.isNullOrEmpty()) {
                return null
            }

            try {
                return Json.decodeFromString(configs)
            } catch (e: Exception) {
                logger<McpServer>().warn("Not found mcp config: $e")
            }

            return null
        }
    }
}
