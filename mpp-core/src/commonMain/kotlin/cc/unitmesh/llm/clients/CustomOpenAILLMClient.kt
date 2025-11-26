package cc.unitmesh.llm.clients

import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.base.AbstractOpenAILLMClient
import ai.koog.prompt.executor.clients.openai.base.OpenAIBaseSettings
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIMessage
import ai.koog.prompt.executor.clients.openai.base.models.OpenAITool
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIToolChoice
import ai.koog.prompt.executor.model.LLMChoice
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrameFlowBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Configuration settings for custom OpenAI-compatible APIs (like GLM, custom endpoints, etc.)
 *
 * @property baseUrl The base URL of the custom OpenAI-compatible API (without trailing slash)
 * @property chatCompletionsPath The path for chat completions endpoint (default: "chat/completions", NO leading slash)
 * @property timeoutConfig Configuration for connection timeouts
 */
class CustomOpenAIClientSettings(
    baseUrl: String,
    chatCompletionsPath: String = "chat/completions",
    timeoutConfig: ConnectionTimeoutConfig = ConnectionTimeoutConfig()
) : OpenAIBaseSettings(baseUrl, chatCompletionsPath, timeoutConfig)

/**
 * Request model for custom OpenAI-compatible chat completion
 */
@Serializable
data class CustomOpenAIChatCompletionRequest(
    val messages: List<OpenAIMessage>,
    val model: String,
    val frequencyPenalty: Double? = null,
    val logprobs: Boolean? = null,
    val maxTokens: Int? = null,
    val presencePenalty: Double? = null,
    val responseFormat: ai.koog.prompt.executor.clients.openai.base.models.OpenAIResponseFormat? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false,
    val temperature: Double? = null,
    val toolChoice: OpenAIToolChoice? = null,
    val tools: List<OpenAITool>? = null,
    val topLogprobs: Int? = null,
    val topP: Double? = null
)

/**
 * Response model for custom OpenAI-compatible chat completion
 */
@Serializable
data class CustomOpenAIChatCompletionResponse(
    override val id: String,
    val `object`: String? = null, // Optional: Some OpenAI-compatible APIs (like GLM) may not include this
    override val created: Long,
    override val model: String,
    val choices: List<Choice>,
    val usage: ai.koog.prompt.executor.clients.openai.base.models.OpenAIUsage? = null
) : ai.koog.prompt.executor.clients.openai.base.models.OpenAIBaseLLMResponse {
    @Serializable
    data class Choice(
        val index: Int,
        val message: OpenAIMessage.Assistant,
        val finishReason: String? = null
    )
}

/**
 * Streaming response model for custom OpenAI-compatible chat completion
 */
@Serializable
data class CustomOpenAIChatCompletionStreamResponse(
    override val id: String,
    val `object`: String? = null, // Optional: Some OpenAI-compatible APIs (like GLM) may not include this
    override val created: Long,
    override val model: String,
    val choices: List<StreamChoice>,
    val usage: ai.koog.prompt.executor.clients.openai.base.models.OpenAIUsage? = null
) : ai.koog.prompt.executor.clients.openai.base.models.OpenAIBaseLLMStreamResponse {
    @Serializable
    data class StreamChoice(
        val index: Int,
        val delta: Delta,
        val finishReason: String? = null
    )

    @Serializable
    data class Delta(
        val role: String? = null,
        val content: String? = null,
        val toolCalls: List<ai.koog.prompt.executor.clients.openai.base.models.OpenAIToolCall>? = null
    )
}

