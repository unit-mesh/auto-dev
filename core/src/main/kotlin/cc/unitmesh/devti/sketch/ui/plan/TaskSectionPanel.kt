package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.TaskStatus
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.AnimatedIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.border.CompoundBorder

/**
 * Section Panel UI Component responsible for rendering and handling interactions for a plan section
 */
class TaskSectionPanel(
    private val project: Project,
    private val index: Int,
    private val planItem: AgentTaskEntry,
    private val onStatusChange: () -> Unit
) : JBPanel<JBPanel<*>>(BorderLayout()) {
    
    private val stepsPanel = JBPanel<JBPanel<*>>()
    private var isExpanded = true
    private var expandButton: JButton? = null
    private var statusLabel: JLabel? = null
    private var progressLabel: JLabel? = null
    
    init {
        layout = BorderLayout()
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(8, 16, 8, 16)
        )
        
        val headerPanel = createHeaderPanel()
        add(headerPanel, BorderLayout.NORTH)
        
        stepsPanel.layout = BoxLayout(stepsPanel, BoxLayout.Y_AXIS)
        stepsPanel.background = JBUI.CurrentTheme.ToolWindow.background()
        stepsPanel.border = JBUI.Borders.emptyLeft(24)
        
        // Add steps to the steps panel
        refreshStepsPanel()
        
        // Create a scroll pane for the steps
        val scrollPane = JBScrollPane(stepsPanel)
        scrollPane.border = BorderFactory.createEmptyBorder()
        scrollPane.isOpaque = false
        scrollPane.viewport.isOpaque = false
        
        add(scrollPane, BorderLayout.CENTER)
        
        // Initially show or hide based on state
        toggleStepsVisibility(isExpanded)
    }
    
    private fun createHeaderPanel(): JPanel {
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            background = JBUI.CurrentTheme.ToolWindow.background()
            border = JBUI.Borders.empty(2)
        }
        
        // Left side with expand button and title
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            isOpaque = false
        }
        
        // Expand/collapse button
        expandButton = JButton(if (isExpanded) "▼" else "▶").apply {
            preferredSize = Dimension(24, 24)
            margin = JBUI.insets(0)
            isBorderPainted = false
            isContentAreaFilled = false
            toolTipText = if (isExpanded) "Collapse section" else "Expand section"
            
            addActionListener {
                isExpanded = !isExpanded
                text = if (isExpanded) "▼" else "▶"
                toolTipText = if (isExpanded) "Collapse section" else "Expand section"
                toggleStepsVisibility(isExpanded)
            }
        }
        
        leftPanel.add(expandButton)
        
        // Status icon
        val statusIcon = when (planItem.status) {
            TaskStatus.COMPLETED -> JLabel(AutoDevIcons.Checked)
            TaskStatus.FAILED -> JLabel(AutoDevIcons.Error)
            TaskStatus.IN_PROGRESS -> JLabel(AutoDevIcons.InProgress)
            TaskStatus.TODO -> JLabel(AutoDevIcons.Build)
        }
        leftPanel.add(statusIcon)
        
        // Title with section number
        val titleLabel = JLabel("${index + 1}. ${planItem.title}").apply {
            font = font.deriveFont(Font.BOLD)
        }
        leftPanel.add(titleLabel)
        
        // Right side with status, progress and action buttons
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
            isOpaque = false
        }
        
        // Progress indicator
        val completedSteps = planItem.steps.count { it.completed }
        val totalSteps = planItem.steps.size
        val progressPercentage = if (totalSteps > 0) (completedSteps * 100) / totalSteps else 0
        
        progressLabel = JLabel("$progressPercentage% complete").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.PLAIN, 10f)
        }
        rightPanel.add(progressLabel)
        
        // Status label
        statusLabel = JLabel(getStatusText(planItem.status)).apply {
            foreground = getStatusColor(planItem.status)
            font = font.deriveFont(Font.BOLD, 11f)
            border = JBUI.Borders.empty(2, 5)
        }
        rightPanel.add(statusLabel)
        
        if (planItem.status == TaskStatus.TODO || planItem.status == TaskStatus.FAILED) {
            val executeButton = JButton("Execute").apply {
                addActionListener { executeSection() }
            }
            rightPanel.add(executeButton)
        }
        
        if (planItem.status == TaskStatus.FAILED) {
            val retryButton = JButton("Retry").apply {
                addActionListener { executeSection() }
            }
            rightPanel.add(retryButton)
        }
        
        headerPanel.add(leftPanel, BorderLayout.WEST)
        headerPanel.add(rightPanel, BorderLayout.EAST)
        
        return headerPanel
    }
    
    private fun toggleStepsVisibility(visible: Boolean) {
        stepsPanel.isVisible = visible
        revalidate()
        repaint()
        
        stepsPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) {
                UIUtil.invokeLaterIfNeeded {
                    parent.revalidate()
                }
            }
            
            override fun componentHidden(e: ComponentEvent) {
                UIUtil.invokeLaterIfNeeded {
                    parent.revalidate()
                }
            }
        })
    }
    
    private fun refreshStepsPanel() {
        stepsPanel.removeAll()
        
        planItem.steps.forEach { step ->
            val taskStepPanel = TaskStepPanel(project, step) {
                updateSectionStatus()
                updateProgressAndStatus()
                onStatusChange()
            }
            
            stepsPanel.add(taskStepPanel)
            stepsPanel.add(Box.createVerticalStrut(4))
        }
    }
    
    private fun executeSection() {
        planItem.updateStatus(TaskStatus.IN_PROGRESS)
        updateProgressAndStatus()
        
        AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
            val content = planItem.title + "\n" + planItem.steps.joinToString("\n") { "   - " + it.step }
            ui.sendInput(AutoDevBundle.message("sketch.plan.finish.task") + content)
        }
        
        refreshStepsPanel()
        revalidate()
        repaint()
    }
    
    fun updateSectionStatus() {
        planItem.updateCompletionStatus()
        updateProgressAndStatus()
    }
    
    private fun updateProgressAndStatus() {
        val completedSteps = planItem.steps.count { it.completed }
        val totalSteps = planItem.steps.size
        val progressPercentage = if (totalSteps > 0) (completedSteps * 100) / totalSteps else 0
        
        progressLabel?.text = "$progressPercentage% complete"
        
        statusLabel?.text = getStatusText(planItem.status)
        statusLabel?.foreground = getStatusColor(planItem.status)
        
        removeAll()
        add(createHeaderPanel(), BorderLayout.NORTH)
        add(stepsPanel, BorderLayout.CENTER)
        
        revalidate()
        repaint()
    }
    
    private fun getStatusText(status: TaskStatus): String {
        return when (status) {
            TaskStatus.COMPLETED -> "Completed"
            TaskStatus.FAILED -> "Failed"
            TaskStatus.IN_PROGRESS -> "In Progress"
            TaskStatus.TODO -> "To Do"
        }
    }
    
    private fun getStatusColor(status: TaskStatus): JBColor {
        return when (status) {
            TaskStatus.COMPLETED -> JBColor(0x59A869, 0x59A869) // Green
            TaskStatus.FAILED -> JBColor(0xD94F4F, 0xD94F4F) // Red
            TaskStatus.IN_PROGRESS -> JBColor(0x3592C4, 0x3592C4) // Blue
            TaskStatus.TODO -> JBColor(0x808080, 0x808080) // Gray
        }
    }
}
