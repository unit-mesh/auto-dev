package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.TaskStatus
import cc.unitmesh.devti.sketch.ui.AutoDevColors
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
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
    private val scrollPane: JBScrollPane
    private val MAX_TITLE_LENGTH = 50
    
    init {
        layout = BorderLayout()
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(4, 16)
        )
        
        val headerPanel = createHeaderPanel()
        add(headerPanel, BorderLayout.NORTH)
        
        stepsPanel.layout = BoxLayout(stepsPanel, BoxLayout.Y_AXIS)
        stepsPanel.background = JBUI.CurrentTheme.ToolWindow.background()
        stepsPanel.border = JBUI.Borders.emptyLeft(4)
        
        refreshStepsPanel()
        
        scrollPane = JBScrollPane(stepsPanel).apply {
            border = BorderFactory.createEmptyBorder()
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        
        add(scrollPane, BorderLayout.CENTER)
        toggleStepsVisibility(isExpanded)
    }
    
    private fun createHeaderPanel(): JPanel {
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            background = JBUI.CurrentTheme.ToolWindow.background()
            border = JBUI.Borders.empty(2)
        }
        
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            isOpaque = false
        }
        
        expandButton = JButton(if (isExpanded) "▼" else "▶").apply {
            preferredSize = Dimension(20, 20)
            margin = JBUI.emptyInsets()
            isBorderPainted = false
            isContentAreaFilled = false
            toolTipText = if (isExpanded) "Collapse section" else "Expand section"
            
            addActionListener { e ->
                isExpanded = !isExpanded
                text = if (isExpanded) "▼" else "▶"
                toolTipText = if (isExpanded) "Collapse section" else "Expand section"
                toggleStepsVisibility(isExpanded)
                e.source = this  // Ensure event propagation
            }
        }
        
        leftPanel.add(expandButton)
        
        val statusIcon = when (planItem.status) {
            TaskStatus.COMPLETED -> JLabel(AutoDevIcons.Checked)
            TaskStatus.FAILED -> JLabel(AutoDevIcons.Error)
            TaskStatus.IN_PROGRESS -> JLabel(AutoDevIcons.InProgress)
            TaskStatus.TODO -> JLabel(AutoDevIcons.Build)
        }
        leftPanel.add(statusIcon)
        
        val fullTitle = "${index + 1}. ${planItem.title}"
        val displayTitle = if (planItem.title.length > MAX_TITLE_LENGTH) {
            "${index + 1}. ${planItem.title.take(MAX_TITLE_LENGTH)}..."
        } else {
            fullTitle
        }
        
        val titleLabel = JLabel(displayTitle).apply {
            font = font.deriveFont(Font.BOLD)
            toolTipText = fullTitle
        }
        leftPanel.add(titleLabel)
        
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
            isOpaque = false
        }
        
        statusLabel = JLabel(getStatusText(planItem.status)).apply {
            foreground = getStatusColor(planItem.status)
            font = font.deriveFont(Font.BOLD, 11f)
            border = JBUI.Borders.empty(2)
            preferredSize = Dimension(80, 16)
        }

        rightPanel.add(statusLabel)
        
        if (planItem.status == TaskStatus.TODO || planItem.status == TaskStatus.FAILED) {
            val executeButton = JButton(AutoDevIcons.Run).apply {
                margin = JBUI.emptyInsets()
                isBorderPainted = false
                isContentAreaFilled = false
                preferredSize = Dimension(20, 20)
                addActionListener { executeSection() }
            }
            rightPanel.add(executeButton)
        }
        
        if (planItem.status == TaskStatus.FAILED) {
            val retryButton = JButton(AutoDevIcons.REPAIR).apply {
                margin = JBUI.emptyInsets()
                isBorderPainted = false
                isContentAreaFilled = false
                preferredSize = Dimension(20, 20)
                addActionListener { executeSection() }
            }
            rightPanel.add(retryButton)
        }
        
        headerPanel.add(leftPanel, BorderLayout.WEST)
        headerPanel.add(rightPanel, BorderLayout.EAST)
        
        return headerPanel
    }
    
    private fun toggleStepsVisibility(visible: Boolean) {
        scrollPane.isVisible = visible
        SwingUtilities.invokeLater {
            parent?.revalidate()
            parent?.repaint()
        }
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
            stepsPanel.add(Box.createVerticalStrut(2))  // Reduced spacing between steps
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
            TaskStatus.COMPLETED -> AutoDevColors.COMPLETED_STATUS
            TaskStatus.FAILED -> AutoDevColors.FAILED_STATUS
            TaskStatus.IN_PROGRESS -> AutoDevColors.IN_PROGRESS_STATUS
            TaskStatus.TODO -> AutoDevColors.TODO_STATUS
        }
    }
}

