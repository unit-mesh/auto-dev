package cc.unitmesh.devti.ai

interface AiAction {
    fun prompt(prompt: String): String
}