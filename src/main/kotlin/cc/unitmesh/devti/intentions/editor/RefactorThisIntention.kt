package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * This class represents an intention to refactor code in a chat environment.
 * It extends the AbstractChatIntention class.
 */
class RefactorThisIntention : AbstractChatIntention() {
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return (editor != null) && (file != null)
    }

    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.refactor.name")

    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.refactor.family.name")

    /**
     * Retrieves the prompt for performing a code refactoring.
     *
     * @param project the current project
     * @param elementToExplain the specific element to explain in the prompt (optional)
     * @return the prompt message for the code refactoring
     */
    override fun getPrompt(project: Project, elementToExplain: PsiElement?): String {
        return "Refactor following code:"
    }
}
