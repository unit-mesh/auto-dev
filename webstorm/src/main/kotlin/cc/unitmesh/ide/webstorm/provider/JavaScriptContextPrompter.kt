package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.ContextPrompter

class JavaScriptContextPrompter : ContextPrompter() {
    override fun displayPrompt(): String {
        return """$action for the code:
            ```${lang}
            $selectedText
            ```
            """.trimIndent()
    }

    override fun requestPrompt(): String {
        return """$action for the code:
            ```${lang}
            $selectedText
            ```
            """.trimIndent()
    }
}
