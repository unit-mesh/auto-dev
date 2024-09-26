package cc.unitmesh.devti.gui.quick

import com.intellij.ide.KeyboardAwareFocusOwner
import com.intellij.temporary.inlay.minimumWidth
import com.intellij.ui.scale.JBUIScale
import java.awt.event.KeyEvent
import javax.swing.JTextField
import javax.swing.KeyStroke

class QuickPromptField : JTextField(), KeyboardAwareFocusOwner {
    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean = true

    init {
        this.minimumWidth = JBUIScale.scale(480)
        this.preferredSize = this.minimumSize

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), QUICK_ASSISTANT_CANCEL_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), QUICK_ASSISTANT_SUBMIT_ACTION)
    }

    companion object {
        const val QUICK_ASSISTANT_CANCEL_ACTION = "quick.assistant.cancel"
        const val QUICK_ASSISTANT_SUBMIT_ACTION = "quick.assistant.submit"
    }
}