package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.gui.planner.AutoDevPlannerToolWindow
import cc.unitmesh.devti.gui.AutoDevPlannerToolWindowFactory
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
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
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class PlanToolbarFactory(private val project: Project) {
    fun createToolbar(component: JComponent): JComponent {
        val actionGroup = DefaultActionGroup(createToolbarActions())
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("PlanSketch", actionGroup, true).apply {
                this.targetComponent = component
            }

        val titleLabel = JLabel("Plan").apply {
            border = JBUI.Borders.empty(0, 10)
        }

        val toolbarPanel = JPanel(BorderLayout()).apply {
            add(titleLabel, BorderLayout.WEST)
            add(toolbar.component, BorderLayout.EAST)
        }

        val toolbarWrapper = Wrapper(JBUI.Panels.simplePanel(toolbarPanel)).also {
            it.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0)
        }

        return toolbarWrapper
    }

    private fun createToolbarActions(): List<AnAction> {
        val pinAction = object : AnAction("Pin", "Show in popup window", AllIcons.Toolbar.Pin) {
            override fun displayTextInToolbar(): Boolean = true

            override fun actionPerformed(e: AnActionEvent) {
                val toolWindowManager = ToolWindowManager.Companion.getInstance(project)
                val toolWindow =
                    toolWindowManager.getToolWindow(AutoDevPlannerToolWindowFactory.Companion.PlANNER_ID)
                        ?: return

                val codingPanel =
                    toolWindow.contentManager.component.components?.filterIsInstance<AutoDevPlannerToolWindow>()
                        ?.firstOrNull()

                toolWindow.activate {
                    val agentStateService = project.getService(AgentStateService::class.java)
                    val currentPlan = agentStateService.getPlan()
                    val planString = MarkdownPlanParser.formatPlanToMarkdown(currentPlan)

                    codingPanel?.switchToPlanView(planString)
                }
            }
        }

        val copyAction = object : AnAction("Copy", "Copy plan to clipboard", AutoDevIcons.COPY) {
            override fun actionPerformed(e: AnActionEvent) {
                val agentStateService = project.getService(AgentStateService::class.java)
                val currentPlan = agentStateService.getPlan()
                val planString = MarkdownPlanParser.formatPlanToMarkdown(currentPlan)

                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(planString)
                clipboard.setContents(selection, null)
            }
        }

        return listOf(copyAction, pinAction)
    }
}