package cc.unitmesh.agent.config

import kotlinx.serialization.Serializable

/**
 * Chat configuration settings
 */
@Serializable
data class ChatConfig(
    val temperature: Double = 0.7,
    val systemPrompt: String = "",
    val maxTokens: Int = 128000
)