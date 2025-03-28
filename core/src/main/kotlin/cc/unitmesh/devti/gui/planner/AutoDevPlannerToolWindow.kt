package cc.unitmesh.devti.gui.planner

import cc.unitmesh.devti.gui.AutoDevPlannerToolWindowFactory
import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.sketch.ui.plan.PlanLangSketch
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

class AutoDevPlannerToolWindow(val project: Project) : SimpleToolWindowPanel(true, true), Disposable {
    override fun getName(): String = "AutoDev Planner"
    var connection = ApplicationManager.getApplication().messageBus.connect(this)
    var content = ""
    var planLangSketch: PlanLangSketch =
        PlanLangSketch(project, content, MarkdownPlanParser.parse(content).toMutableList(), true)

    private var markdownEditor: MarkdownLanguageField? = null
    private val contentPanel = JPanel(BorderLayout())
    private var isEditorMode = false
    private var isIssueInputMode = false
    private var currentCallback: ((String) -> Unit)? = null
    private var issueInputCallback: ((String) -> Unit)? = null
    private val planPanel: JPanel by lazy { createPlanPanel() }
    private lateinit var issueInputPanel: ShadowPanel

    init {
        // Check if there's no plan content and conditionally show the appropriate panel
        if (content.isBlank()) {
            isIssueInputMode = true
            contentPanel.add(createIssueInputPanel(), BorderLayout.CENTER)
        } else {
            contentPanel.add(planPanel, BorderLayout.CENTER)
        }

        add(contentPanel, BorderLayout.CENTER)

        connection.subscribe(PlanUpdateListener.Companion.TOPIC, object : PlanUpdateListener {
            override fun onPlanUpdate(items: MutableList<AgentTaskEntry>) {
                if (!isEditorMode && !isIssueInputMode) {
                    runInEdt {
                        planLangSketch.updatePlan(items)
                    }
                }
            }
        })
    }

    private fun createPlanPanel(): JPanel {
        return panel {
            row {
                cell(planLangSketch)
                    .fullWidth()
                    .resizableColumn()
            }
        }.apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(8)
            )
            background = JBUI.CurrentTheme.ToolWindow.background()
        }
    }

    private fun createIssueInputPanel(): JPanel {
        issueInputPanel = ShadowPanel(
            title = "Enter Issue Description",
            submitButtonText = "Generate Tasks",
            cancelButtonText = "Cancel",
            onSubmit = { issueText ->
                if (issueText.isNotBlank()) {
                    issueInputCallback?.invoke(issueText)
                    switchToPlanView()
                }
            },
            onCancel = {
                switchToPlanView()
            }
        )
        return issueInputPanel
    }

    private fun switchToEditorView() {
        if (isEditorMode) return
        if (isIssueInputMode) {
            isIssueInputMode = false
        }

        contentPanel.removeAll()
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
        contentPanel.revalidate()
        contentPanel.repaint()

        isEditorMode = true
    }

    private fun switchToIssueInputView() {
        if (isIssueInputMode) return
        if (isEditorMode) {
            isEditorMode = false
        }

        contentPanel.removeAll()
        contentPanel.add(issueInputPanel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()

        isIssueInputMode = true
        issueInputPanel.setText("")
        issueInputPanel.requestTextAreaFocus()
    }

    fun switchToPlanView(newContent: String? = null) {
        if (newContent != null && newContent != content) {
            content = newContent

            val parsedItems = MarkdownPlanParser.parse(newContent).toMutableList()
            planLangSketch.updatePlan(parsedItems)
        }

        contentPanel.removeAll()
        contentPanel.add(planPanel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()

        isEditorMode = false
        isIssueInputMode = false
    }

    override fun dispose() {
        markdownEditor = null
    }

    companion object {
        fun showPlanEditor(project: Project, planText: String, callback: (String) -> Unit) {
            val toolWindow =
                ToolWindowManager.Companion.getInstance(project).getToolWindow(AutoDevPlannerToolWindowFactory.Companion.PlANNER_ID)
            if (toolWindow != null) {
                val content = toolWindow.contentManager.getContent(0)
                val plannerWindow = content?.component as? AutoDevPlannerToolWindow

                plannerWindow?.let {
                    it.currentCallback = callback
                    if (planText.isNotEmpty() && planText != it.content) {
                        it.content = planText
                    }

                    it.switchToEditorView()
                    toolWindow.show()
                }
            }
        }

        fun showIssueInput(project: Project, callback: (String) -> Unit) {
            val toolWindow = ToolWindowManager.Companion.getInstance(project).getToolWindow(
                AutoDevPlannerToolWindowFactory.Companion.PlANNER_ID)
            if (toolWindow == null) return

            val content = toolWindow.contentManager.getContent(0)
            val plannerWindow = content?.component as? AutoDevPlannerToolWindow

            plannerWindow?.let {
                it.issueInputCallback = callback
                it.switchToIssueInputView()
                toolWindow.show()
            }
        }
    }
}
