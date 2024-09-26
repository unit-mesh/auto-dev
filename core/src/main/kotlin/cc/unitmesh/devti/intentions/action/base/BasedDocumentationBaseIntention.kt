package cc.unitmesh.devti.intentions.action.base

import cc.unitmesh.devti.custom.document.CustomDocumentationConfig
import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.intentions.action.task.LivingDocumentationTask
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilBase

abstract class BasedDocumentationBaseIntention : ChatBaseIntention() {
    abstract val config: CustomDocumentationConfig

    override fun priority(): Int = 90

    override fun startInWriteAction(): Boolean = false

    private val logger = logger<BasedDocumentationBaseIntention>()

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
            val targetsInSelection = documentation.findDocTargetsInSelection(findFile, selectionModel)
            if (targetsInSelection.isNotEmpty()) {
                targetsInSelection.map {
                    writingDocument(editor, it, documentation)
                }
            } else {
                writingDocument(editor, findFile, documentation)
            }

            return
        }
        val element = PsiUtilBase.getElementAtCaret(editor) ?: return
        val nearestDocumentationTarget = documentation.findNearestDocumentationTarget(element)
        if (nearestDocumentationTarget != null) {
            writingDocument(editor, nearestDocumentationTarget, documentation)
            return
        }

        logger.warn("No selected text and no nearest documentation target found")
    }

    open fun writingDocument(editor: Editor, element: PsiElement, documentation: LivingDocumentation) {
        val task = LivingDocumentationTask(editor, element, LivingDocumentationType.COMMENT, documentation)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}