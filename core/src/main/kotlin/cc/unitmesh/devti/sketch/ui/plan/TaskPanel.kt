package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.plan.AgentPlanStep
import cc.unitmesh.devti.observer.plan.TaskStatus
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

/**
 * Task Panel UI Component responsible for rendering and handling interactions for a single task
 */
class TaskPanel(
    private val project: Project,
    private val task: AgentPlanStep,
    private val onStatusChange: () -> Unit
) : JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 2, 0)) {
    private val taskLabel: JLabel

    init {
        border = JBUI.Borders.empty(4, 16, 4, 0)
        taskLabel = createStyledTaskLabel()

        val statusIcon = createStatusIcon()
        add(statusIcon)

        if (task.status == TaskStatus.TODO) {
            add(createExecuteButton())
        }

        add(taskLabel)
        setupContextMenu()
    }

    private fun createStatusIcon(): JComponent {
        return when (task.status) {
            TaskStatus.COMPLETED -> JLabel(AllIcons.Actions.Checked)
            TaskStatus.FAILED -> JLabel(AllIcons.General.Error)
            TaskStatus.IN_PROGRESS -> JLabel(AllIcons.Toolwindows.ToolWindowBuild)
            TaskStatus.TODO -> JBCheckBox().apply {
                isSelected = task.completed
                addActionListener {
                    task.completed = isSelected
                    task.updateStatus(if (isSelected) TaskStatus.COMPLETED else TaskStatus.TODO)
                    updateTaskLabel()
                    onStatusChange()
                }
                isBorderPainted = false
                isContentAreaFilled = false
            }
        }
    }

    private fun createExecuteButton(): JButton {
        return JButton(AllIcons.Actions.Execute).apply {
            border = BorderFactory.createEmptyBorder()
            isOpaque = true
            preferredSize = Dimension(20, 20)
            toolTipText = "Execute"

            addActionListener {
                AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                    ui.sendInput(AutoDevBundle.message("sketch.plan.finish.task") + task.step)
                }
            }
        }
    }

    private fun createStyledTaskLabel(): JLabel {
        val labelText = getLabelTextByStatus()
        return JLabel(labelText).apply {
            border = JBUI.Borders.emptyLeft(5)
        }
    }

    private fun getLabelTextByStatus(): String {
        return when (task.status) {
            TaskStatus.COMPLETED -> "<html><strike>${task.step}</strike></html>"
            TaskStatus.FAILED -> "<html><span style='color:red'>${task.step}</span></html>"
            TaskStatus.IN_PROGRESS -> "<html><span style='color:blue;font-style:italic'>${task.step}</span></html>"
            TaskStatus.TODO -> task.step
        }
    }

    private fun updateTaskLabel() {
        taskLabel.text = getLabelTextByStatus()
    }

    private fun setupContextMenu() {
        val taskPopupMenu = JPopupMenu()

        val statusMenuItems = mapOf(
            "Mark as Completed [✓]" to TaskStatus.COMPLETED,
            "Mark as In Progress [*]" to TaskStatus.IN_PROGRESS,
            "Mark as Failed [!]" to TaskStatus.FAILED,
            "Mark as Todo [ ]" to TaskStatus.TODO
        )

        statusMenuItems.forEach { (label, status) ->
            val menuItem = JMenuItem(label)
            menuItem.addActionListener {
                task.updateStatus(status)
                updateTaskLabel()
                onStatusChange()
            }
            taskPopupMenu.add(menuItem)
        }

        taskLabel.componentPopupMenu = taskPopupMenu
    }
}