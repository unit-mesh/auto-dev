package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class CodeCompletionIntention : AbstractChatIntention() {
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return (editor != null) && (file != null)
    }

    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.complete.name")

    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.complete.family.name")

    override fun getPrompt(project: Project, elementToExplain: PsiElement?): String {
        return "Code completion"
    }
}
