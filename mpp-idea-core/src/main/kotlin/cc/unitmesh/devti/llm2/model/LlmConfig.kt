package cc.unitmesh.devti.llm2.model

import cc.unitmesh.devti.settings.AutoDevSettingsState
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.text.ifEmpty

@Serializable
data class Auth(
    val type: String,
    val token: String = "", // Optional for GitHub Copilot (dynamic refresh)
    val expiredAt: Int = 0, // in seconds, 0 means never expire
)

@Serializable
data class CustomRequest(
    val headers: Map<String, String> = emptyMap(), // Custom headers
    val body: Map<String, JsonElement> = emptyMap(), // Custom body fields
    val stream: Boolean = true // Whether to use streaming response
) {
    companion object {
        fun fromLegacyFormat(requestFormat: String): CustomRequest {
            return try {
                val jsonElement = Json.parseToJsonElement(requestFormat)
                if (jsonElement !is JsonObject) {
                    return getDefault()
                }

                val customFields = jsonElement["customFields"]?.jsonObject
                val customHeaders = jsonElement["customHeaders"]?.jsonObject

                val headers = customHeaders?.mapValues { (_, value) ->
                    when (value) {
                        is JsonPrimitive -> value.content
                        else -> value.toString().removeSurrounding("\"")
                    }
                } ?: emptyMap()

                val body = customFields?.mapValues { (_, value) ->
                    value // Keep as JsonElement
                } ?: emptyMap()

                val stream = when (val streamValue = body["stream"]) {
                    is JsonPrimitive -> {
                        when {
                            streamValue.booleanOrNull != null -> streamValue.boolean
                            streamValue.isString -> streamValue.content.toBoolean()
                            else -> true
                        }
                    }
                    else -> true
                }

                CustomRequest(headers, body, stream)
            } catch (e: Exception) {
                getDefault()
            }
        }

        private fun getDefault(): CustomRequest {
            return CustomRequest(
                body = mapOf(
                    "model" to JsonPrimitive("gpt-3.5-turbo"),
                    "temperature" to JsonPrimitive(0.0),
                    "stream" to JsonPrimitive(true)
                )
            )
        }
    }
}

/**
 * 用户保存的 LLM 配置，[modelType] Default 表示未应用于任何场景
 */
