package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatActionType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * This class represents an intention to refactor code in a chat environment.
 * It extends the AbstractChatIntention class.
 */
class RefactorThisIntention : AbstractChatIntention() {
    override fun getActionType() = ChatActionType.REFACTOR

    override val prompt = "Refactor following code:"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return (editor != null) && (file != null)
    }

    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.refactor.name")

    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.refactor.family.name")
}
