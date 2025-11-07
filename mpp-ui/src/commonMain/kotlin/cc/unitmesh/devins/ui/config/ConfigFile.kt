package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import kotlinx.serialization.Serializable

/**
 * Root configuration file structure
 *
 * Maps to ~/.autodev/config.yaml format:
 * ```yaml
 * active: default
 * configs:
 *   - name: default
 *     provider: openai
 *     apiKey: sk-...
 *     model: gpt-4
 *   - name: work
 *     provider: anthropic
 *     apiKey: sk-ant-...
 *     model: claude-3-opus
 * mcpServers:
 *   filesystem:
 *     command: npx
 *     args: ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/allowed/files"]
 * ```
 */
@Serializable
data class ConfigFile(
    val active: String = "",
    val configs: List<NamedModelConfig> = emptyList(),
    val mcpServers: Map<String, McpServerConfig> = emptyMap(),
    val language: String = "en"  // Language preference: "en" or "zh"
)

class AutoDevConfigWrapper(val configFile: ConfigFile) {
    fun getActiveConfig(): NamedModelConfig? {
        if (configFile.active.isEmpty() || configFile.configs.isEmpty()) {
            return null
        }

        return configFile.configs.find { it.name == configFile.active }
            ?: configFile.configs.firstOrNull()
    }

    fun getAllConfigs(): List<NamedModelConfig> = configFile.configs

    fun getActiveName(): String = configFile.active

    fun isValid(): Boolean {
        val active = getActiveConfig() ?: return false

        if (active.provider.equals("ollama", ignoreCase = true)) {
            return active.model.isNotEmpty()
        }

        return active.provider.isNotEmpty() &&
            active.apiKey.isNotEmpty() &&
            active.model.isNotEmpty()
    }


    fun getActiveModelConfig(): ModelConfig? {
        return getActiveConfig()?.toModelConfig()
    }

    fun getMcpServers(): Map<String, McpServerConfig> {
        return configFile.mcpServers
    }

    fun getEnabledMcpServers(): Map<String, McpServerConfig> {
        return configFile.mcpServers.filter { !it.value.disabled && it.value.validate() }
    }
}
