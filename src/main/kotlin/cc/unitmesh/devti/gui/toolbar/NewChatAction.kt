package cc.unitmesh.devti.gui.toolbar

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent

class NewChatAction : DumbAwareAction(), CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {

    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val label = JBLabel("New Chat")
        label.font = UIUtil.getToolTipFont()
        return label
    }
}
