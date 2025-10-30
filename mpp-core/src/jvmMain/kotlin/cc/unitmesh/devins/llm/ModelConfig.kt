package cc.unitmesh.devins.llm

import kotlinx.serialization.Serializable

/**
 * LLM Provider types supported by Koog
 */
enum class LLMProviderType(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    GOOGLE("Google"),
    DEEPSEEK("DeepSeek"),
    OLLAMA("Ollama"),
    OPENROUTER("OpenRouter"),
    BEDROCK("AWS Bedrock");

    companion object {
        fun fromDisplayName(name: String): LLMProviderType? {
            return entries.find { it.displayName == name }
        }
    }
}

/**
 * Model configuration for LLM
 */
@Serializable
data class ModelConfig(
    val provider: LLMProviderType = LLMProviderType.DEEPSEEK,
    val modelName: String = "deepseek-chat",
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2000,
    val topP: Double = 1.0,
    val baseUrl: String = "" // For custom endpoints like Ollama
) {
    fun isValid(): Boolean {
        return when (provider) {
            LLMProviderType.OLLAMA -> modelName.isNotEmpty() && baseUrl.isNotEmpty()
            else -> apiKey.isNotEmpty() && modelName.isNotEmpty()
        }
    }

    companion object {
        fun default() = ModelConfig()

        /**
         * Get default models for each provider
         */
        fun getDefaultModelsForProvider(provider: LLMProviderType): List<String> {
            return when (provider) {
                LLMProviderType.OPENAI -> listOf(
                    "gpt-4o",
                    "gpt-4o-mini", 
                    "gpt-4-turbo",
                    "gpt-3.5-turbo"
                )
                LLMProviderType.ANTHROPIC -> listOf(
                    "claude-3-5-sonnet-20241022",
                    "claude-3-5-haiku-20241022",
                    "claude-3-opus-20240229"
                )
                LLMProviderType.GOOGLE -> listOf(
                    "gemini-2.0-flash-exp",
                    "gemini-1.5-pro",
                    "gemini-1.5-flash"
                )
                LLMProviderType.DEEPSEEK -> listOf(
                    "deepseek-chat",
                    "deepseek-reasoner"
                )
                LLMProviderType.OLLAMA -> listOf(
                    "llama3.2",
                    "llama3.1",
                    "qwen2.5",
                    "mistral"
                )
                LLMProviderType.OPENROUTER -> listOf(
                    "openai/gpt-4o",
                    "anthropic/claude-3.5-sonnet",
                    "google/gemini-pro"
                )
                LLMProviderType.BEDROCK -> listOf(
                    "anthropic.claude-3-sonnet",
                    "anthropic.claude-3-haiku",
                    "meta.llama3-70b"
                )
            }
        }
    }
}

