package cc.unitmesh.devins.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.list
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.*
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow

class KoogLLMService(private val config: ModelConfig) {
    fun streamPrompt(userPrompt: String): Flow<String> = flow {
        val executor = createExecutor()
        val model = getModelForProvider()

        val prompt = prompt(
            id = "chat",
            params = LLMParams(temperature = config.temperature, toolChoice = LLMParams.ToolChoice.None)
        ) {
            user(userPrompt)
        }

        executor.executeStreaming(prompt, model)
            .cancellable()
            .collect { frame ->
                when (frame) {
                    is StreamFrame.Append -> {
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
    }

    suspend fun sendPrompt(prompt: String): String {
        return try {
            val executor = createExecutor()
            
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = getModelForProvider()
            )
            
            agent.run(prompt)
        } catch (e: Exception) {
            "[Error: ${e.message}]"
        }
    }

    private fun getModelForProvider(): LLModel {
        return when (config.provider) {
            LLMProviderType.OPENAI -> {
                OpenAIModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.OpenAI, 128000)
            }
            LLMProviderType.DEEPSEEK -> {
                DeepSeekModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.DeepSeek, 64000)
            }
            LLMProviderType.ANTHROPIC -> {
                AnthropicModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.Anthropic, 200000)
            }
            LLMProviderType.GOOGLE -> {
                GoogleModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.Google, 128000)
            }
            LLMProviderType.OPENROUTER -> {
                OpenRouterModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.OpenRouter, 128000)
            }
            LLMProviderType.OLLAMA -> {
                LLModel(
                    provider = LLMProvider.Ollama,
                    id = config.modelName,
                    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
                    contextLength = 128000
                )
            }
            LLMProviderType.BEDROCK -> {
                createDefaultModel(LLMProvider.Bedrock, 128000)
            }
        }
    }

    private fun createDefaultModel(provider: LLMProvider, contextLength: Long): LLModel {
        return LLModel(
            provider = provider,
            id = config.modelName,
            capabilities = listOf(),
            contextLength = contextLength,
        )
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
