package cc.unitmesh.llm

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

/**
 * 模型注册中心 - 负责管理所有支持的模型定义
 * 职责：
 * 1. 提供各个 Provider 的默认模型列表
 * 2. 根据 Provider 和模型名称创建 LLModel 对象
 * 3. 查询模型信息
 */
object ModelRegistry {
    
    /**
     * 获取指定 Provider 支持的所有模型名称列表
     */
    fun getAvailableModels(provider: LLMProviderType): List<String> {
        return when (provider) {
            LLMProviderType.OPENAI -> OpenAIModels.all
            LLMProviderType.ANTHROPIC -> AnthropicModels.all
            LLMProviderType.GOOGLE -> GoogleModels.all
            LLMProviderType.DEEPSEEK -> DeepSeekModels.all
            LLMProviderType.OPENROUTER -> OpenRouterModels.all
            LLMProviderType.OLLAMA -> OllamaModels.all
        }
    }

    /**
     * 根据 Provider 和模型名称创建 LLModel 对象
     */
    fun createModel(provider: LLMProviderType, modelName: String): LLModel? {
        val availableModels = getAvailableModels(provider)
        if (modelName !in availableModels) {
            return null
        }

        return when (provider) {
            LLMProviderType.OPENAI -> OpenAIModels.create(modelName)
            LLMProviderType.ANTHROPIC -> AnthropicModels.create(modelName)
            LLMProviderType.GOOGLE -> GoogleModels.create(modelName)
            LLMProviderType.DEEPSEEK -> DeepSeekModels.create(modelName)
            LLMProviderType.OPENROUTER -> OpenRouterModels.create(modelName)
            LLMProviderType.OLLAMA -> OllamaModels.create(modelName)
        }
    }

    /**
     * 创建通用模型（当模型不在预定义列表中时使用）
     */
    fun createGenericModel(
        provider: LLMProviderType,
        modelName: String,
        contextLength: Long = 128000L
    ): LLModel {
        val llmProvider = when (provider) {
            LLMProviderType.OPENAI -> LLMProvider.OpenAI
            LLMProviderType.ANTHROPIC -> LLMProvider.Anthropic
            LLMProviderType.GOOGLE -> LLMProvider.Google
            LLMProviderType.DEEPSEEK -> LLMProvider.DeepSeek
            LLMProviderType.OLLAMA -> LLMProvider.Ollama
            LLMProviderType.OPENROUTER -> LLMProvider.OpenRouter
        }

        return LLModel(
            provider = llmProvider,
            id = modelName,
            capabilities = listOf(LLMCapability.Completion, LLMCapability.Temperature),
            contextLength = contextLength
        )
    }

    // ============= 内部模型定义 =============

    private object OpenAIModels {
        val all = listOf(
            // Reasoning models
            "o4-mini", "o3-mini", "o3", "o1",
            // Chat models
            "gpt-4o", "gpt-4.1", "gpt-5", "gpt-5-mini", "gpt-5-nano", "gpt-5-codex",
            // Audio models
            "gpt-audio", "gpt-4o-mini-audio-preview", "gpt-4o-audio-preview",
            // Cost optimized
            "gpt-4.1-nano", "gpt-4.1-mini", "gpt-4o-mini",
            // Embeddings
            "text-embedding-3-small", "text-embedding-3-large", "text-embedding-ada-002"
        )

        fun create(modelName: String): LLModel {
            val (contextLength, maxOutputTokens) = when {
                modelName.startsWith("o4-mini") || modelName.startsWith("o3") || modelName.startsWith("o1") ->
                    200_000L to 100_000L
                modelName.startsWith("gpt-4.1") -> 1_047_576L to 32_768L
                modelName.startsWith("gpt-5") -> 400_000L to 128_000L
                modelName.startsWith("gpt-4o") -> 128_000L to 16_384L
                modelName.startsWith("text-embedding") -> 8_191L to null
                else -> 128_000L to 16_384L
            }

            val capabilities = when {
                modelName.startsWith("text-embedding") -> listOf(LLMCapability.Embed)
                modelName.contains("audio") -> listOf(
                    LLMCapability.Temperature,
                    LLMCapability.Completion,
                    LLMCapability.Tools,
                    LLMCapability.ToolChoice,
                    LLMCapability.Audio
                )
                else -> listOf(
                    LLMCapability.Temperature,
                    LLMCapability.Tools,
                    LLMCapability.ToolChoice,
                    LLMCapability.Vision.Image,
                    LLMCapability.Document,
                    LLMCapability.Completion,
                    LLMCapability.MultipleChoices
                )
            }

            return LLModel(
                provider = LLMProvider.OpenAI,
                id = modelName,
                capabilities = capabilities,
                contextLength = contextLength,
                maxOutputTokens = maxOutputTokens
            )
        }
    }

    private object AnthropicModels {
        val all = listOf(
            "claude-3-opus", "claude-3-haiku", "claude-3-5-sonnet", "claude-3-5-haiku",
            "claude-3-7-sonnet", "claude-sonnet-4-0", "claude-opus-4-0", "claude-opus-4-1",
            "claude-sonnet-4-5", "claude-haiku-4-5"
        )

        fun create(modelName: String): LLModel {
            val (contextLength, maxOutputTokens) = when {
                modelName.contains("3-7") || modelName.contains("4-5") || modelName.contains("sonnet-4") ->
                    200_000L to 64_000L
                modelName.contains("opus-4") -> 200_000L to 32_000L
                modelName.contains("3-5") -> 200_000L to 8_192L
                else -> 200_000L to 4_096L
            }

            return LLModel(
                provider = LLMProvider.Anthropic,
                id = modelName,
                capabilities = listOf(
                    LLMCapability.Temperature,
                    LLMCapability.Tools,
                    LLMCapability.ToolChoice,
                    LLMCapability.Vision.Image,
                    LLMCapability.Document,
                    LLMCapability.Completion
                ),
                contextLength = contextLength,
                maxOutputTokens = maxOutputTokens
            )
        }
    }

