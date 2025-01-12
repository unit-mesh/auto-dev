package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.actions.chat.base.collectElementProblemAsSting
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class FixThisAction : RefactorThisAction() {
    init {
        presentationText("settings.autodev.rightClick.fixThis", templatePresentation)
    }

    override fun getActionType(): ChatActionType = ChatActionType.FIX_ISSUE

    override fun addAdditionPrompt(project: Project, editor: Editor, element: PsiElement): String {
        return collectElementProblemAsSting(element, project, editor)
    }
}
