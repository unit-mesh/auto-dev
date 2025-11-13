package cc.unitmesh.llm

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
    GLM("GLM"),
    QWEN("Qwen"),
    KIMI("Kimi"),
    CUSTOM_OPENAI_BASE("custom-openai-base");

    companion object {
        fun fromDisplayName(name: String): LLMProviderType? {
            return entries.find { it.displayName == name }
        }
    }
}

/**
 * LLM 模型配置 - 只负责存储配置信息
 * 职责：
 * 1. 存储 LLM 连接配置（Provider、API Key、Base URL 等）
 * 2. 存储模型参数配置（temperature、maxTokens 等）
 * 3. 验证配置有效性
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
    /**
     * 验证配置是否有效
     */
    fun isValid(): Boolean {
        return when (provider) {
            LLMProviderType.OLLAMA -> 
                modelName.isNotEmpty() && baseUrl.isNotEmpty()
            LLMProviderType.GLM, LLMProviderType.QWEN, LLMProviderType.KIMI, LLMProviderType.CUSTOM_OPENAI_BASE -> 
                apiKey.isNotEmpty() && modelName.isNotEmpty() && baseUrl.isNotEmpty()
            else -> 
                apiKey.isNotEmpty() && modelName.isNotEmpty()
        }
    }

    companion object {
        /**
         * 创建默认配置
         */
        fun default() = ModelConfig()

        @Deprecated(
            message = "Use ModelRegistry.getAvailableModels() instead",
            replaceWith = ReplaceWith("ModelRegistry.getAvailableModels(provider)", "cc.unitmesh.llm.ModelRegistry")
        )
        fun getDefaultModelsForProvider(provider: LLMProviderType): List<String> {
            return ModelRegistry.getAvailableModels(provider)
        }

        @Deprecated(
            message = "Use ModelRegistry.createModel() instead",
            replaceWith = ReplaceWith("ModelRegistry.createModel(provider, modelName)", "cc.unitmesh.llm.ModelRegistry")
        )
        fun getDefaultModelForProvider(provider: LLMProviderType, modelName: String): ai.koog.prompt.llm.LLModel? {
            return ModelRegistry.createModel(provider, modelName)
        }
    }
}

