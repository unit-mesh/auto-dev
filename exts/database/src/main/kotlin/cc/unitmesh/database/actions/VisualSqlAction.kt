package cc.unitmesh.database.actions

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class VisualSqlAction : IntentionAction {
    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = AutoDevBundle.message("migration.database.plsql")

    override fun getText(): String = AutoDevBundle.message("migration.database.plsql.visual")

    override fun isAvailable(project: Project, p1: Editor?, p2: PsiFile?): Boolean {
        return false
    }

    override fun invoke(project: Project, p1: Editor?, p2: PsiFile?) {
        TODO("Not yet implemented")
    }


}
