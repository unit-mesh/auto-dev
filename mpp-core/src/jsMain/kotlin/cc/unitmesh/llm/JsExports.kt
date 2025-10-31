@file:JsExport

package cc.unitmesh.llm

import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise

/**
 * JavaScript-friendly wrapper for KoogLLMService
 * This class is exported to JavaScript and provides a simpler API
 */
@JsExport
class JsKoogLLMService(config: JsModelConfig) {
    private val kotlinConfig: ModelConfig
    private val service: KoogLLMService
    
    init {
        // Convert string provider to LLMProviderType
        val provider = when (config.providerName.uppercase()) {
            "OPENAI" -> LLMProviderType.OPENAI
            "ANTHROPIC" -> LLMProviderType.ANTHROPIC
            "GOOGLE" -> LLMProviderType.GOOGLE
            "DEEPSEEK" -> LLMProviderType.DEEPSEEK
            "OLLAMA" -> LLMProviderType.OLLAMA
            "OPENROUTER" -> LLMProviderType.OPENROUTER
            else -> throw IllegalArgumentException("Unknown provider: ${config.providerName}")
        }
        
        kotlinConfig = ModelConfig(
            provider = provider,
            modelName = config.modelName,
            apiKey = config.apiKey,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            baseUrl = config.baseUrl
        )
        
        service = KoogLLMService(kotlinConfig)
    }
    
    /**
     * Stream a prompt and return a Promise that resolves when streaming completes
     * @param userPrompt The user's prompt
     * @param historyMessages Previous conversation messages
     * @param onChunk Callback for each chunk of text received
     * @param onError Callback for errors
     * @param onComplete Callback when streaming completes
     */
    @JsName("streamPrompt")
    fun streamPrompt(
        userPrompt: String,
        historyMessages: Array<JsMessage> = emptyArray(),
        onChunk: (String) -> Unit,
        onError: ((Throwable) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ): Promise<Unit> {
        return Promise { resolve, reject ->
            GlobalScope.launch {
                try {
                    val messages = historyMessages.map { it.toKotlinMessage() }
                    service.streamPrompt(userPrompt, EmptyFileSystem(), messages)
                        .catch { error ->
                            onError?.invoke(error)
                            reject(error)
                        }
                        .collect { chunk ->
                            onChunk(chunk)
                        }
                    onComplete?.invoke()
                    resolve(Unit)
                } catch (e: Throwable) {
                    onError?.invoke(e)
                    reject(e)
                }
            }
        }
    }
    
    // Note: suspend functions cannot be exported to JS directly
    // They need to be called from Kotlin coroutines
    // JavaScript code should use streamPrompt() instead
    
    companion object {
        /**
         * Create a service with validation
         */
        @JsName("create")
        fun create(config: JsModelConfig): JsKoogLLMService {
            return JsKoogLLMService(config)
        }
    }
}

/**
 * JavaScript-friendly model configuration
 * Uses string for provider to avoid enum export issues
 */
@JsExport
data class JsModelConfig(
    val providerName: String,  // "OPENAI", "ANTHROPIC", "GOOGLE", "DEEPSEEK", "OLLAMA", "OPENROUTER"
    val modelName: String,
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val baseUrl: String = ""
) {
    fun toKotlinConfig(): ModelConfig {
        val provider = when (providerName.uppercase()) {
            "OPENAI" -> LLMProviderType.OPENAI
            "ANTHROPIC" -> LLMProviderType.ANTHROPIC
            "GOOGLE" -> LLMProviderType.GOOGLE
            "DEEPSEEK" -> LLMProviderType.DEEPSEEK
            "OLLAMA" -> LLMProviderType.OLLAMA
            "OPENROUTER" -> LLMProviderType.OPENROUTER
            else -> throw IllegalArgumentException("Unknown provider: $providerName")
        }
        
        return ModelConfig(
            provider = provider,
            modelName = modelName,
            apiKey = apiKey,
            temperature = temperature,
            maxTokens = maxTokens,
            baseUrl = baseUrl
        )
    }
}

/**
 * JavaScript-friendly message
 */
@JsExport
data class JsMessage(
    val role: String,  // "user", "assistant", or "system"
    val content: String
) {
    fun toKotlinMessage(): Message {
        val messageRole = when (role.lowercase()) {
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            "system" -> MessageRole.SYSTEM
            else -> MessageRole.USER
        }
        return Message(messageRole, content)
    }
}

/**
 * JavaScript-friendly result wrapper
 */
@JsExport
data class JsResult(
    val success: Boolean,
    val value: String,
    val error: String?
)

/**
 * Helper object to get available models for a provider
 */
@JsExport
object JsModelRegistry {
    @JsName("getAvailableModels")
    fun getAvailableModels(providerName: String): Array<String> {
        val provider = when (providerName.uppercase()) {
            "OPENAI" -> LLMProviderType.OPENAI
            "ANTHROPIC" -> LLMProviderType.ANTHROPIC
            "GOOGLE" -> LLMProviderType.GOOGLE
            "DEEPSEEK" -> LLMProviderType.DEEPSEEK
            "OLLAMA" -> LLMProviderType.OLLAMA
            "OPENROUTER" -> LLMProviderType.OPENROUTER
            else -> throw IllegalArgumentException("Unknown provider: $providerName")
        }
        return ModelRegistry.getAvailableModels(provider).toTypedArray()
    }
    
    @JsName("getAllProviders")
    fun getAllProviders(): Array<String> {
        return arrayOf("OPENAI", "ANTHROPIC", "GOOGLE", "DEEPSEEK", "OLLAMA", "OPENROUTER")
    }
}

