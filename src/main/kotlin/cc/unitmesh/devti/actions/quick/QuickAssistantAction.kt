package cc.unitmesh.devti.actions.quick

import cc.unitmesh.devti.actions.quick.QuickPrompt.Companion.QUICK_ASSISTANT_CANCEL_ACTION
import cc.unitmesh.devti.actions.quick.QuickPrompt.Companion.QUICK_ASSISTANT_SUBMIT_ACTION
import com.intellij.ide.KeyboardAwareFocusOwner
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiFile
import com.intellij.temporary.inlay.InlayComponent
import com.intellij.temporary.inlay.minimumWidth
import com.intellij.ui.scale.JBUIScale
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_ENTER
import java.awt.event.KeyEvent.VK_RIGHT
import java.util.concurrent.atomic.AtomicReference
import javax.swing.AbstractAction
import javax.swing.JTextField
import javax.swing.KeyStroke

/**
 * A quick insight action is an action that can be triggered by a user,
 * user can input custom text to call with LLM.
 */
class QuickAssistantAction : AnAction() {
    private var currentPromptInlay: Inlay<InlayComponent<QuickPrompt>>? = null

    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val sourceFile = dataContext.getData(CommonDataKeys.PSI_FILE)
        val offset = editor.caretModel.offset
        val showAbove = InlayProperties().showAbove(true)
            .showWhenFolded(true);

        val promptInlay: Inlay<InlayComponent<QuickPrompt>>? =
            InlayComponent.add(editor as EditorEx, offset, showAbove, QuickPrompt())

        currentPromptInlay = promptInlay

        promptInlay?.let { doExecute(it, sourceFile) }
    }

    private fun doExecute(inlay: Inlay<InlayComponent<QuickPrompt>>, sourceFile: PsiFile?) {
        val component = inlay.renderer.component
        val project = inlay.editor.project ?: return

        val actionMap = component.actionMap
        actionMap.put(QUICK_ASSISTANT_SUBMIT_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val inlay = currentPromptInlay ?: return
                val text = component.getText()

                // TODO
                currentPromptInlay = null
            }
        })

        actionMap.put(QUICK_ASSISTANT_CANCEL_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                currentPromptInlay = null
            }
        })

        IdeFocusManager.getInstance(project).requestFocus(component, false)
    }
}

class QuickPrompt : JTextField(), KeyboardAwareFocusOwner {
    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean = true

    companion object {
        const val QUICK_ASSISTANT_CANCEL_ACTION = "quick.assistant.cancel"
        const val QUICK_ASSISTANT_SUBMIT_ACTION = "quick.assistant.submit"
    }

    init {
        this.minimumWidth = JBUIScale.scale(480)
        this.preferredSize = this.minimumSize

        inputMap.put(KeyStroke.getKeyStroke(VK_RIGHT, 0), QUICK_ASSISTANT_CANCEL_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(VK_ENTER, 0), QUICK_ASSISTANT_SUBMIT_ACTION)
    }
}
