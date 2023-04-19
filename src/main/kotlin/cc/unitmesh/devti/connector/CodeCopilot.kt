package cc.unitmesh.devti.connector

interface CodeCopilot {
    fun codeCompleteFor(text: String, className: String): String

    fun autoComment(text: String): String
}