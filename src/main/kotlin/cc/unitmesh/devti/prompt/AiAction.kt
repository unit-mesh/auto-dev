package cc.unitmesh.devti.prompt

interface AiAction {
    fun prompt(prompt: String): String
}