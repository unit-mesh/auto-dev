package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.llm.ModelConfig
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
    val mcpServers: Map<String, McpServerConfig> = emptyMap()
)

/**
 * Named model configuration for multi-config support
 */
@Serializable
data class NamedModelConfig(
    val name: String,
    val provider: String,
    val apiKey: String,
    val model: String,
    val baseUrl: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096
) {
    /**
     * Convert to ModelConfig for use with LLM services
     */
    fun toModelConfig(): ModelConfig {
        val providerType =
            cc.unitmesh.llm.LLMProviderType.entries.find {
                it.name.equals(provider, ignoreCase = true)
            } ?: cc.unitmesh.llm.LLMProviderType.OPENAI

        return ModelConfig(
            provider = providerType,
            modelName = model,
            apiKey = apiKey,
            temperature = temperature,
            maxTokens = maxTokens,
            baseUrl = baseUrl
        )
    }

    companion object {
        /**
         * Create from ModelConfig
         */
        fun fromModelConfig(
            name: String,
            config: ModelConfig
        ): NamedModelConfig {
            return NamedModelConfig(
                name = name,
                provider = config.provider.name.lowercase(),
                apiKey = config.apiKey,
                model = config.modelName,
                baseUrl = config.baseUrl,
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )
        }
    }
}

/**
 * Wrapper class for configuration with validation and convenience methods
 */
class AutoDevConfigWrapper(private val configFile: ConfigFile) {
    /**
     * Get the entire config file structure
     */
    fun getConfigFile(): ConfigFile = configFile

    /**
     * Get the active configuration
     */
    fun getActiveConfig(): NamedModelConfig? {
        if (configFile.active.isEmpty() || configFile.configs.isEmpty()) {
            return null
        }

        return configFile.configs.find { it.name == configFile.active }
            ?: configFile.configs.firstOrNull()
    }

    /**
     * Get all configurations
     */
    fun getAllConfigs(): List<NamedModelConfig> = configFile.configs

    /**
     * Get active config name
     */
    fun getActiveName(): String = configFile.active

    /**
     * Check if any valid configuration exists
     */
    fun isValid(): Boolean {
        val active = getActiveConfig() ?: return false

        // Ollama doesn't require API key
        if (active.provider.equals("ollama", ignoreCase = true)) {
            return active.model.isNotEmpty()
        }

        return active.provider.isNotEmpty() &&
            active.apiKey.isNotEmpty() &&
            active.model.isNotEmpty()
    }

    /**
     * Get the active config as ModelConfig
     */
    fun getActiveModelConfig(): ModelConfig? {
        return getActiveConfig()?.toModelConfig()
    }

    /**
     * Get MCP server configurations
     */
    fun getMcpServers(): Map<String, McpServerConfig> {
        return configFile.mcpServers
    }

    /**
     * Get enabled MCP servers
     */
    fun getEnabledMcpServers(): Map<String, McpServerConfig> {
        return configFile.mcpServers.filter { !it.value.disabled && it.value.validate() }
    }
}
