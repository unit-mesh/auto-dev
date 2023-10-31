package cc.unitmesh.devti.gui.toolbar

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import javax.swing.JButton
import javax.swing.JComponent

class NewChatAction : DumbAwareAction(), CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {

    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val message = AutoDevBundle.message("autodev.chat.new")
        val button: JButton = object : JButton(message) {
            init {
                putClientProperty("ActionToolbar.smallVariant", true)
                putClientProperty("customButtonInsets", JBInsets(1, 1, 1, 1).asUIResource())
                setOpaque(false)
                addActionListener {
                    val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return@addActionListener
                    val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(
                        AutoDevToolWindowFactory.Util.id
                    )
                    val contentManager = toolWindowManager?.contentManager
                    val codingPanel =
                        contentManager?.component?.components?.filterIsInstance<ChatCodingPanel>()?.firstOrNull()

                    codingPanel?.clearChat()
                }
            }
        }

        return Wrapper(button).also {
            it.setBorder(JBUI.Borders.empty(0, 10))
        }
    }
}
