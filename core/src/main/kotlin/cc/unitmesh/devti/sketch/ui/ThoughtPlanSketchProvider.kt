package cc.unitmesh.devti.sketch.ui

import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.sketch.ui.plan.MarkdownPlanParser
import cc.unitmesh.devti.sketch.ui.plan.PlanItem
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

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
    private val planItems: MutableList<PlanItem>
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
        add(titleLabel, BorderLayout.WEST)
        add(toolbar.component, BorderLayout.EAST)
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
                executePopup().mouseClicked(null)
            }
        }

        return listOf(popupAction)
    }
    
    private fun executePopup(): MouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            var popup: JBPopup? = null
            popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setProject(project)
                .setResizable(true)
                .setMovable(true)
                .setTitle("Thought Plan")
                .setCancelButton(MinimizeButton("Hide"))
                .setCancelCallback {
                    popup?.cancel()
                    // Return the panel to its original location
                    remove(panel)
                    add(panel, BorderLayout.CENTER)
                    revalidate()
                    repaint()
                    true
                }
                .setFocusable(true)
                .setRequestFocus(true)
                .createPopup()

            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            if (editor != null) {
                popup.showInBestPositionFor(editor)
            } else {
                popup.showInFocusCenter()
            }
        }
    }

    private fun createPlanUI() {
        planItems.forEachIndexed { index, planItem ->
            val titlePanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
                border = JBUI.Borders.empty()
            }

            val titleText = if (planItem.completed)
                "<html><b>${index + 1}. ${planItem.title} ✓</b></html>"
            else
                "<html><b>${index + 1}. ${planItem.title}</b></html>"

            val sectionLabel = JLabel(titleText)
            sectionLabel.border = JBUI.Borders.empty()
            titlePanel.add(sectionLabel)
            contentPanel.add(titlePanel)

            planItem.tasks.forEachIndexed { taskIndex, task ->
                val taskPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
                    border = JBUI.Borders.empty()
                }

                val checkbox = JBCheckBox(task).apply {
                    isSelected = planItem.taskCompleted[taskIndex]
                    addActionListener {
                        planItem.taskCompleted[taskIndex] = isSelected
                    }
                }

                taskPanel.add(checkbox)
                contentPanel.add(taskPanel)
            }

            if (index < planItems.size - 1) {
                contentPanel.add(Box.createVerticalStrut(8))
            }
        }
    }

    override fun getExtensionName(): String = "ThoughtPlan"

    override fun getViewText(): String = content

    override fun updateViewText(text: String, complete: Boolean) {
        this.content = text
        updateUi(text)
    }

    private fun updateUi(text: String) {
        val newPlanItems = MarkdownPlanParser.parse(text)
        if (newPlanItems.isNotEmpty()) {
            val completionState = mutableMapOf<String, Boolean>()
            planItems.forEach { planItem ->
                planItem.tasks.forEachIndexed { index, task ->
                    val taskKey = task.replace("✓", "").trim()
                    completionState[taskKey] = planItem.taskCompleted[index]
                }
            }

            contentPanel.removeAll()
            planItems.clear()

            newPlanItems.forEach { newItem ->
                val taskCompletedList = MutableList(newItem.tasks.size) { index ->
                    val taskKey = newItem.tasks[index].replace("✓", "").trim()
                    completionState[taskKey] ?: newItem.taskCompleted[index]
                }
                
                planItems.add(PlanItem(
                    newItem.title,
                    newItem.tasks,
                    newItem.completed,
                    taskCompletedList
                ))
            }

            createPlanUI()
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    override fun getComponent(): JComponent = this

    override fun onDoneStream(allText: String) {
        updateUi(this.content)
    }

    override fun updateLanguage(language: Language?, originLanguage: String?) {}

    override fun dispose() {}
}
