package cc.unitmesh.devti.intentions.action.base

import cc.unitmesh.devti.custom.CustomDocumentationConfig
import cc.unitmesh.devti.intentions.action.task.LivingDocumentationTask
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
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
        if (selectedText != null) {
            val owners: List<PsiNameIdentifierOwner> = findSelectedElementToDocument(editor, project, selectionModel)
            for (identifierOwner in owners) {
                writingDocument(editor, identifierOwner)
            }

            return
        }

        val closestToCaretNamedElement: PsiNameIdentifierOwner? = getClosestToCaretNamedElement(editor)
        if (closestToCaretNamedElement != null) {
            writingDocument(editor, closestToCaretNamedElement)
        }
    }

    open fun writingDocument(editor: Editor, element: PsiNameIdentifierOwner) {
        val task: Task.Backgroundable = LivingDocumentationTask(editor, element)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun getClosestToCaretNamedElement(editor: Editor): PsiNameIdentifierOwner? {
        val element = PsiUtilBase.getElementAtCaret(editor) ?: return null
        return getClosestNamedElement(element)
    }

    private fun getClosestNamedElement(element: PsiElement): PsiNameIdentifierOwner? {
        val support = LivingDocumentation.forLanguage(element.language) ?: return null
        return support.findNearestDocumentationTarget(element)
    }

    private fun findSelectedElementToDocument(
        editor: Editor,
        project: Project,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        val rootFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return emptyList()
        val findFile: PsiFile = PsiManager.getInstance(project).findFile(rootFile.virtualFile) ?: return emptyList()
        val documentation = LivingDocumentation.forLanguage(findFile.language) ?: return emptyList()

        return documentation.findDocTargetsInSelection(findFile, selectionModel)
    }

}