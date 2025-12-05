package cc.unitmesh.devti.actions.quick

import com.intellij.ide.KeyboardAwareFocusOwner
import java.awt.Dimension
import java.awt.event.KeyEvent
import javax.swing.JTextField
import javax.swing.KeyStroke

class QuickPromptField : JTextField(), KeyboardAwareFocusOwner {
    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean = true

    init {
        this.preferredSize = Dimension(480, minimumSize.height)

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), QUICK_ASSISTANT_CANCEL_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), QUICK_ASSISTANT_SUBMIT_ACTION)
    }

    companion object {
        const val QUICK_ASSISTANT_CANCEL_ACTION = "quick.assistant.cancel"
        const val QUICK_ASSISTANT_SUBMIT_ACTION = "quick.assistant.submit"
    }
}