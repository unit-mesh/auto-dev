package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.TaskStatus
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Section Panel UI Component responsible for rendering and handling interactions for a plan section
 */
class SectionPanel(
    private val project: Project,
    private val index: Int,
    private val planItem: AgentTaskEntry,
    private val onStatusChange: () -> Unit
) : JBPanel<JBPanel<*>>(BorderLayout()) {

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = JBUI.CurrentTheme.ToolWindow.background()
        val titlePanel = createSectionTitlePanel()
        add(titlePanel)

        planItem.steps.forEach { task ->
            add(TaskPanel(project, task) {
                updateSectionStatus()
                onStatusChange()
            })
        }

        revalidate()
    }

    private fun createSectionTitlePanel(): JPanel {
        val titlePanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 2, 0)).apply {
            border = JBUI.Borders.empty(2)
            background = JBUI.CurrentTheme.ToolWindow.background()
        }

        if (planItem.status == TaskStatus.TODO && !planItem.completed) {
            titlePanel.add(createExecuteSectionButton())
        }

        val statusIndicator = when (planItem.status) {
            TaskStatus.COMPLETED -> "✓"
            TaskStatus.FAILED -> "!"
            TaskStatus.IN_PROGRESS -> "*"
            TaskStatus.TODO -> ""
        }

        val titleText = if (statusIndicator.isNotEmpty()) {
            "<html><b>${index + 1}. ${planItem.title} [$statusIndicator]</b></html>"
        } else {
            "<html><b>${index + 1}. ${planItem.title}</b></html>"
        }

        val sectionLabel = JLabel(titleText)
        sectionLabel.border = JBUI.Borders.emptyLeft(2)

        titlePanel.add(sectionLabel)
        return titlePanel
    }

    private fun createExecuteSectionButton(): JButton {
        return JButton(AllIcons.Actions.Execute).apply {
            border = BorderFactory.createEmptyBorder()

            preferredSize = Dimension(20, 20)
            toolTipText = "Execute Task"
            background = JBUI.CurrentTheme.ToolWindow.background()

            addActionListener {
                AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                    val allSteps = planItem.steps.joinToString("\n") { it.step }
                    ui.sendInput(AutoDevBundle.message("sketch.plan.finish.task") + allSteps)
                }
            }
        }
    }

    fun updateSectionStatus() {
        planItem.updateCompletionStatus()
    }
}