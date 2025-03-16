package cc.unitmesh.devti.sketch.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.observer.plan.AgentPlan
import cc.unitmesh.devti.observer.plan.PlanTask
import cc.unitmesh.devti.observer.plan.TaskStatus
import cc.unitmesh.devti.observer.plan.PlanBoard
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class ThoughtPlanSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "plan"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        val planItems = MarkdownPlanParser.parse(content)
        if (planItems.isNotEmpty()) {
            return PlanSketch(project, content, planItems.toMutableList())
        }

        return object : CodeHighlightSketch(project, content, null), ExtensionLangSketch {
            override fun getExtensionName(): String = "ThoughtPlan"
        }
    }
}

class PlanSketch(
    private val project: Project,
    private var content: String,
    private val agentPlans: MutableList<AgentPlan>,
    private val isInPopup: Boolean = false
) : JBPanel<PlanSketch>(BorderLayout()), ExtensionLangSketch {
    private val panel = JBPanel<PlanSketch>(BorderLayout())
    private val contentPanel = JBPanel<PlanSketch>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBEmptyBorder(JBUI.insets(8))
    }

    private val actionGroup = DefaultActionGroup(createConsoleActions())
    private val toolbar = ActionManager.getInstance().createActionToolbar("PlanSketch", actionGroup, true).apply {
        targetComponent = panel
    }

    private val titleLabel = JLabel("Thought Plan").apply {
        border = JBUI.Borders.empty(0, 10)
    }

    private val toolbarPanel = JPanel(BorderLayout()).apply {
        if (!isInPopup) {
            add(titleLabel, BorderLayout.WEST)
            add(toolbar.component, BorderLayout.EAST)
        }
    }

    private val toolbarWrapper = Wrapper(JBUI.Panels.simplePanel(toolbarPanel)).also {
        it.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1, 1, 1, 1)
    }

    init {
        createPlanUI()

        val scrollPane = JBScrollPane(contentPanel)
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(toolbarWrapper, BorderLayout.NORTH)

        add(panel, BorderLayout.CENTER)
    }

    private fun createConsoleActions(): List<AnAction> {
        val popupAction = object : AnAction("Popup", "Show in popup window", AllIcons.Ide.External_link_arrow) {
            override fun displayTextInToolbar(): Boolean = true

            override fun actionPerformed(e: AnActionEvent) {
                project.getService(AgentStateService::class.java).updatePlan(agentPlans)
                project.getService(PlanBoard::class.java).updateShow()
            }
        }

        return listOf(popupAction)
    }


    private fun createPlanUI() {
        agentPlans.forEachIndexed { index, planItem ->
            val titlePanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
                border = JBUI.Borders.empty()
            }

            // Check if all tasks in the section are completed
            updateSectionCompletionStatus(planItem)

            // Create a formatted title with the appropriate status marker
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
            sectionLabel.border = JBUI.Borders.empty()
            titlePanel.add(sectionLabel)
            contentPanel.add(titlePanel)

            planItem.tasks.forEachIndexed { taskIndex, task ->
                val taskPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
                    border = JBUI.Borders.empty()
                }

                // First create task label with appropriate styling based on status
                val taskLabel = createStyledTaskLabel(task)

                // Create a custom status indicator based on task status
                val statusIcon = when (task.status) {
                    TaskStatus.COMPLETED -> JLabel(AllIcons.Actions.Checked)
                    TaskStatus.FAILED -> JLabel(AllIcons.General.Error)
                    TaskStatus.IN_PROGRESS -> JLabel(AllIcons.Actions.Execute) 
                    TaskStatus.TODO -> JBCheckBox().apply {
                        isSelected = task.completed
                        addActionListener {
                            task.completed = isSelected
                            if (isSelected) {
                                task.updateStatus(TaskStatus.COMPLETED)
                            } else {
                                task.updateStatus(TaskStatus.TODO)
                            }
                            
                            // Update section status when task status changes
                            val currentSection = agentPlans.find { it.tasks.contains(task) }
                            currentSection?.let { updateSectionCompletionStatus(it) }
                            
                            updateTaskLabel(taskLabel, task)
                            contentPanel.revalidate()
                            contentPanel.repaint()
                        }
                        isBorderPainted = false
                        isContentAreaFilled = false
                    }
                }
                
                taskPanel.add(statusIcon)

                // Add execute button for incomplete tasks
                if (task.status == TaskStatus.TODO || task.status == TaskStatus.IN_PROGRESS) {
                    val executeButton = JButton(AllIcons.Actions.Execute).apply {
                        border = BorderFactory.createEmptyBorder()
                        preferredSize = Dimension(24, 24)
                        toolTipText = "Execute"

                        addActionListener {
                            AutoDevToolWindowFactory.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                                ui.sendInput(AutoDevBundle.message("sketch.plan.finish.task") + task.step)
                            }
                        }
                    }

                    taskPanel.add(executeButton)
                }
                
                taskPanel.add(taskLabel)
                
                // Add context menu for changing task status
                val taskPopupMenu = JPopupMenu()
                val markCompletedItem = JMenuItem("Mark as Completed [✓]")
                val markInProgressItem = JMenuItem("Mark as In Progress [*]")
                val markFailedItem = JMenuItem("Mark as Failed [!]")
                val markTodoItem = JMenuItem("Mark as Todo [ ]")
                
                markCompletedItem.addActionListener {
                    task.updateStatus(TaskStatus.COMPLETED)
                    updateTaskLabel(taskLabel, task)
                    
                    // Update section status after changing task status
                    val currentSection = agentPlans.find { it.tasks.contains(task) }
                    currentSection?.let { updateSectionCompletionStatus(it) }
                    
                    contentPanel.revalidate()
                    contentPanel.repaint()
                }
                
                markInProgressItem.addActionListener {
                    task.updateStatus(TaskStatus.IN_PROGRESS)
                    updateTaskLabel(taskLabel, task)
                    
                    val currentSection = agentPlans.find { it.tasks.contains(task) }
                    currentSection?.let { updateSectionCompletionStatus(it) }
                    
                    contentPanel.revalidate()
                    contentPanel.repaint()
                }
                
                markFailedItem.addActionListener {
                    task.updateStatus(TaskStatus.FAILED)
                    updateTaskLabel(taskLabel, task)
                    
                    val currentSection = agentPlans.find { it.tasks.contains(task) }
                    currentSection?.let { updateSectionCompletionStatus(it) }
                    
                    contentPanel.revalidate()
                    contentPanel.repaint()
                }
                
                markTodoItem.addActionListener {
                    task.updateStatus(TaskStatus.TODO)
                    updateTaskLabel(taskLabel, task)
                    
                    val currentSection = agentPlans.find { it.tasks.contains(task) }
                    currentSection?.let { updateSectionCompletionStatus(it) }
                    
                    contentPanel.revalidate()
                    contentPanel.repaint()
                }
                
                taskPopupMenu.add(markCompletedItem)
                taskPopupMenu.add(markInProgressItem)
                taskPopupMenu.add(markFailedItem)
                taskPopupMenu.add(markTodoItem)
                
                taskLabel.componentPopupMenu = taskPopupMenu
                
                contentPanel.add(taskPanel)
            }

            if (index < agentPlans.size - 1) {
                contentPanel.add(Box.createVerticalStrut(8))
            }
        }
    }

    // Helper method to create a styled task label based on status
    private fun createStyledTaskLabel(task: PlanTask): JLabel {
        val labelText = when (task.status) {
            TaskStatus.COMPLETED -> "<html><strike>${task.step}</strike></html>"
            TaskStatus.FAILED -> "<html><span style='color:red'>${task.step}</span></html>"
            TaskStatus.IN_PROGRESS -> "<html><span style='color:blue;font-style:italic'>${task.step}</span></html>"
            TaskStatus.TODO -> task.step
        }
        
        return JLabel(labelText).apply {
            border = JBUI.Borders.emptyLeft(5)
        }
    }
    
    // Helper method to update the task label based on current status
    private fun updateTaskLabel(label: JLabel, task: PlanTask) {
        label.text = when (task.status) {
            TaskStatus.COMPLETED -> "<html><strike>${task.step}</strike></html>"
            TaskStatus.FAILED -> "<html><span style='color:red'>${task.step}</span></html>"
            TaskStatus.IN_PROGRESS -> "<html><span style='color:blue;font-style:italic'>${task.step}</span></html>"
            TaskStatus.TODO -> task.step
        }
    }

    // Helper method to update section completion status based on tasks
    private fun updateSectionCompletionStatus(planItem: AgentPlan) {
        // Use the new method instead of reflection
        planItem.updateCompletionStatus()
        
        // Update the UI to reflect the new status
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    override fun getExtensionName(): String = "ThoughtPlan"

    override fun getViewText(): String = content

    override fun updateViewText(text: String, complete: Boolean) {
        this.content = text
        updatePlan(text)
    }

    fun updatePlan(text: String) {
        updatePlan(MarkdownPlanParser.parse(text))
    }

    fun updatePlan(newPlanItems: List<AgentPlan>) {
        if (newPlanItems.isNotEmpty()) {
            // Save current states of all tasks
            val taskStateMap = mutableMapOf<String, Pair<Boolean, TaskStatus>>()

            agentPlans.forEach { planItem ->
                planItem.tasks.forEach { task ->
                    taskStateMap[task.step] = Pair(task.completed, task.status)
                }
            }

            contentPanel.removeAll()
            agentPlans.clear()

            newPlanItems.forEach { newItem ->
                agentPlans.add(newItem)

                newItem.tasks.forEach { task ->
                    // Restore saved states if available
                    taskStateMap[task.step]?.let { (completed, status) ->
                        task.completed = completed
                        task.status = status
                    }
                }
            }

            createPlanUI()
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    override fun getComponent(): JComponent = this

    override fun onComplete(allText: String) {
        if (!isInPopup) {
            updatePlan(this.content)
            project.getService(AgentStateService::class.java).updatePlan(agentPlans)
        }
    }

    override fun updateLanguage(language: Language?, originLanguage: String?) {}

    override fun dispose() {}
}
