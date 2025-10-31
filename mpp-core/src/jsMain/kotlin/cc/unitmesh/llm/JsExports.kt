@file:JsExport

package cc.unitmesh.llm

import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * JavaScript-friendly wrapper for KoogLLMService
 * This class is exported to JavaScript and provides a simpler API
 */
@JsExport
class JsKoogLLMService(config: JsModelConfig) {
    private val kotlinConfig = ModelConfig(
        provider = config.provider,
        modelName = config.modelName,
        apiKey = config.apiKey,
        temperature = config.temperature,
        maxTokens = config.maxTokens,
        baseUrl = config.baseUrl
    )
    
    private val service = KoogLLMService(kotlinConfig)
    
    /**
     * Stream a prompt and return a Flow of strings
     */
    @JsName("streamPrompt")
    fun streamPrompt(
        userPrompt: String,
        historyMessages: Array<JsMessage> = emptyArray()
    ): Flow<String> {
        val messages = historyMessages.map { it.toKotlinMessage() }
        return service.streamPrompt(userPrompt, EmptyFileSystem(), messages)
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
 */
@JsExport
data class JsModelConfig(
    val provider: LLMProviderType,
    val modelName: String,
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val baseUrl: String = ""
)

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
    fun getAvailableModels(provider: LLMProviderType): Array<String> {
        return ModelRegistry.getAvailableModels(provider).toTypedArray()
    }
    
    @JsName("getAllProviders")
    fun getAllProviders(): Array<LLMProviderType> {
        return LLMProviderType.entries.toTypedArray()
    }
}

