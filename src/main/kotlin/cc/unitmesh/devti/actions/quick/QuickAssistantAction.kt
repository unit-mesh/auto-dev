package cc.unitmesh.devti.actions.quick

import cc.unitmesh.devti.actions.quick.QuickPrompt.Companion.QUICK_ASSISTANT_CANCEL_ACTION
import cc.unitmesh.devti.actions.quick.QuickPrompt.Companion.QUICK_ASSISTANT_SUBMIT_ACTION
import cc.unitmesh.devti.intentions.action.task.BaseCompletionTask
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import com.intellij.ide.KeyboardAwareFocusOwner
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.temporary.inlay.InlayPanel
import com.intellij.temporary.inlay.minimumWidth
import com.intellij.ui.scale.JBUIScale
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import javax.swing.AbstractAction
import javax.swing.JTextField
import javax.swing.KeyStroke

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

        val promptInlay: InlayPanel<QuickPrompt>? =
            InlayPanel.add(editor as EditorEx, offset, QuickPrompt())

        promptInlay?.let { doExecute(it, project, editor, element, sourceFile) }
    }

    private var isCanceled: Boolean = false

    private fun doExecute(
        inlay: InlayPanel<QuickPrompt>,
        project: Project,
        editor: EditorEx,
        element: PsiElement?,
        sourceFile: PsiFile,
    ) {
        val component = inlay.component

        val actionMap = component.actionMap
        val language = sourceFile.language

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
    }
}

class QuickPrompt : JTextField(), KeyboardAwareFocusOwner {
    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean = true

    init {
        this.minimumWidth = JBUIScale.scale(480)
        this.preferredSize = this.minimumSize

        inputMap.put(KeyStroke.getKeyStroke(VK_ESCAPE, 0), QUICK_ASSISTANT_CANCEL_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(VK_ENTER, 0), QUICK_ASSISTANT_SUBMIT_ACTION)
    }

    companion object {
        const val QUICK_ASSISTANT_CANCEL_ACTION = "quick.assistant.cancel"
        const val QUICK_ASSISTANT_SUBMIT_ACTION = "quick.assistant.submit"
    }
}
