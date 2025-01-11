package cc.unitmesh.devti.llms.custom

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

data class ChatFunctionCall(
    val name: String? = null,
    val arguments: JsonNode? = null,
)

data class ChatMessage(
    val role: String,
    @JsonInclude
    val content: String? = null,
    val name: String? = null,
    @JsonProperty("function_call")
    val functionCall: ChatFunctionCall? = null,
)

data class ChatCompletionChoice(
    val index: Int? = null,
    @JsonAlias("delta")
    val message: ChatMessage? = null,
    @JsonProperty("finish_reason")
    val finishReason: String? = null,
)

data class Usage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Long = 0,
    @JsonProperty("completion_tokens")
    val completionTokens: Long = 0,
    @JsonProperty("total_tokens")
    val totalTokens: Long = 0,
)

data class ChatCompletionResult(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long = 0,
    val model: String? = null,
    val choices: List<ChatCompletionChoice>,
    val usage: Usage? = null,
)