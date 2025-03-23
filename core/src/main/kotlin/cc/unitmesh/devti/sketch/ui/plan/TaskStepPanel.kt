// filepath: /Volumes/source/ai/autocrud/core/src/main/kotlin/cc/unitmesh/devti/sketch/ui/plan/TaskStepPanel.kt
package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.plan.AgentPlanStep
import cc.unitmesh.devti.observer.plan.TaskStatus
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
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
            TaskStatus.COMPLETED -> JLabel(AutoDevIcons.Checked)
            TaskStatus.FAILED -> JLabel(AutoDevIcons.Error)
            TaskStatus.IN_PROGRESS -> JLabel(AutoDevIcons.Build)
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
        return JButton(AutoDevIcons.Run).apply {
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
        
        val backgroundColor = UIUtil.getPanelBackground()
        val backgroundColorHex = ColorUtil.toHex(backgroundColor)
        
        // Get the foreground color that matches the current theme
        val foregroundColor = UIUtil.getLabelForeground()
        val foregroundColorHex = ColorUtil.toHex(foregroundColor)
        
        val editorColorsManager = EditorColorsManager.getInstance()
        val currentScheme = editorColorsManager.schemeForCurrentUITheme
        val editorFontName = currentScheme.editorFontName
        val editorFontSize = currentScheme.editorFontSize

        return JEditorPane().apply {
            contentType = "text/html"

            val editorKit = HTMLEditorKit()
            val styleSheet = StyleSheet()
            styleSheet.addRule("body { font-family: '$editorFontName'; font-size: ${editorFontSize}pt; background-color: #$backgroundColorHex; color: #$foregroundColorHex; }")
            styleSheet.addRule("a { color: #3366CC; text-decoration: underline; }")
            styleSheet.addRule("a:hover { color: #3366CC; }")
            editorKit.styleSheet = styleSheet

            this.editorKit = editorKit

            val document = this.document as HTMLDocument
            document.putProperty("IgnoreCharsetDirective", true)
            project.basePath?.let {
                val url: String? = LocalFileSystem.getInstance().findFileByPath(it)?.url
                document.putProperty("Base", url)
            }

            border = JBUI.Borders.emptyLeft(5)
            isEditable = false
            background = backgroundColor

            text = "<html><body>$labelText</body></html>"

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
