package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.actions.chat.base.ChatCheckForUpdateAction
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class GenerateApiTestAction : ChatCheckForUpdateAction() {
    init {
        presentationText("settings.autodev.rightClick.genApiTest", templatePresentation)
    }

    override fun getActionType(): ChatActionType = ChatActionType.GENERATE_TEST_DATA

    override fun addAdditionPrompt(project: Project, editor: Editor, element: PsiElement): String {
        this.getActionType().context.code = element.text
        this.getActionType().context.language = element.language.displayName

        return ""
    }
}
