package cc.unitmesh.devins.idea.model

import kotlinx.serialization.Serializable

/**
 * LLM provider configuration.
 */
@Serializable
data class LLMConfig(
    val provider: LLMProvider = LLMProvider.OPENAI,
    val apiKey: String = "",
    val modelName: String = "gpt-4",
    val baseUrl: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096
) {
    fun isValid(): Boolean {
        return when (provider) {
            LLMProvider.OLLAMA -> modelName.isNotEmpty() && baseUrl.isNotEmpty()
            else -> apiKey.isNotEmpty() && modelName.isNotEmpty()
        }
    }
}

/**
 * Supported LLM providers.
 */
@Serializable
enum class LLMProvider(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    DEEPSEEK("DeepSeek"),
    OLLAMA("Ollama"),
    OPENROUTER("OpenRouter");

    companion object {
        fun fromDisplayName(name: String): LLMProvider {
            return entries.find { it.displayName.equals(name, ignoreCase = true) }
                ?: OPENAI
        }
    }
}

