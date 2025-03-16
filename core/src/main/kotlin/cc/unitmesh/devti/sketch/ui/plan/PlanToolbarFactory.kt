package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.gui.AutoDevPlanerToolWindowFactory
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Toolbar factory for creating the plan sketch toolbar
 */
class PlanToolbarFactory(private val project: Project) {
    fun createToolbar(): JComponent {
        val actionGroup = DefaultActionGroup(createToolbarActions())
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("PlanSketch", actionGroup, true)

        val titleLabel = JLabel("Thought Plan").apply {
            border = JBUI.Borders.empty(0, 10)
        }

        val toolbarPanel = JPanel(BorderLayout()).apply {
            add(titleLabel, BorderLayout.WEST)
            add(toolbar.component, BorderLayout.EAST)
        }

        val toolbarWrapper = Wrapper(JBUI.Panels.simplePanel(toolbarPanel)).also {
            it.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1, 1, 1, 1)
        }

        return toolbarWrapper
    }

    private fun createToolbarActions(): List<AnAction> {
        val pinAction = object : AnAction("Pin", "Show in popup window", AllIcons.Toolbar.Pin) {
            override fun displayTextInToolbar(): Boolean = true

            override fun actionPerformed(e: AnActionEvent) {
                val toolWindow =
                    ToolWindowManager.Companion.getInstance(project).getToolWindow(AutoDevPlanerToolWindowFactory.Companion.PlANNER_ID)
                        ?: return

                toolWindow.activate {
                    // todo
                }
            }
        }

        return listOf(pinAction)
    }
}