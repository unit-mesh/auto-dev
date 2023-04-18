package cc.unitmesh.devti.prompt

interface AiExecutor {
    suspend fun prompt(prompt: String): String
}