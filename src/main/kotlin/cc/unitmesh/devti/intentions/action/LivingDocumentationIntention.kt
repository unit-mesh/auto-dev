package cc.unitmesh.devti.intentions.action

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.action.task.LivingDocumentationTask
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiUtilBase

class LivingDocumentationIntention : IntentionAction {
    override fun startInWriteAction(): Boolean = false

    override fun getText(): String = AutoDevBundle.message("intentions.living.documentation.name")

    override fun getFamilyName(): String = AutoDevBundle.message("intentions.living.documentation.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false

        return LivingDocumentation.forLanguage(file.language) != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        if (!isAvailable(project, editor, file)) return

        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        if (selectedText != null) {
            val owners: List<PsiNameIdentifierOwner> = findSelectedElementToDocument(editor, project, selectionModel)
            for (identifierOwner in owners) {
                val task: Task.Backgroundable = LivingDocumentationTask(editor, project, identifierOwner)

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }
        }

    }

    private fun findSelectedElementToDocument(
        editor: Editor,
        project: Project,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        val rootFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return emptyList()
        val findFile: PsiFile = PsiManager.getInstance(project).findFile(rootFile.virtualFile) ?: return emptyList()
        val support = LivingDocumentation.forLanguage(findFile.language) ?: return emptyList()

        return support.findDocTargetsInSelection(findFile, selectionModel)
    }

}