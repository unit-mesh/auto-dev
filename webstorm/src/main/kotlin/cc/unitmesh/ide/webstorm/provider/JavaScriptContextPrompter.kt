package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class JavaScriptContextPrompter : ContextPrompter() {
    override fun initContext(
        actionType: ChatBotActionType,
        text: String,
        file: PsiFile?,
        project: Project,
        offset: Int
    ) {
        super.initContext(actionType, text, file, project, offset)
    }

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
