package cc.unitmesh.devins.llm

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.list
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
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
    val modelName: String = "",
    val apiKey: String = "",
    val temperature: Double = 0.0,
    val maxTokens: Int = 128000,
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
         * Get available models for each provider using Koog's list() method
         * 使用 Koog 框架的 list() 方法动态获取所有可用模型
         */
        fun getDefaultModelsForProvider(provider: LLMProviderType): List<String> {
            return when (provider) {
                LLMProviderType.OPENAI -> {
                    OpenAIModels.list().map { it.id }
                }
                LLMProviderType.ANTHROPIC -> {
                    AnthropicModels.list().map { it.id }
                }
                LLMProviderType.GOOGLE -> {
                    GoogleModels.list().map { it.id }
                }
                LLMProviderType.DEEPSEEK -> {
                    DeepSeekModels.list().map { it.id }
                }
                LLMProviderType.OPENROUTER -> {
                    OpenRouterModels.list().map { it.id }.ifEmpty {
                        listOf("openai/gpt-4o", "anthropic/claude-4.5-sonnet", "google/gemini-pro")
                    }
                }

                else -> {
                    emptyList()
                }
            }
        }
    }
}

