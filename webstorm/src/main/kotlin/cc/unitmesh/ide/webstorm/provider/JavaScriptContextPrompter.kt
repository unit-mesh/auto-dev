package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.ContextPrompter

class JavaScriptContextPrompter : ContextPrompter() {

    override fun getUIPrompt(): String {
        return """$action for the code:
            ```${lang}
            $selectedText
            ```
            """.trimIndent()
    }

    override fun getRequestPrompt(): String {
        return """$action for the code:
            ```${lang}
            $selectedText
            ```
            """.trimIndent()
    }
}
