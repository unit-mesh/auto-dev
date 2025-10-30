package cc.unitmesh.devins.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.*
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import java.time.Duration

/**
 * Service for interacting with LLMs using the Koog framework
 */
class KoogLLMService(private val config: ModelConfig) {
    
    /**
     * Send a prompt to the LLM and get TRUE streaming response
     * Uses Koog's executeStreaming API for real-time streaming
     */
    fun streamPrompt(userPrompt: String): Flow<String> = flow {
        try {
            val executor = createExecutor()
            val model = getModelForProvider()
            
            // Create prompt using Koog DSL
            val prompt = prompt(
                id = "chat",
                params = LLMParams(temperature = config.temperature.toDouble())
            ) {
                // Add system prompt
                system("You are a helpful AI assistant for code development and analysis.")
                // Add user message
                user(userPrompt)
            }
            
            // Use real streaming API
            executor.executeStreaming(prompt, model)
                .collect { frame ->
                    when (frame) {
                        is StreamFrame.Append -> {
                            // Emit text chunks as they arrive in real-time
                            emit(frame.text)
                        }
                        is StreamFrame.End -> {
                            // Stream ended successfully
                        }
                        is StreamFrame.ToolCall -> {
                            // Tool calls (可以后续扩展)
                        }
                    }
                }
        } catch (e: Exception) {
            emit("\n\n[Error: ${e.message}]")
            throw e
        }
    }

    /**
     * Send a prompt and get the complete response (non-streaming)
     */
    suspend fun sendPrompt(prompt: String): String {
        return try {
            // Create executor based on provider
            val executor = createExecutor()
            
            // Create agent with Koog's SimpleAPI
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = getModelForProvider(),
                systemPrompt = "You are a helpful AI assistant for code development and analysis."
            )
            
            // Execute and return result
            agent.run(prompt)
        } catch (e: Exception) {
            "[Error: ${e.message}]"
        }
    }

    /**
     * Get the appropriate LLModel based on provider and model name
     */
    private fun getModelForProvider(): LLModel {
        return when (config.provider) {
            LLMProviderType.OPENAI -> {
                // Use predefined models when available
                when (config.modelName) {
                    "gpt-4o" -> OpenAIModels.Chat.GPT4o
                    "gpt-4o-mini" -> OpenAIModels.CostOptimized.GPT4oMini
                    else -> LLModel(
                        provider = LLMProvider.OpenAI,
                        id = config.modelName,
                        capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
                        contextLength = 128000
                    )
                }
            }
            LLMProviderType.DEEPSEEK -> {
                when (config.modelName) {
                    "deepseek-chat" -> DeepSeekModels.DeepSeekChat
                    "deepseek-reasoner" -> DeepSeekModels.DeepSeekReasoner
                    else -> LLModel(
                        provider = LLMProvider.DeepSeek,
                        id = config.modelName,
                        capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
                        contextLength = 64000
                    )
                }
            }
            // For other providers, create generic models
            else -> {
                LLModel(
                    provider = getProviderForType(config.provider),
                    id = config.modelName,
                    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
                    contextLength = 128000
                )
            }
        }
    }

    /**
     * Map our provider type to Koog's LLMProvider
     */
    private fun getProviderForType(type: LLMProviderType): LLMProvider {
        return when (type) {
            LLMProviderType.OPENAI -> LLMProvider.OpenAI
            LLMProviderType.ANTHROPIC -> LLMProvider.Anthropic
            LLMProviderType.GOOGLE -> LLMProvider.Google
            LLMProviderType.DEEPSEEK -> LLMProvider.DeepSeek
            LLMProviderType.OLLAMA -> LLMProvider.Ollama
            LLMProviderType.OPENROUTER -> LLMProvider.OpenRouter
            LLMProviderType.BEDROCK -> LLMProvider.Bedrock
        }
    }

    /**
     * Create appropriate executor based on provider configuration
     */
    private fun createExecutor(): SingleLLMPromptExecutor {
        return when (config.provider) {
            LLMProviderType.OPENAI -> simpleOpenAIExecutor(config.apiKey)
            LLMProviderType.ANTHROPIC -> simpleAnthropicExecutor(config.apiKey)
            LLMProviderType.GOOGLE -> simpleGoogleAIExecutor(config.apiKey)
            LLMProviderType.DEEPSEEK -> {
                // DeepSeek doesn't have a simple function, create client manually
                SingleLLMPromptExecutor(DeepSeekLLMClient(config.apiKey))
            }
            LLMProviderType.OLLAMA -> simpleOllamaAIExecutor(
                baseUrl = config.baseUrl.ifEmpty { "http://localhost:11434" }
            )
            LLMProviderType.OPENROUTER -> simpleOpenRouterExecutor(config.apiKey)
            LLMProviderType.BEDROCK -> {
                // Bedrock requires AWS credentials in format: accessKeyId:secretAccessKey
                val credentials = config.apiKey.split(":")
                if (credentials.size != 2) {
                    throw IllegalArgumentException("Bedrock requires API key in format: accessKeyId:secretAccessKey")
                }
                simpleBedrockExecutor(
                    awsAccessKeyId = credentials[0],
                    awsSecretAccessKey = credentials[1]
                )
            }
        }
    }

    /**
     * Validate the configuration by making a simple test call
     */
    suspend fun validateConfig(): Result<String> {
        return try {
            val response = sendPrompt("Say 'OK' if you can hear me.")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * Create a service instance from configuration
         */
        fun create(config: ModelConfig): KoogLLMService {
            if (!config.isValid()) {
                throw IllegalArgumentException("Invalid model configuration: ${config.provider} requires ${if (config.provider == LLMProviderType.OLLAMA) "baseUrl and modelName" else "apiKey and modelName"}")
            }
            return KoogLLMService(config)
        }
    }
}
