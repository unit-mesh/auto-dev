package cc.unitmesh.devti.gui.toolbar

import cc.unitmesh.devti.observer.agent.AgentStateService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JComponent

class CopyAllMessagesAction : AnAction("Copy All Messages", "Copy all messages", AllIcons.Actions.Copy),
    CustomComponentAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    override fun actionPerformed(e: AnActionEvent) {
        copyMessages(e.project)
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button: JButton = object : JButton() {
            init {
                putClientProperty("ActionToolbar.smallVariant", true)
                putClientProperty("customButtonInsets", JBInsets(1, 1, 1, 1).asUIResource())

                setOpaque(false)
                addActionListener {
                    copyMessages(ActionToolbar.getDataContextFor(this).getData(CommonDataKeys.PROJECT))
                }
            }
        }

        return Wrapper(button).also {
            it.setBorder(JBUI.Borders.empty(0, 10))
        }
    }

    private fun copyMessages(project: Project?) {
        val agentStateService = project?.getService(AgentStateService::class.java) ?: return
        val allText = agentStateService.getAllMessages().joinToString("\n") { it.content }
        val selection = StringSelection(allText)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, null)
    }
}
