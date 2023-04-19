package cc.unitmesh.devti.connector

interface AiConnector {
    suspend fun prompt(prompt: String): String
}