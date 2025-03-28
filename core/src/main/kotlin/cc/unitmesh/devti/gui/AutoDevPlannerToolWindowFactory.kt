package cc.unitmesh.devti.gui

import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.sketch.ui.plan.PlanLangSketch
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splittable
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.psi.PsiFile
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.lang.LanguageUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileTypes.FileTypeManager
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.Box
import javax.swing.JTextArea
import javax.swing.JLabel
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicBoolean

class AutoDevPlannerToolWindowFactory : ToolWindowFactory, ToolWindowManagerListener, DumbAware {
    private val orientation = AtomicBoolean(true)

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val panel = AutoDevPlannerToolWindow(project)
        val manager = toolWindow.contentManager
        manager.addContent(manager.factory.createContent(panel, null, false).apply { isCloseable = false })
        project.messageBus.connect(manager).subscribe(ToolWindowManagerListener.TOPIC, this)
        toolWindow.setTitleActions(listOfNotNull(ActionUtil.getAction("AutoDevPlanner.ToolWindow.TitleActions")))
    }

    override fun stateChanged(manager: ToolWindowManager) {
        val window = manager.getToolWindow(PlANNER_ID) ?: return
        if (window.isDisposed) return
        val vertical = !window.anchor.isHorizontal
        if (vertical != orientation.getAndSet(vertical)) {
            for (content in window.contentManager.contents) {
                val splittable = content?.component as? Splittable
                splittable?.orientation = vertical
            }
        }
    }

    companion object {
        val PlANNER_ID = "AutoDevPlanner"
    }
}

class AutoDevPlannerToolWindow(val project: Project) : SimpleToolWindowPanel(true, true), Disposable {
    override fun getName(): String = "AutoDev Planner"
    var connection = ApplicationManager.getApplication().messageBus.connect(this)
    var content = ""
    var planLangSketch: PlanLangSketch = PlanLangSketch(project, content, MarkdownPlanParser.parse(content).toMutableList(), true)

    private var markdownEditor: MarkdownLanguageField? = null
    private val contentPanel = JPanel(BorderLayout())
    private var isEditorMode = false
    private var isIssueInputMode = false
    private var currentCallback: ((String) -> Unit)? = null
    private var issueInputCallback: ((String) -> Unit)? = null
    private val planPanel: JPanel by lazy { createPlanPanel() }
    private val issueInputPanel: JPanel by lazy { createIssueInputPanel() }
    private var issueTextArea: JTextArea? = null

    init {
        // Check if there's no plan content and conditionally show the appropriate panel
        if (content.isBlank()) {
            isIssueInputMode = true
            contentPanel.add(issueInputPanel, BorderLayout.CENTER)
        } else {
            contentPanel.add(planPanel, BorderLayout.CENTER)
        }
        
        add(contentPanel, BorderLayout.CENTER)

        connection.subscribe(PlanUpdateListener.TOPIC, object : PlanUpdateListener {
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
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)
        panel.background = JBUI.CurrentTheme.ToolWindow.background()

        val headerLabel = JLabel("Enter Issue Description").apply {
            font = font.deriveFont(font.size + 2f)
            border = JBUI.Borders.emptyBottom(10)
        }
        
        issueTextArea = JTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(5)
            text = ""
            rows = 15
        }
        
        val scrollPane = JBScrollPane(issueTextArea).apply {
            preferredSize = Dimension(400, 300)
        }
        
        val buttonPanel = JPanel(BorderLayout())
        val buttonsBox = Box.createHorizontalBox().apply {
            add(JButton("Generate Tasks").apply {
                addActionListener {
                    val issueText = issueTextArea?.text ?: ""
                    if (issueText.isNotBlank()) {
                        issueInputCallback?.invoke(issueText)
                        switchToPlanView()
                    }
                }
            })
            add(Box.createHorizontalStrut(10))
            add(JButton("Cancel").apply {
                addActionListener {
                    switchToPlanView()
                }
            })
        }
        buttonPanel.add(buttonsBox, BorderLayout.EAST)
        buttonPanel.border = JBUI.Borders.emptyTop(10)
        
        panel.add(headerLabel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        
        return panel
    }

    private fun switchToEditorView() {
        if (isEditorMode) return
        if (isIssueInputMode) {
            isIssueInputMode = false
        }

        if (markdownEditor == null) {
            markdownEditor = MarkdownLanguageField(project, content, "Edit your plan here...", "plan.md")
        } else {
            markdownEditor?.text = content
        }

        val buttonPanel = JPanel(BorderLayout())
        val buttonsBox = Box.createHorizontalBox().apply {
            add(JButton("Save").apply {
                addActionListener {
                    val newContent = markdownEditor?.text ?: ""
                    if (newContent == content) {
                        return@addActionListener
                    }

                    switchToPlanView(newContent)
                    currentCallback?.invoke(newContent)
                }
            })
            add(Box.createHorizontalStrut(10))
            add(JButton("Cancel").apply {
                addActionListener {
                    switchToPlanView()
                }
            })
        }
        buttonPanel.add(buttonsBox, BorderLayout.EAST)
        buttonPanel.border = JBUI.Borders.empty(5)

        contentPanel.removeAll()
        val editorPanel = JPanel(BorderLayout())
        editorPanel.add(JBScrollPane(markdownEditor), BorderLayout.CENTER)
        editorPanel.add(buttonPanel, BorderLayout.SOUTH)

        contentPanel.add(editorPanel, BorderLayout.CENTER)
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
        issueTextArea?.text = ""
        issueTextArea?.requestFocus()
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
        issueTextArea = null
    }

    companion object {
        fun showPlanEditor(project: Project, planText: String, callback: (String) -> Unit) {
            val toolWindow =
                ToolWindowManager.getInstance(project).getToolWindow(AutoDevPlannerToolWindowFactory.PlANNER_ID)
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
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(AutoDevPlannerToolWindowFactory.PlANNER_ID)
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

private class MarkdownLanguageField(
    private val myProject: Project?,
    val value: String,
    private val placeholder: String,
    private val fileName: String
) : LanguageTextField(
    LanguageUtil.getFileTypeLanguage(FileTypeManager.getInstance().getFileTypeByExtension("md")), myProject, value,
    object : SimpleDocumentCreator() {
        override fun createDocument(value: String?, language: Language?, project: Project?): Document {
            return createDocument(value, language, project, this)
        }

        override fun customizePsiFile(file: PsiFile?) {
            file?.name = fileName
        }
    }
) {
    override fun createEditor(): EditorEx {
        return super.createEditor().apply {
            setShowPlaceholderWhenFocused(true)
            setHorizontalScrollbarVisible(true)
            setVerticalScrollbarVisible(true)
            setPlaceholder(placeholder)

            val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
            this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)

            settings.isLineNumbersShown = true
            settings.isLineMarkerAreaShown = false
            settings.isFoldingOutlineShown = false
            settings.isUseSoftWraps = true
        }
    }
}

