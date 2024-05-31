package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class GenerateApiTestAction : ChatBaseAction() {
    init {
        val presentation = getTemplatePresentation()
        presentation.text = AutoDevBundle.message("settings.autodev.rightClick.genApiTest")
    }

    override fun getActionType(): ChatActionType = ChatActionType.GENERATE_TEST_DATA

    override fun addAdditionPrompt(project: Project, editor: Editor, element: PsiElement): String {
        this.getActionType().context.code = element.text
        this.getActionType().context.language = element.language.displayName

        return ""
    }
}
