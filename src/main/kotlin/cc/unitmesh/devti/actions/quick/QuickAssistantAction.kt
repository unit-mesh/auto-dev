package cc.unitmesh.devti.actions.quick

import cc.unitmesh.devti.gui.quick.QuickPromptField
import cc.unitmesh.devti.gui.quick.QuickPromptField.Companion.QUICK_ASSISTANT_CANCEL_ACTION
import cc.unitmesh.devti.gui.quick.QuickPromptField.Companion.QUICK_ASSISTANT_SUBMIT_ACTION
import cc.unitmesh.devti.intentions.action.task.BaseCompletionTask
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.temporary.inlay.InlayPanel
import java.awt.event.ActionEvent
import javax.swing.AbstractAction

/**
 * A quick insight action is an action that can be triggered by a user,
 * user can input custom text to call with LLM.
 */
class QuickAssistantAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val offset = editor.caretModel.offset
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
        val element = e.getData(CommonDataKeys.PSI_ELEMENT)
        val sourceFile = dataContext.getData(CommonDataKeys.PSI_FILE) ?: return

        // get prompts from context
        useInlayMode(editor, offset, project, element, sourceFile)
    }

    private fun useInlayMode(
        editor: Editor,
        offset: Int,
        project: Project,
        element: PsiElement?,
        sourceFile: PsiFile
    ) {
        val promptInlay: InlayPanel<QuickPromptField>? =
            InlayPanel.add(editor as EditorEx, offset, QuickPromptField())

        promptInlay?.let { doExecute(it, project, editor, element, sourceFile) }
    }

    private fun doExecute(
        inlay: InlayPanel<QuickPromptField>,
        project: Project,
        editor: EditorEx,
        element: PsiElement?,
        sourceFile: PsiFile,
    ) {
        val component = inlay.component

        val actionMap = component.actionMap

        actionMap.put(QUICK_ASSISTANT_SUBMIT_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val text =
                    """Generate a concise code snippet with no extra text, description, or comments. 
                        |The code should achieve the following task: ${component.getText()}"""
                        .trimMargin()

                val offset = editor.caretModel.offset

                val request = runReadAction {
                    CodeCompletionRequest.create(editor, offset, element, null, text)
                } ?: return

                val task = object : BaseCompletionTask(request) {
                    override fun keepHistory(): Boolean = false
                    override fun promptText(): String = text
                }

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

                Disposer.dispose(inlay.inlay!!)
            }
        })

        actionMap.put(QUICK_ASSISTANT_CANCEL_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                Disposer.dispose(inlay.inlay!!)
            }
        })

        IdeFocusManager.getInstance(project).requestFocus(component, false)
        EditorUtil.disposeWithEditor(editor, inlay.inlay!!)
    }
}

