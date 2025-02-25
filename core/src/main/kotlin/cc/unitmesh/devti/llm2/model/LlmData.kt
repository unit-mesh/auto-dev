package cc.unitmesh.devti.llm2.model

import kotlinx.serialization.Serializable

@Serializable
data class Auth(
    val type: String,
    val token: String
)

@Serializable
data class LlmConfig(
    val name: String,
    val description: String,
    val url: String,
    val auth: Auth,
    val requestFormat: String,
    val responseFormat: String
)
