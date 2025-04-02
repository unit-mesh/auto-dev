package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.TaskStatus
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class PlanLangSketch(
    private val project: Project,
    private var content: String,
    private var agentTaskItems: MutableList<AgentTaskEntry>,
    private val isInToolwindow: Boolean = false
) : JBPanel<PlanLangSketch>(BorderLayout(JBUI.scale(0), 0)), ExtensionLangSketch {
    private val contentPanel = JPanel(VerticalLayout(JBUI.scale(0)))
    val scrollPane: JBScrollPane
    private val toolbarFactory = PlanToolbarFactory(project)
    private var hasUpdated = false

    init {
        if (!isInToolwindow) {
            add(toolbarFactory.createToolbar(this), BorderLayout.NORTH)
            border = JBUI.Borders.compound(
                JBUI.Borders.empty(0, 4),
                JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 1)
            )
        }

        renderPlan()

        scrollPane = JBScrollPane(contentPanel).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = JBUI.Borders.empty()

            viewport.isOpaque = false
            viewport.view = contentPanel
        }

        val wrapperPanel = JPanel(BorderLayout())
        wrapperPanel.add(scrollPane, BorderLayout.CENTER)
        wrapperPanel.background = JBUI.CurrentTheme.ToolWindow.background()

        add(wrapperPanel, BorderLayout.CENTER)

        minimumSize = Dimension(200, 0)
    }

    fun renderPlan() {
        contentPanel.removeAll()
        agentTaskItems.forEachIndexed { index, planItem ->
            val taskSectionPanel = TaskSectionPanel(project, index, planItem) {
                contentPanel.revalidate()
                contentPanel.repaint()
            }

            contentPanel.add(taskSectionPanel)
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    fun updatePlan(newPlanItems: List<AgentTaskEntry>) {
        if (newPlanItems.isEmpty()) {
            contentPanel.removeAll()
            contentPanel.revalidate()
            contentPanel.repaint()
            return
        }

        val taskStateMap = mutableMapOf<String, Pair<Boolean, TaskStatus>>()
        agentTaskItems.forEach { planItem ->
            planItem.steps.forEach { task ->
                taskStateMap[task.step] = Pair(task.completed, task.status)
            }
        }

        agentTaskItems.clear()

        newPlanItems.forEach { newItem ->
            agentTaskItems.add(newItem)
            newItem.updateCompletionStatus()
        }

        renderPlan()
    }

    fun savePlanToService() {
        project.getService(AgentStateService::class.java).updatePlan(agentTaskItems)
    }

    override fun getExtensionName(): String = "ThoughtPlan"

    override fun getViewText(): String = content

    override fun updateViewText(text: String, complete: Boolean) {
        this.content = text
        val agentPlans = MarkdownPlanParser.parse(text)
        updatePlan(agentPlans)
    }

    override fun onComplete(code: String) {
        if (hasUpdated) return
        if (!isInToolwindow) {
            val agentPlans = MarkdownPlanParser.parse(content).toMutableList()
            updatePlan(agentPlans)
            savePlanToService()

            hasUpdated = true
        }
    }

    override fun getComponent(): JComponent = this

    override fun dispose() {}
}
