package cc.unitmesh.devti.gui.chat

import com.intellij.lang.Language

class IntentionPromptFormatterProvider(val prompt: String, val selectedText: String, val lang: Language) : PromptFormatterProvider {
    override fun getUIPrompt(): String {
        return """
            $prompt
            for the code:
            ```${lang.displayName}
            $selectedText
            ```
            """.trimIndent()
    }

    override fun getRequestPrompt(): String {
        return """
            $prompt
            for the code:
            ```${lang.displayName}
            $selectedText
            ```
            """.trimIndent()
    }
}