@Serializable
data class LlmConfig(
    val name: String,
    val description: String = "",
    val url: String,
    val auth: Auth = Auth("Bearer"), // Optional token for GitHub Copilot
    val maxTokens: Int = 4096, // Maximum tokens for the model
    val customRequest: CustomRequest = CustomRequest(), // Custom headers and body
    val modelType: ModelType = ModelType.Default,

    // Legacy fields for backward compatibility
    @Deprecated("Use customRequest instead")
    val requestFormat: String = "",
    @Deprecated("Use customRequest.stream to determine response format")
    val responseFormat: String = ""
) {

    /**
     * Get the response format based on stream setting
     */
    fun getResponseFormatByStream(): String {
        return if (customRequest.stream) {
            "\$.choices[0].delta.content" // Streaming format
        } else {
            "\$.choices[0].message.content" // Non-streaming format
        }
    }

    /**
     * Convert to legacy request format for compatibility
     */
    fun toLegacyRequestFormat(): String {
        return buildJsonObject {
            if (customRequest.headers.isNotEmpty()) {
                put("customHeaders", buildJsonObject {
                    customRequest.headers.forEach { (key, value) ->
                        put(key, value)
                    }
                })
            }
            put("customFields", buildJsonObject {
                customRequest.body.forEach { (key, value) ->
                    put(key, value)
                }
            })
        }.toString()
    }
    companion object {
        fun load(): List<LlmConfig> {
            val llms = AutoDevSettingsState.getInstance().customLlms.trim()
            if (llms.isEmpty()) {
                return emptyList()
            }

            val configs: List<LlmConfig> = try {
                // Use a JSON configuration that's more lenient with unknown properties
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json.decodeFromString(llms)
            } catch (e: Exception) {
                // Log the error but don't throw - try to recover
                println("Warning: Failed to load custom llms, attempting recovery: $e")

                // Try to recover by attempting to parse individual configs
                try {
                    recoverFromCorruptedConfig(llms)
                } catch (recoveryException: Exception) {
                    println("Error: Could not recover from corrupted config: $recoveryException")
                    throw Exception("Failed to load custom llms: $e")
                }
            }

            return configs
        }

        /**
         * Attempt to recover from corrupted configuration by parsing individual configs
         */
        private fun recoverFromCorruptedConfig(llms: String): List<LlmConfig> {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            // Try to parse as JSON array and fix individual items
            val jsonElement = json.parseToJsonElement(llms)
            if (jsonElement !is JsonArray) {
                return emptyList()
            }

            val recoveredConfigs = mutableListOf<LlmConfig>()

            for (element in jsonElement) {
                try {
                    if (element is JsonObject) {
                        // Check if this config has the legacy "Others" modelType and fix it
                        val mutableElement = element.toMutableMap()
                        val modelType = mutableElement["modelType"]
                        if (modelType is JsonPrimitive && modelType.content == "Others") {
                            mutableElement["modelType"] = JsonPrimitive("Default")
                        }

                        val fixedElement = JsonObject(mutableElement)
                        val config = json.decodeFromJsonElement<LlmConfig>(fixedElement)
                        recoveredConfigs.add(config)
                    }
                } catch (e: Exception) {
                    println("Warning: Skipping corrupted config item: $e")
                    // Continue with next config
                }
            }

            return recoveredConfigs
        }

        /**
         * Check if a model ID represents a GitHub Copilot model
         */
        private fun isGithubModel(modelId: String): Boolean {
            // First, try to check against the actual GitHub models list if available
            try {
                val manager = com.intellij.openapi.components.service<cc.unitmesh.devti.llm2.GithubCopilotManager>()
                if (manager.isInitialized()) {
                    val githubModels = manager.getSupportedModels(forceRefresh = false)
                    if (githubModels != null) {
                        return githubModels.any { it.id == modelId }
                    }
                }
            } catch (e: Exception) {
                // Fallback to pattern matching if service is not available
            }

            // Fallback: GitHub models are those that are available through GitHub Copilot
            // We can identify them by common GitHub model patterns
            return modelId.startsWith("gpt-") ||
                   modelId.startsWith("claude-") ||
                   modelId.startsWith("o1-") ||
                   modelId.contains("github") ||
                   modelId in listOf("gpt-4", "gpt-4-turbo", "gpt-3.5-turbo", "claude-3-5-sonnet")
        }

        /**
         * Create a GitHub Copilot LlmConfig for the given model ID
         */
        private fun createGithubConfig(modelId: String, modelType: ModelType = ModelType.Default): LlmConfig {
            return LlmConfig(
                name = modelId,
                description = "GitHub Copilot model: $modelId",
                url = "https://api.githubcopilot.com/chat/completions",
                auth = Auth(type = "Bearer", token = ""), // Token will be dynamically obtained
                maxTokens = 4096,
                customRequest = CustomRequest(
                    headers = mapOf(
                        "Editor-Version" to "Zed/Unknown",
                        "Copilot-Integration-Id" to "vscode-chat"
                    ),
                    body = mapOf(
                        "model" to JsonPrimitive(modelId),
                        "temperature" to JsonPrimitive(0.1),
                        "stream" to JsonPrimitive(true)
                    ),
                    stream = true
                ),
                modelType = modelType
            )
        }

        fun load(modelType: ModelType): List<LlmConfig> = load().filter { it.modelType == modelType }

        fun hasPlanModel(): Boolean = load(ModelType.Plan).isNotEmpty()

        fun default(): LlmConfig {
            val state = AutoDevSettingsState.getInstance()

            // Try to use the new default model configuration first
            if (state.defaultModelId.isNotEmpty()) {
                // Check if it's a GitHub model first
                if (isGithubModel(state.defaultModelId)) {
                    return createGithubConfig(state.defaultModelId)
                }

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

            // Convert legacy format to new structure
            val customRequest = CustomRequest.fromLegacyFormat(requestFormat)

            return LlmConfig(
                name = modelName,
                description = "Legacy default configuration",
                url = serverUrl,
                auth = Auth(
                    type = "Bearer",
                    token = token,
                ),
                maxTokens = 4096, // Default max tokens
                customRequest = customRequest,
                modelType = ModelType.Default,
                // Keep legacy fields for compatibility
                requestFormat = requestFormat,
                responseFormat = responseFormat
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

            // Check if it's a GitHub model first
            if (isGithubModel(modelId)) {
                return createGithubConfig(modelId, modelType)
            }

            // Find the model by ID in custom LLMs
            val customLlms = load()
            val specificModel = customLlms.find { it.name == modelId }

            // Return specific model or fallback to default
            return specificModel ?: default()
        }
    }
}

/**
 * Custom serializer for ModelType to handle backward compatibility with legacy "Others" type
 */
@Serializer(forClass = ModelType::class)
object ModelTypeSerializer : KSerializer<ModelType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ModelType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ModelType) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): ModelType {
        val value = decoder.decodeString()
        return when (value) {
            "Others" -> ModelType.Default // Map legacy "Others" to "Default"
            "Default" -> ModelType.Default
            "Plan" -> ModelType.Plan
            "Act" -> ModelType.Act
            "Completion" -> ModelType.Completion
            "Embedding" -> ModelType.Embedding
            "FastApply" -> ModelType.FastApply
            else -> ModelType.Default // Fallback to Default for any unknown types
        }
    }
}

@Serializable(with = ModelTypeSerializer::class)
enum class ModelType {
    Default, Plan, Act, Completion, Embedding, FastApply
}
