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
                baseUrl = config.baseUrl.trimEnd('/'),
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )
        }
    }
}

class AutoDevConfigWrapper(private val configFile: ConfigFile) {
    fun getConfigFile(): ConfigFile = configFile

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
