package cc.unitmesh.devti.gui.planner

import cc.unitmesh.devti.gui.AutoDevPlannerToolWindowFactory
import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.shadow.IssueInputPanel
import cc.unitmesh.devti.sketch.ui.plan.PlanLangSketch
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.dsl.builder.panel
import java.awt.*
import javax.swing.*

class AutoDevPlannerToolWindow(val project: Project) : SimpleToolWindowPanel(true, true), Disposable {
    override fun getName(): String = "AutoDev Planner"
    var connection = ApplicationManager.getApplication().messageBus.connect(this)
    var content = ""
    var planLangSketch: PlanLangSketch =
        PlanLangSketch(project, content, MarkdownPlanParser.parse(content).toMutableList(), true)

    private var markdownEditor: MarkdownLanguageField? = null
    private val contentPanel = JPanel(BorderLayout())
    
    private var currentView: PlannerView? = null
    private var currentCallback: ((String) -> Unit)? = null
    private val plannerResultSummary = PlannerResultSummary(project, mutableListOf())

    init {
        val toolbar = ActionManager.getInstance()
            .createActionToolbar(
                AutoDevPlannerToolWindowFactory.Companion.PlANNER_ID,
                ActionUtil.getAction("AutoDevPlanner.ToolWindow.TitleActions") as ActionGroup,
                true
            )
            
        val toolbarPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        toolbarPanel.add(toolbar.component)
        
        add(toolbarPanel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)

        if (content.isBlank()) {
            switchToView(IssueInputView())
        } else {
            switchToView(PlanView())
        }

        connection.subscribe(PlanUpdateListener.Companion.TOPIC, object : PlanUpdateListener {
            override fun onPlanUpdate(items: MutableList<AgentTaskEntry>) {
                if (currentView is PlanView) {
                    runInEdt {
                        planLangSketch.updatePlan(items)
                        contentPanel.components.find { it is LoadingPanel }?.let {
                            contentPanel.remove(it)
                            contentPanel.revalidate()
                            contentPanel.repaint()
                        }
                    }
                }
            }

            override fun onUpdateChange(changes: MutableList<Change>) {
                runInEdt {
                    plannerResultSummary.updateChanges(changes)
                    if (currentView is PlanView) {
                        if (contentPanel.components.none { it == plannerResultSummary }) {
                            contentPanel.add(plannerResultSummary, BorderLayout.SOUTH)
                        }

                        contentPanel.revalidate()
                        contentPanel.repaint()
                    }
                }
            }
        })
    }

    private fun switchToView(view: PlannerView) {
        if (currentView?.viewType == view.viewType) return
        
        contentPanel.removeAll()
        
        currentView = view
        view.initialize(this)
        
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    fun switchToPlanView(newContent: String? = null) {
        if (newContent != null && newContent != content) {
            content = newContent
            val parsedItems = MarkdownPlanParser.parse(newContent).toMutableList()
            planLangSketch.updatePlan(parsedItems)
        }

        switchToView(PlanView())
    }

    fun showLoadingState(issueText: String) {
        content = issueText
        val parsedItems = MarkdownPlanParser.parse(issueText).toMutableList()
        planLangSketch.updatePlan(parsedItems)

        switchToView(LoadingView())
    }

    override fun dispose() {
        markdownEditor = null
    }
    
    interface PlannerView {
        val viewType: PlannerViewType
        fun initialize(window: AutoDevPlannerToolWindow)
    }
    
    enum class PlannerViewType {
        PLAN, EDITOR, ISSUE_INPUT, LOADING
    }
    
    inner class PlanView : PlannerView {
        override val viewType = PlannerViewType.PLAN
        
        override fun initialize(window: AutoDevPlannerToolWindow) {
            val planPanel = panel {
                row {
                    cell(planLangSketch)
                        .fullWidth()
                        .resizableColumn()
                }
            }
            
            contentPanel.add(planPanel, BorderLayout.CENTER)
            contentPanel.add(plannerResultSummary, BorderLayout.SOUTH)
        }
    }
    
    inner class EditorView : PlannerView {
        override val viewType = PlannerViewType.EDITOR
        
        override fun initialize(window: AutoDevPlannerToolWindow) {
            val editPlanPanel = EditPlanPanel(
                project = project,
                content = content,
                onSave = { newContent ->
                    if (newContent == content) {
                        return@EditPlanPanel
                    }
                    switchToPlanView(newContent)
                    currentCallback?.invoke(newContent)
                },
                onCancel = {
                    switchToPlanView()
                }
            )

            contentPanel.add(editPlanPanel, BorderLayout.CENTER)
        }
    }
    
    inner class IssueInputView : PlannerView {
        override val viewType = PlannerViewType.ISSUE_INPUT
        private lateinit var issueInputPanel: IssueInputPanel
        
        override fun initialize(window: AutoDevPlannerToolWindow) {
            issueInputPanel = IssueInputPanel(
                project,
                onSubmit = { issueText ->
                    if (issueText.isNotBlank()) {
                        showLoadingState(issueText)
                    }
                },
                onCancel = {
                    switchToPlanView()
                }
            )
            
            contentPanel.add(issueInputPanel, BorderLayout.CENTER)
            issueInputPanel.setText("")
            issueInputPanel.requestTextAreaFocus()
        }
    }
    
    inner class LoadingView : PlannerView {
        override val viewType = PlannerViewType.LOADING
        
        override fun initialize(window: AutoDevPlannerToolWindow) {
            val planPanel = panel {
                row {
                    cell(planLangSketch)
                        .fullWidth()
                        .resizableColumn()
                }
            }
            
            contentPanel.add(planPanel, BorderLayout.CENTER)
            contentPanel.add(LoadingPanel(project), BorderLayout.NORTH)
        }
    }

    companion object {
        fun showPlanEditor(project: Project, planText: String, callback: (String) -> Unit) {
            val toolWindow =
                ToolWindowManager.Companion.getInstance(project)
                    .getToolWindow(AutoDevPlannerToolWindowFactory.Companion.PlANNER_ID)
            if (toolWindow != null) {
                val content = toolWindow.contentManager.getContent(0)
                val plannerWindow = content?.component as? AutoDevPlannerToolWindow

                plannerWindow?.let {
                    it.currentCallback = callback
                    if (planText.isNotEmpty() && planText != it.content) {
                        it.content = planText
                    }

                    it.switchToView(it.EditorView())
                    toolWindow.show()
                }
            }
        }

        fun showIssueInput(project: Project) {
            val toolWindow = ToolWindowManager.Companion.getInstance(project).getToolWindow(
                AutoDevPlannerToolWindowFactory.Companion.PlANNER_ID
            )
            if (toolWindow == null) return

            val content = toolWindow.contentManager.getContent(0)
            val plannerWindow = content?.component as? AutoDevPlannerToolWindow

            plannerWindow?.let {
                it.switchToView(it.IssueInputView())
                toolWindow.show()
            }
        }
    }
}
