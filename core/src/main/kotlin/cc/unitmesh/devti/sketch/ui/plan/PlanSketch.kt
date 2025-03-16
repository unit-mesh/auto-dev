package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.AutoDevPlanerToolWindowFactory
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.observer.plan.AgentPlanStep
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.TaskStatus
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
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
        border = JBUI.Borders.empty(1, 16, 1, 0)
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
        val titlePanel = createSectionTitlePanel()
        add(titlePanel)
        
        // Add all tasks in this section
        planItem.steps.forEach { task ->
            add(TaskPanel(project, task) {
                updateSectionStatus()
                onStatusChange()
            })
        }
    }
    
    private fun createSectionTitlePanel(): JPanel {
        val titlePanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 2, 0)).apply {
            border = JBUI.Borders.empty(2)
        }
        
        // Add execute button for todo sections
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
            isOpaque = true
            preferredSize = Dimension(20, 20)
            toolTipText = "Execute Task"
            
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

/**
 * Controller class for managing the plan data and UI updates
 */
class PlanController(
    private val project: Project, 
    private val contentPanel: JPanel,
    private var agentTaskItems: MutableList<AgentTaskEntry>
) {
    fun renderPlan() {
        contentPanel.removeAll()
        
        agentTaskItems.forEachIndexed { index, planItem ->
            val sectionPanel = SectionPanel(project, index, planItem) {
                contentPanel.revalidate()
                contentPanel.repaint()
            }
            
            contentPanel.add(sectionPanel)
            
            if (index < agentTaskItems.size - 1) {
                contentPanel.add(Box.createVerticalStrut(8))
            }
        }
        
        contentPanel.revalidate()
        contentPanel.repaint()
    }
    
    fun updatePlan(newPlanItems: List<AgentTaskEntry>) {
        if (newPlanItems.isEmpty()) {
            return
        }
        
        // Save current states of all tasks
        val taskStateMap = mutableMapOf<String, Pair<Boolean, TaskStatus>>()
        
        agentTaskItems.forEach { planItem ->
            planItem.steps.forEach { task ->
                taskStateMap[task.step] = Pair(task.completed, task.status)
            }
        }
        
        agentTaskItems.clear()
        
        newPlanItems.forEach { newItem ->
            agentTaskItems.add(newItem)
            
            newItem.steps.forEach { task ->
                // Restore saved states if available
                taskStateMap[task.step]?.let { (completed, status) ->
                    task.completed = completed
                    task.status = status
                }
            }
        }
        
        renderPlan()
    }
    
    fun savePlanToService() {
        project.getService(AgentStateService::class.java).updatePlan(agentTaskItems)
    }
}

/**
 * Main PlanSketch class that integrates all components
 */
class PlanSketch(
    private val project: Project,
    private var content: String,
    private var agentTaskItems: MutableList<AgentTaskEntry>,
    private val isInToolwindow: Boolean = false
) : JBPanel<PlanSketch>(BorderLayout(JBUI.scale(8), 0)), ExtensionLangSketch {
    private val contentPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBEmptyBorder(JBUI.insets(4))
    }
    
    private val toolbarFactory = PlanToolbarFactory(project)
    private val planController = PlanController(project, contentPanel, agentTaskItems)
    
    init {
        if (!isInToolwindow) {
            add(toolbarFactory.createToolbar(), BorderLayout.NORTH)
            border = JBUI.Borders.empty(8)
        }
        
        planController.renderPlan()
        add(contentPanel, BorderLayout.CENTER)
    }

    override fun getExtensionName(): String = "ThoughtPlan"
    
    override fun getViewText(): String = content
    
    override fun updateViewText(text: String, complete: Boolean) {
        this.content = text
        val agentPlans = MarkdownPlanParser.parse(text)
        planController.updatePlan(agentPlans)
    }
    
    override fun onComplete(context: String) {
        if (!isInToolwindow) {
            val agentPlans = MarkdownPlanParser.parse(content).toMutableList()
            planController.updatePlan(agentPlans)
            planController.savePlanToService()
        }
    }
    
    fun updatePlan(newPlanItems: List<AgentTaskEntry>) {
        planController.updatePlan(newPlanItems)
    }
    
    override fun getComponent(): JComponent = this
    
    override fun updateLanguage(language: Language?, originLanguage: String?) {}
    
    override fun dispose() {}
}