/**
 * Implementation of [LLMClient] for custom OpenAI-compatible APIs.
 * This client can be used with any OpenAI-compatible API like GLM, custom endpoints, etc.
 *
 * **IMPORTANT URL Construction in Ktor**:
 * - When using `defaultRequest { url(baseUrl) }` and then `post(path)`:
 *   - If `path` starts with `/`, Ktor treats it as absolute and DISCARDS the baseUrl path
 *   - If `path` does NOT start with `/`, Ktor appends it to baseUrl
 * - Example:
 *   - baseUrl = "https://api.example.com/v1", path = "/chat/completions"
 *   - Result: https://api.example.com/chat/completions (WRONG - lost /v1)
 *   - baseUrl = "https://api.example.com/v1", path = "chat/completions"
 *   - Result: https://api.example.com/v1/chat/completions (CORRECT)
 *
 * @param apiKey The API key for the custom API
 * @param baseUrl The base URL of the custom API (e.g., "https://open.bigmodel.cn/api/paas/v4", without trailing slash)
 * @param chatCompletionsPath The path for chat completions (default: "chat/completions", NO leading slash)
 * @param timeoutConfig Configuration for connection timeouts
 * @param baseClient Optional custom HTTP client
 * @param clock Clock instance for tracking timestamps
 */
class CustomOpenAILLMClient(
    apiKey: String,
    baseUrl: String,
    chatCompletionsPath: String = "chat/completions",
    timeoutConfig: ConnectionTimeoutConfig = ConnectionTimeoutConfig(),
    baseClient: HttpClient = HttpClient(),
    clock: Clock = Clock.System
) : AbstractOpenAILLMClient<CustomOpenAIChatCompletionResponse, CustomOpenAIChatCompletionStreamResponse>(
    apiKey,
    CustomOpenAIClientSettings(baseUrl, chatCompletionsPath, timeoutConfig),
    baseClient,
    clock,
    staticLogger
) {

    private companion object {
        private val staticLogger = KotlinLogging.logger { }

        init {
            // Register custom OpenAI JSON schema generators for structured output
            // Use OpenAI provider since custom providers are OpenAI-compatible
            registerOpenAIJsonSchemaGenerators(LLMProvider.OpenAI)
        }
    }

    override fun llmProvider(): LLMProvider = LLMProvider.OpenAI // OpenAI-compatible provider

    override fun serializeProviderChatRequest(
        messages: List<OpenAIMessage>,
        model: LLModel,
        tools: List<OpenAITool>?,
        toolChoice: OpenAIToolChoice?,
        params: LLMParams,
        stream: Boolean
    ): String {
        val responseFormat = createResponseFormat(params.schema, model)

        val request = CustomOpenAIChatCompletionRequest(
            messages = messages,
            model = model.id,
            frequencyPenalty = null,
            logprobs = null,
            maxTokens = null,
            presencePenalty = null,
            responseFormat = responseFormat,
            stop = null,
            stream = stream,
            temperature = params.temperature,
            toolChoice = toolChoice,
            tools = tools,
            topLogprobs = null,
            topP = null
        )

        return json.encodeToString(request)
    }

    override fun processProviderChatResponse(response: CustomOpenAIChatCompletionResponse): List<LLMChoice> {
        require(response.choices.isNotEmpty()) { "Empty choices in response" }
        return response.choices.map {
            it.message.toMessageResponses(
                it.finishReason,
                createMetaInfo(response.usage),
            )
        }
    }

    override fun decodeStreamingResponse(data: String): CustomOpenAIChatCompletionStreamResponse =
        json.decodeFromString(data)

    override fun decodeResponse(data: String): CustomOpenAIChatCompletionResponse =
        json.decodeFromString(data)

    override suspend fun StreamFrameFlowBuilder.processStreamingChunk(chunk: CustomOpenAIChatCompletionStreamResponse) {
        chunk.choices.firstOrNull()?.let { choice ->
            choice.delta.content?.let { emitAppend(it) }
            choice.delta.toolCalls?.forEach { toolCall ->
                upsertToolCall(0, toolCall.id, toolCall.function.name, toolCall.function.arguments)
            }
            choice.finishReason?.let { emitEnd(it, createMetaInfo(chunk.usage)) }
        }
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult {
        logger.warn { "Moderation is not supported by custom OpenAI-compatible APIs" }
        throw UnsupportedOperationException("Moderation is not supported by custom OpenAI-compatible APIs.")
    }
}

