package cc.unitmesh.devti.llm2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val version: String
)

@Serializable
data class ModelCapabilities(
    val family: String,
    val limits: ModelLimits,
    val `object`: String,
    val supports: ModelSupports,
    val tokenizer: String,
    val type: String
)

@Serializable
data class ModelLimits(
    @SerialName("max_context_window_tokens")
    val maxContextWindowTokens: Int,
    @SerialName("max_output_tokens")
    val maxOutputTokens: Int,
    @SerialName("max_prompt_tokens")
    val maxPromptTokens: Int
)

@Serializable
data class ModelSupports(
    val streaming: Boolean,
    @SerialName("tool_calls")
    val toolCalls: Boolean
)
