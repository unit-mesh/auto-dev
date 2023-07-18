package cc.unitmesh.ide.pycharm.provider

import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class PythonContextPrompter : ContextPrompter {
    private var action: ChatBotActionType? = null
    private var selectedText: String = ""
    private var file: PsiFile? = null
    private var project: Project? = null

    private val lang: String = file?.language?.displayName ?: ""
    override fun initContext(actionType: ChatBotActionType, prefixText: String, file: PsiFile?, project: Project) {
        this.action = actionType
        this.selectedText = prefixText
        this.file = file
        this.project = project
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
