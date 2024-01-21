package cc.unitmesh.devti.intentions.action.base

import cc.unitmesh.devti.custom.document.CustomDocumentationConfig
import cc.unitmesh.devti.intentions.action.task.LivingDocumentationTask
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiUtilBase

abstract class BasedDocumentationIntention : AbstractChatIntention() {
    abstract val config: CustomDocumentationConfig

    override fun priority(): Int = 90

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        if (!isAvailable(project, editor, file)) return

        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText

        val documentation = LivingDocumentation.forLanguage(file.language) ?: return

        if (selectedText != null) {
            val rootFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return
            val findFile: PsiFile = PsiManager.getInstance(project).findFile(rootFile.virtualFile) ?: return

            // find all targets in selection
            documentation.findDocTargetsInSelection(findFile, selectionModel).map {
                writingDocument(editor, it)
            }
            return
        } else {
            val element = PsiUtilBase.getElementAtCaret(editor) ?: return
            val nearestDocumentationTarget = documentation.findNearestDocumentationTarget(element) ?: return
            writingDocument(editor, nearestDocumentationTarget)

            return
        }


    }

    open fun writingDocument(editor: Editor, element: PsiNameIdentifierOwner) {
        val task: Task.Backgroundable = LivingDocumentationTask(editor, element)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}