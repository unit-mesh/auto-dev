package cc.unitmesh.devti.llm2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.exp

@Serializable
data class CopilotModelsResponse(
    val data: List<CopilotModel>
)

@Serializable
data class CopilotModel(
    val capabilities: ModelCapabilities,
    val id: String,
    @SerialName("model_picker_enabled")
    val modelPickerEnabled: Boolean,
    val name: String,
    val `object`: String,
    val preview: Boolean,
    val vendor: String,
    val version: String,
    val policy: ModelPolicy? = null,
) {
    val isEmbedding: Boolean
        get() = capabilities.type == "embeddings"

    fun asLlmConfig(auth: Auth): LlmConfig {
        val modelType = when {
            isEmbedding -> ModelType.Embedding
            id.contains("completion") -> ModelType.Completion
            else -> ModelType.Default
        }

        val requestFormat = """
            {
              "customFields": {
                "model": "$id",
                "temperature": 0.0,
                "stream": true,
                "headers": {
                  "Editor-Version": "Zed/Unknow",
                  "Copilot-Integration-Id": "vscode-chat"
                }
              }
            }
        """.trimIndent()

        return LlmConfig(
            name = name,
            description = "$vendor $name (${if (preview) "预览版" else "稳定版"})",
            url = "https://api.githubcopilot.com/chat/completions",
            auth = auth,
            requestFormat = requestFormat,
            responseFormat = "\$.choices[0].delta.content", // 流式响应的标准 JSON 路径
            modelType = modelType
        )
    }
}

@Serializable
data class ModelPolicy(
    val state: String? = null,
    val terms: String? = null
)

@Serializable
data class ModelCapabilities(
    val family: String? = null,
    val limits: ModelLimits? = null,
    val `object`: String? = null,
    val supports: ModelSupports? = null,
    val tokenizer: String? = null,
    val type: String? = null
)

@Serializable
data class ModelLimits(
    @SerialName("max_context_window_tokens")
    val maxContextWindowTokens: Int? = null,
    @SerialName("max_prompt_tokens")
    val maxPromptTokens: Int? = null,
    @SerialName("max_output_tokens")
    val maxOutputTokens: Int? = null,
    @SerialName("max_inputs")
    val maxInputs: Int? = null,
    val vision: VisionLimits? = null
)

@Serializable
data class VisionLimits(
    @SerialName("max_prompt_image_size")
    val maxPromptImageSize: Int? = null,
    @SerialName("max_prompt_images")
    val maxPromptImages: Int? = null,
    @SerialName("supported_media_types")
    val supportedMediaTypes: List<String>? = null
)

@Serializable
data class ModelSupports(
    val streaming: Boolean? = null,
    @SerialName("tool_calls")
    val toolCalls: Boolean? = null,
    @SerialName("parallel_tool_calls")
    val parallelToolCalls: Boolean? = null,
    val vision: Boolean? = null,
    @SerialName("structured_outputs")
    val structuredOutputs: Boolean? = null,
    val dimensions: Boolean? = null
)
