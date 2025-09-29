package cc.unitmesh.devti.llms.custom

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.Serializable

data class ChatFunctionCall(
    @JsonProperty("name")
    val name: String? = null,
    @JsonProperty("arguments")
    val arguments: JsonNode? = null,
)

data class ChatMessage(
    @JsonProperty("role")
    val role: String? = null,
    @JsonProperty("content")
    @JsonInclude
    val content: String? = null,
    @JsonProperty("name")
    val name: String? = null,
    @JsonProperty("function_call")
    val functionCall: ChatFunctionCall? = null,
)

data class ChatCompletionChoice(
    @JsonProperty("index")
    val index: Int? = null,
    @JsonProperty("message")
    @JsonAlias("delta")
    val message: ChatMessage? = null,
    @JsonProperty("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class Usage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Long = 0,
    @JsonProperty("completion_tokens")
    val completionTokens: Long = 0,
    @JsonProperty("total_tokens")
    val totalTokens: Long = 0,
)

data class ChatCompletionResult(
    @JsonProperty("id")
    val id: String? = null,
    @JsonProperty("object")
    val `object`: String? = null,
    @JsonProperty("created")
    val created: Long = 0,
    @JsonProperty("model")
    val model: String? = null,
    @JsonProperty("choices")
    val choices: List<ChatCompletionChoice>,
    @JsonProperty("usage")
    val usage: Usage? = null,
)