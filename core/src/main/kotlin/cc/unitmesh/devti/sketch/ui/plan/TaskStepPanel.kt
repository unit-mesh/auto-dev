package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.plan.AgentPlanStep
import cc.unitmesh.devti.observer.plan.TaskStatus
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.event.HyperlinkEvent
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

/**
 * Task Panel UI Component responsible for rendering and handling interactions for a single task
 */
class TaskStepPanel(
    private val project: Project,
    private val task: AgentPlanStep,
    private val onStatusChange: () -> Unit
) : JBPanel<TaskStepPanel>() {
    private val taskLabel: JEditorPane

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = JBUI.Borders.empty(4, 16, 4, 0)
        background = JBUI.CurrentTheme.ToolWindow.background()
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
            preferredSize = Dimension(20, 20)
            toolTipText = "Execute"
            background = JBUI.CurrentTheme.ToolWindow.background()

            addActionListener {
                AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                    ui.sendInput(AutoDevBundle.message("sketch.plan.finish.task") + task.step)
                }
            }
        }
    }

    private fun createStyledTaskLabel(): JEditorPane {
        val labelText = getLabelTextByStatus()
        val editorKit = HTMLEditorKit()
        val styleSheet = StyleSheet()
        styleSheet.addRule("a { color: #FF0000; text-decoration: underline red; }")
        styleSheet.addRule("a:hover { color: #FF0000; }")
        editorKit.styleSheet = styleSheet

        val document = HTMLDocument()
        document.putProperty("IgnoreCharsetDirective", true)
        project.basePath?.let {
            val url: String? = LocalFileSystem.getInstance().findFileByPath(it)?.url
            document.putProperty("Base", url)
        }

        return JEditorPane().apply {
            this.editorKit = editorKit
            this.document = document
            text = "<html>$labelText</html>"
            border = JBUI.Borders.emptyLeft(5)
            isEditable = false
            background = JBUI.CurrentTheme.ToolWindow.background()
            addHyperlinkListener { e ->
                if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    val filePath = e.url?.path ?: e.description
                    val realPath = project.basePath + "/" + filePath
                    val virtualFile = LocalFileSystem.getInstance().findFileByPath(realPath)
                    if (virtualFile != null) {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    }
                }
            }
        }
    }

    private fun getLabelTextByStatus(): String {
        var text = task.step
        task.codeFileLinks.forEach { link ->
            text = text.replace(
                "[${link.displayText}](${link.filePath})",
                "<a href='${link.filePath}'>${link.displayText}</a>"
            )
        }

        return when (task.status) {
            TaskStatus.COMPLETED -> "<strike>$text</strike>"
            TaskStatus.FAILED -> "<span style='color:red'>$text</span>"
            TaskStatus.IN_PROGRESS -> "<span style='color:blue;font-style:italic'>$text</span>"
            TaskStatus.TODO -> text
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