package cc.unitmesh.devti.models

interface CodeCopilotProvider : LLMProvider {
    fun autoComment(text: String): String
    fun findBug(text: String): String
}