package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.ContextPrompter

class JavaScriptContextPrompter : ContextPrompter() {

    override fun createDisplayPrompt(): String {
        return """$action for the code:
            ```${lang}
            $selectedText
            ```
            """.trimIndent()
    }

    override fun createRequestPrompt(): String {
        return """$action for the code:
            ```${lang}
            $selectedText
            ```
            """.trimIndent()
    }
}
