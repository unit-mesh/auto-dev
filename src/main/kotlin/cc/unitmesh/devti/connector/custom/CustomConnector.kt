package cc.unitmesh.devti.connector.custom

import cc.unitmesh.devti.connector.CodeCopilot

class CustomConnector(val url: String, val key: String) : CodeCopilot {
    private fun prompt(instruction: String, input: String): String {
        return ""
    }

    override fun codeCompleteFor(text: String, className: String): String {
        return ""
    }

    override fun autoComment(text: String): String {
        return ""
    }

}