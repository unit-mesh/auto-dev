package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.gui.AutoDevPlannerToolWindowFactory
import cc.unitmesh.devti.gui.planner.AutoDevPlannerToolWindow
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.TaskStatus
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities.invokeLater

class PlanLangSketch(
    private val project: Project,
    private var content: String,
    private var agentTaskItems: MutableList<AgentTaskEntry>,
    private val isInToolwindow: Boolean = false,
    private val autoPinEnabled: Boolean = true
) : JBPanel<PlanLangSketch>(BorderLayout(JBUI.scale(0), 0)), ExtensionLangSketch {
    private val contentPanel = JPanel(VerticalLayout(JBUI.scale(0)))
    var scrollPane: JBScrollPane? = null
    private val toolbarFactory = PlanToolbarFactory(project)
    private var hasUpdated = false

    // 压缩模式相关
    private var isCompressed = false
    private var compressedPanel: JPanel? = null
    private var fullContentPanel: JPanel? = null

    init {
        setupUI()
        renderPlan()

        // 默认Pin到工具窗口（如果启用）
        if (autoPinEnabled && !isInToolwindow) {
            // 延迟执行，确保组件已完全初始化
            invokeLater {
                autoPinToToolWindow()
            }
        }
    }

    private fun setupUI() {
        // 创建压缩面板
        setupCompressedPanel()

        // 创建完整内容面板
        setupFullContentPanel()

        // 默认显示完整内容
        showFullContent()

        minimumSize = Dimension(200, 0)
    }

    private fun setupCompressedPanel() {
        compressedPanel = JPanel(BorderLayout()).apply {
            background = JBUI.CurrentTheme.ToolWindow.background()
            border = JBUI.Borders.compound(
                JBUI.Borders.empty(0, 4),
                JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 1)
            )

            val titlePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5)).apply {
                background = JBUI.CurrentTheme.ToolWindow.background()

                val expandIcon = JBLabel(AllIcons.General.ArrowRight).apply {
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            toggleCompression()
                        }
                    })
                }

                val titleLabel = JLabel("Plan (${agentTaskItems.size} tasks)").apply {
                    font = font.deriveFont(font.style or java.awt.Font.BOLD)
                }

                add(expandIcon)
                add(titleLabel)
            }

            add(titlePanel, BorderLayout.CENTER)
            preferredSize = Dimension(preferredSize.width, 30)
        }
    }

    private fun setupFullContentPanel() {
        if (!isInToolwindow) {
            val toolbar = toolbarFactory.createToolbar(this)
            // 在工具栏中添加压缩按钮
            addCompressButtonToToolbar(toolbar)
        }

        scrollPane = JBScrollPane(contentPanel).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = JBUI.Borders.empty()

            viewport.isOpaque = false
            viewport.view = contentPanel
        }

        fullContentPanel = JPanel(BorderLayout()).apply {
            background = JBUI.CurrentTheme.ToolWindow.background()

            if (!isInToolwindow) {
                val toolbar = toolbarFactory.createToolbar(this@PlanLangSketch)
                add(toolbar, BorderLayout.NORTH)
                border = JBUI.Borders.compound(
                    JBUI.Borders.empty(0, 4),
                    JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 1)
                )
            }

            add(scrollPane, BorderLayout.CENTER)
        }
    }

    private fun addCompressButtonToToolbar(toolbar: JComponent) {
        // 这里可以添加压缩按钮到工具栏，但为了简化，我们使用双击标题来切换
    }

    private fun showFullContent() {
        removeAll()
        fullContentPanel?.let { add(it, BorderLayout.CENTER) }
        isCompressed = false
        revalidate()
        repaint()
    }

    private fun showCompressedContent() {
        removeAll()
        compressedPanel?.let { add(it, BorderLayout.CENTER) }
        isCompressed = true
        revalidate()
        repaint()
    }

    fun toggleCompression() {
        if (isCompressed) {
            showFullContent()
        } else {
            showCompressedContent()
        }
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

        // 更新压缩面板中的任务数量
        updateCompressedPanelInfo()
    }

    private fun updateCompressedPanelInfo() {
        compressedPanel?.let { panel ->
            val titlePanel = panel.getComponent(0) as? JPanel
            titlePanel?.let { tp ->
                val titleLabel = tp.components.find { it is JLabel && it != tp.components[0] } as? JLabel
                titleLabel?.text = "Plan (${agentTaskItems.size} tasks)"
            }
        }
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

            // 自动Pin到工具窗口
            if (autoPinEnabled) {
                autoPinToToolWindow()
            }

            hasUpdated = true
        }
    }

    private fun autoPinToToolWindow() {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(AutoDevPlannerToolWindowFactory.PlANNER_ID)
            ?: return

        val codingPanel = toolWindow.contentManager.component.components?.filterIsInstance<AutoDevPlannerToolWindow>()
            ?.firstOrNull()

        toolWindow.activate {
            val agentStateService = project.getService(AgentStateService::class.java)
            val currentPlan = agentStateService.getPlan()
            val planString = MarkdownPlanParser.formatPlanToMarkdown(currentPlan)

            codingPanel?.switchToPlanView(planString)
        }
    }

    override fun getComponent(): JComponent = this

    override fun dispose() {}
}
