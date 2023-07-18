package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class IntentionContextPrompter(val prompt: String, val selectedText: String, val lang: Language) :
    ContextPrompter {


    override fun initContext(actionType: ChatBotActionType, prefixText: String, file: PsiFile?, project: Project) {

    }

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