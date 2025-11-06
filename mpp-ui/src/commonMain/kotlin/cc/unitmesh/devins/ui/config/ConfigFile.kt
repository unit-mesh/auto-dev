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
    val maxTokens: Int = 8192
) {
    /**
     * Convert to ModelConfig for use with LLM services
     */
    fun toModelConfig(): ModelConfig {
        // Try to match by enum name first, handling both underscore and hyphen
        val normalizedProvider = provider.replace("-", "_").uppercase()
        val providerType =
            cc.unitmesh.llm.LLMProviderType.entries.find {
                it.name == normalizedProvider
            } ?: cc.unitmesh.llm.LLMProviderType.OPENAI

        return ModelConfig(
            provider = providerType,
            modelName = model,
            apiKey = apiKey,
            temperature = temperature,
            maxTokens = maxTokens,
            baseUrl = baseUrl.let { if (it.isNotEmpty() && !it.endsWith('/')) "$it/" else it } // Ensure trailing slash for Ktor URL joining
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
            // Use lowercase with hyphens for better YAML readability
            val providerName = config.provider.name.lowercase().replace("_", "-")
            return NamedModelConfig(
                name = name,
                provider = providerName,
                apiKey = config.apiKey,
                model = config.modelName,
                baseUrl = config.baseUrl.trimEnd('/'), // Remove trailing slash for YAML readability
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
