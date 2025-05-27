package cc.unitmesh.devti.llm2.model

import cc.unitmesh.devti.settings.AutoDevSettingsState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.text.ifEmpty

@Serializable
data class Auth(
    val type: String,
    val token: String,
    val expiredAt: Int = 0, // in seconds, 0 means never expire
)

/**
 * 用户保存的 LLM 配置荐，[modelType] Default 表示未应用于任何场景
 */
@Serializable
data class LlmConfig(
    val name: String,
    val description: String = "",
    val url: String,
    val auth: Auth,
    val requestFormat: String,
    val responseFormat: String,
    val modelType: ModelType = ModelType.Default
) {
    companion object {
        fun load(): List<LlmConfig> {
            val llms = AutoDevSettingsState.getInstance().customLlms.trim()
            if (llms.isEmpty()) {
                return emptyList()
            }

            val configs: List<LlmConfig> = try {
                Json.decodeFromString(llms)
            } catch (e: Exception) {
                throw Exception("Failed to load custom llms: $e")
            }

            return configs
        }

        fun load(modelType: ModelType): List<LlmConfig> = load().filter { it.modelType == modelType }

        fun hasPlanModel(): Boolean = load(ModelType.Plan).isNotEmpty()

        fun default(): LlmConfig {
            val state = AutoDevSettingsState.getInstance()

            // Try to use the new default model configuration first
            if (state.defaultModelId.isNotEmpty()) {
                // Find the model by ID in custom LLMs
                val customLlms = load()
                val defaultModel = customLlms.find { it.name == state.defaultModelId }
                if (defaultModel != null) {
                    return defaultModel
                }
            }

            // Fallback to legacy configuration for backward compatibility
            @Suppress("DEPRECATION")
            val modelName = state.customModel.ifEmpty { "gpt-3.5-turbo" }
            @Suppress("DEPRECATION")
            val serverUrl = state.customEngineServer.ifEmpty { "https://api.openai.com/v1/chat/completions" }
            @Suppress("DEPRECATION")
            val token = state.customEngineToken
            @Suppress("DEPRECATION")
            val requestFormat = state.customEngineRequestFormat.ifEmpty {
                """{ "customFields": {"model": "$modelName", "temperature": 0.0, "stream": true} }"""
            }
            @Suppress("DEPRECATION")
            val responseFormat = state.customEngineResponseFormat.ifEmpty {
                "\$.choices[0].delta.content"
            }

            return LlmConfig(
                name = modelName,
                description = "Legacy default configuration",
                url = serverUrl,
                auth = Auth(
                    type = "Bearer",
                    token = token,
                ),
                requestFormat = requestFormat,
                responseFormat = responseFormat,
                modelType = ModelType.Default,
            )
        }

        /**
         * Get the appropriate model for a specific category
         */
        fun forCategory(modelType: ModelType): LlmConfig {
            val state = AutoDevSettingsState.getInstance()

            // If using default for all categories, return the default model
            if (state.useDefaultForAllCategories) {
                return default()
            }

            // Get category-specific model ID
            val modelId = when (modelType) {
                ModelType.Plan -> state.selectedPlanModelId
                ModelType.Act -> state.selectedActModelId
                ModelType.Completion -> state.selectedCompletionModelId
                ModelType.Embedding -> state.selectedEmbeddingModelId
                ModelType.FastApply -> state.selectedFastApplyModelId
                else -> state.defaultModelId
            }

            // If no specific model is set, use default
            if (modelId.isEmpty()) {
                return default()
            }

            // Find the model by ID
            val customLlms = load()
            val specificModel = customLlms.find { it.name == modelId }

            // Return specific model or fallback to default
            return specificModel ?: default()
        }
    }
}

@Serializable
enum class ModelType {
    Default, Plan, Act, Completion, Embedding, FastApply
}
