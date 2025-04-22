package cc.unitmesh.devti.language.processor.ui

import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.KeyStroke

class PendingApprovalPanel : JPanel() {
    private val approveButton = JButton("Approve")
    private val rejectButton = JButton("Reject")

    init {
        val layoutBuilder = panel {
            row {
                label(getShortcutLabel("⌘ + ↵", "Ctrl + ↵"))
                cell(approveButton)

                label(getShortcutLabel("⌘ + ⌦", "Ctrl + Del"))
                cell(rejectButton)
            }
        }


        layoutBuilder.border = JBUI.Borders.empty(0, 10)
        this.add(layoutBuilder)
    }

    private fun getShortcutLabel(shortcutForMac: String, shortcutForOthers: String): String {
        return if (System.getProperty("os.name").contains("Mac")) {
            shortcutForMac
        } else {
            shortcutForOthers
        }
    }

    fun setupKeyShortcuts(popup: JBPopup, approve: (Any) -> Unit, reject: (Any) -> Unit) {
        approveButton.addActionListener(approve)
        approveButton.registerKeyboardAction(
            {
                popup.closeOk(null)
                approveButton.doClick()
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW
        )

        rejectButton.addActionListener(reject)
        rejectButton.registerKeyboardAction(
            {
                popup.closeOk(null)
                rejectButton.doClick()
            }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW
        )
    }
}