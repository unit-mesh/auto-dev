package cc.unitmesh.devti.gui.planner

import cc.unitmesh.devti.gui.AutoDevPlannerToolWindowFactory
import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.shadow.ShadowPanel
import cc.unitmesh.devti.sketch.ui.plan.PlanLangSketch
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

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
    private val planPanel: JPanel by lazy { createPlanPanel() }
    private lateinit var issueInputPanel: ShadowPanel

    init {
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
                        contentPanel.components.find { it is LoadingPanel }?.let {
                            contentPanel.remove(it)
                            contentPanel.revalidate()
                            contentPanel.repaint()
                        }
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
            project,
            title = "Enter Issue Description",
            onSubmit = { issueText ->
                if (issueText.isNotBlank()) {
                    showLoadingState(issueText)
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

    private fun showLoadingState(issueText: String) {
        content = issueText
        val parsedItems = MarkdownPlanParser.parse(issueText).toMutableList()
        planLangSketch.updatePlan(parsedItems)

        contentPanel.removeAll()
        contentPanel.add(planPanel, BorderLayout.CENTER)

        val loadingPanel = LoadingPanel(project)
        contentPanel.add(loadingPanel, BorderLayout.NORTH)

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

                    it.switchToEditorView()
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
                it.switchToIssueInputView()
                toolWindow.show()
            }
        }
    }
}

private class LoadingPanel(project: Project) : JPanel(BorderLayout()) {
    private val textPane = JTextPane()
    private val timer = Timer(50, null)
    private var currentText = ""
    private var currentIndex = 0
    private val loadingTexts = listOf(
        "🤔 Analyzing your request...",
        "💡 Generating a plan...",
        "⚙️ Processing the steps...",
        "✨ Almost there..."
    )
    private var currentTextIndex = 0

    init {
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = JBUI.Borders.empty(10)
        preferredSize = Dimension(0, JBUI.scale(60))

        val containerPanel = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val gradient = GradientPaint(
                    0f, 0f,
                    JBUI.CurrentTheme.ToolWindow.background(),
                    0f, height.toFloat(),
                    JBUI.CurrentTheme.ToolWindow.background().darker()
                )
                g2d.paint = gradient
                g2d.fillRect(0, 0, width, height)
            }
        }
        containerPanel.border = LineBorder(UIUtil.getBoundsColor(), 1, true)
        containerPanel.preferredSize = Dimension(0, JBUI.scale(50))

        textPane.apply {
            background = Color(0, 0, 0, 0)
            foreground = UIUtil.getLabelForeground()
            font = JBUI.Fonts.create("Monospaced", 14)
            isEditable = false
            isOpaque = false
            border = JBUI.Borders.empty(10, 0)
        }

        containerPanel.add(textPane, BorderLayout.CENTER)
        add(containerPanel, BorderLayout.CENTER)

        startTypingAnimation()
    }

    private fun startTypingAnimation() {
        timer.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                if (currentIndex < loadingTexts[currentTextIndex].length) {
                    currentText += loadingTexts[currentTextIndex][currentIndex]
                    updateText()
                    currentIndex++
                } else {
                    currentIndex = 0
                    currentText = ""
                    currentTextIndex = (currentTextIndex + 1) % loadingTexts.size
                }
            }
        })
        timer.start()
    }

    private fun updateText() {
        val doc = textPane.styledDocument
        val style = SimpleAttributeSet()
        StyleConstants.setForeground(style, UIUtil.getLabelForeground())
        StyleConstants.setFontSize(style, 14)
        StyleConstants.setFontFamily(style, "Monospaced")

        try {
            doc.remove(0, doc.length)
            doc.insertString(0, currentText, style)
        } catch (e: Exception) {
            // Ignore any document modification errors
        }
    }

    override fun removeNotify() {
        super.removeNotify()
        timer.stop()
    }
}
