package cc.unitmesh.devti.actions.quick

import com.intellij.ide.KeyboardAwareFocusOwner
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.ui.scale.JBUIScale
import org.jetbrains.plugins.notebooks.visualization.r.inlays.InlayComponent
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_ENTER
import java.awt.event.KeyEvent.VK_RIGHT
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
        val sourceFile = dataContext.getData(CommonDataKeys.PSI_FILE)
        val offset = editor.caretModel.offset
        val showAbove = InlayProperties().showAbove(true)
    }
}

class QuickPrompt : JTextField(), KeyboardAwareFocusOwner {
    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean {
        return true
    }

    init {
        this.minimumWidth = JBUIScale.scale(480)
        this.preferredSize = this.minimumSize

        val inputMap = inputMap
        inputMap.put(KeyStroke.getKeyStroke(VK_RIGHT, 0), "cancel")
        inputMap.put(KeyStroke.getKeyStroke(VK_ENTER, 0), "enter")
    }
}
