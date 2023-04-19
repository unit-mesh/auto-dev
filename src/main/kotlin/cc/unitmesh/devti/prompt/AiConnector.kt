package cc.unitmesh.devti.prompt

interface AiConnector {
    suspend fun prompt(prompt: String): String
}