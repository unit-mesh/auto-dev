package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.toolwindow.chatWithSelection
import cc.unitmesh.devti.gui.chat.ChatActionType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class NewChatWithCodeIntention : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.chat.new.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.new.family.name")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        var selectedText = editor.selectionModel.selectedText
        val elementToExplain = getElementToAction(project, editor)

        if (selectedText == null) {
            if (elementToExplain == null) return

            selectElement(elementToExplain, editor)
            selectedText = editor.selectionModel.selectedText
        }

        if (selectedText == null) return

        val language = file.language.displayName

        chatWithSelection(project, language, selectedText, ChatActionType.CHAT)
    }

}