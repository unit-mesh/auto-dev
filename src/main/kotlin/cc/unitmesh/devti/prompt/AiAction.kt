package cc.unitmesh.devti.prompt

interface AiAction {
    suspend fun prompt(prompt: String): String
}