    private object GoogleModels {
        val all = listOf(
            "gemini-2.0-flash", "gemini-2.0-flash-001", "gemini-2.0-flash-lite",
            "gemini-2.0-flash-lite-001", "gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite"
        )

        fun create(modelName: String): LLModel {
            val (contextLength, maxOutputTokens) = when {
                modelName.contains("2.5") -> 1_048_576L to 65_536L
                else -> 1_048_576L to 8_192L
            }

            return LLModel(
                provider = LLMProvider.Google,
                id = modelName,
                capabilities = listOf(
                    LLMCapability.Temperature,
                    LLMCapability.Completion,
                    LLMCapability.MultipleChoices,
                    LLMCapability.Tools,
                    LLMCapability.ToolChoice,
                    LLMCapability.Vision.Image,
                    LLMCapability.Vision.Video,
                    LLMCapability.Audio
                ),
                contextLength = contextLength,
                maxOutputTokens = maxOutputTokens
            )
        }
    }

    private object DeepSeekModels {
        val all = listOf("deepseek-chat", "deepseek-reasoner")

        fun create(modelName: String): LLModel {
            val (contextLength, maxOutputTokens) = when {
                modelName == "deepseek-reasoner" -> 64_000L to 64_000L
                else -> 64_000L to 8_000L
            }

            return LLModel(
                provider = LLMProvider.DeepSeek,
                id = modelName,
                capabilities = listOf(
                    LLMCapability.Completion,
                    LLMCapability.Temperature,
                    LLMCapability.Tools,
                    LLMCapability.ToolChoice,
                    LLMCapability.MultipleChoices
                ),
                contextLength = contextLength,
                maxOutputTokens = maxOutputTokens
            )
        }
    }

    private object OpenRouterModels {
        val all = listOf(
            // Free models
            "microsoft/phi-4-reasoning:free",
            // Anthropic models
            "anthropic/claude-3-opus", "anthropic/claude-3-sonnet", "anthropic/claude-3-haiku",
            "anthropic/claude-3.5-sonnet", "anthropic/claude-3.7-sonnet", "anthropic/claude-sonnet-4",
            "anthropic/claude-opus-4.1",
            // OpenAI models
            "openai/gpt-4o-mini", "openai/gpt-5-chat", "openai/gpt-5", "openai/gpt-5-mini",
            "openai/gpt-5-nano", "openai/gpt-oss-120b", "openai/gpt-4", "openai/gpt-4o",
            "openai/gpt-4-turbo", "openai/gpt-3.5-turbo",
            // Meta models
            "meta/llama-3-70b", "meta/llama-3-70b-instruct",
            // Mistral models
            "mistralai/mistral-7b-instruct", "mistralai/mixtral-8x7b-instruct",
            // Google models
            "google/gemini-2.5-flash-lite", "google/gemini-2.5-flash", "google/gemini-2.5-pro",
            // DeepSeek models
            "deepseek/deepseek-chat-v3-0324",
            // Qwen models
            "qwen/qwen-2.5-72b-instruct"
        )

        fun create(modelName: String): LLModel {
            val (contextLength, maxOutputTokens) = when {
                modelName.contains("claude") -> when {
                    modelName.contains("3-7") || modelName.contains("4-5") || modelName.contains("sonnet-4") ->
                        200_000L to 64_000L
                    modelName.contains("opus-4") -> 200_000L to 32_000L
                    modelName.contains("3-5") -> 200_000L to 8_200L
                    else -> 200_000L to 4_096L
                }
                modelName.contains("gpt-5") -> 400_000L to 128_000L
                modelName.contains("gpt-4") -> when {
                    modelName.contains("4o") -> 128_000L to 16_400L
                    else -> 32_768L to null
                }
                modelName.contains("gpt-3.5") -> 16_385L to null
                modelName.contains("gemini") -> 1_048_576L to 65_600L
                modelName.contains("deepseek") -> 163_800L to 163_800L
                modelName.contains("llama") -> 8_000L to null
                modelName.contains("mistral") -> 32_768L to null
                modelName.contains("qwen") -> 131_072L to 8_192L
                else -> 32_768L to null
            }

            val capabilities = if (modelName.contains("claude") || modelName.contains("gpt-4") || modelName.contains("gemini")) {
                listOf(
                    LLMCapability.Temperature,
                    LLMCapability.Speculation,
                    LLMCapability.Tools,
                    LLMCapability.Completion,
                    LLMCapability.Vision.Image
                )
            } else {
                listOf(
                    LLMCapability.Temperature,
                    LLMCapability.Speculation,
                    LLMCapability.Tools,
                    LLMCapability.Completion
                )
            }

            return LLModel(
                provider = LLMProvider.OpenRouter,
                id = modelName,
                capabilities = capabilities,
                contextLength = contextLength,
                maxOutputTokens = maxOutputTokens
            )
        }
    }

    private object OllamaModels {
        val all = listOf(
            "llama3.2", "llama3.1", "qwen2.5", "deepseek-coder",
            "codellama", "mistral", "gemma2"
        )

        fun create(modelName: String): LLModel {
            return LLModel(
                provider = LLMProvider.Ollama,
                id = modelName,
                capabilities = listOf(
                    LLMCapability.Completion,
                    LLMCapability.Tools,
                    LLMCapability.Temperature
                ),
                contextLength = 128_000L,
                maxOutputTokens = null
            )
        }
    }
}
