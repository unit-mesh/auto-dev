package cc.unitmesh.devti.llm2.model

import cc.unitmesh.devti.settings.AutoDevSettingsState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.text.ifEmpty

@Serializable
data class Auth(
    val type: String,
    val token: String
)

@Serializable
data class LlmConfig(
    val name: String,
    val description: String = "",
    val url: String,
    val auth: Auth,
    val requestFormat: String,
    val responseFormat: String,
    val modelType: ModelType = ModelType.Others
) {
    companion object {
        fun load(): List<LlmConfig> {
            val configs: List<LlmConfig> = try {
                val llms = AutoDevSettingsState.getInstance().customLlms
                Json.decodeFromString(llms)
            } catch (e: Exception) {
                throw Exception("Failed to load custom llms: $e")
            }

            return configs
        }

        fun load(modelType: ModelType): List<LlmConfig> = load().filter { it.modelType == modelType }

        fun default(): LlmConfig {
            val state = AutoDevSettingsState.getInstance()
            val modelName = state.customModel
            return LlmConfig(
                name = modelName,
                description = "",
                url = state.customEngineServer,
                auth = Auth(
                    type = "Bearer",
                    token = state.customEngineToken,
                ),
                requestFormat = state.customEngineRequestFormat.ifEmpty {
                    """{ "customFields": {"model": "$modelName", "temperature": 0.0, "stream": true} }"""
                },
                responseFormat = state.customEngineResponseFormat.ifEmpty {
                    "\$.choices[0].delta.content"
                },
                modelType = ModelType.Default,
            )
        }
    }
}

@Serializable
enum class ModelType {
    Default, Plan, Act, Completion, Embedding, FastApply, Others
}
