package cc.unitmesh.llm

import kotlinx.serialization.Serializable

/**
 * Named model configuration for multi-config support
 */
@Serializable
public data class NamedModelConfig(
    val name: String,
    val provider: String,
    val apiKey: String,
    val model: String,
    val baseUrl: String = "",
    val temperature: Double = 0.0,
    val maxTokens: Int = 128000
) {
    /**
     * Convert to ModelConfig for use with LLM services
     */
    fun toModelConfig(): ModelConfig {
        // Try to match by enum name first, handling both underscore and hyphen
        val normalizedProvider = provider.replace("-", "_").uppercase()
        val providerType =
            LLMProviderType.entries.find {
                it.name == normalizedProvider
            } ?: LLMProviderType.OPENAI

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