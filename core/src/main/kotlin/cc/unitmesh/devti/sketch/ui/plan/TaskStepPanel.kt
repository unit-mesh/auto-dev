package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.plan.AgentPlanStep
import cc.unitmesh.devti.observer.plan.TaskStatus
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTextPane
import javax.swing.text.AttributeSet
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.MutableAttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

/**
 * Task Panel UI Component responsible for rendering and handling interactions for a single task
 */
class TaskStepPanel(
    private val project: Project,
    private val task: AgentPlanStep,
    private val onStatusChange: () -> Unit
) : JBPanel<TaskStepPanel>() {
    private val taskLabel: JTextPane
    private val doc: StyledDocument
    
    private val linkMap = mutableMapOf<IntRange, String>()

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = JBUI.Borders.empty(4, 16, 4, 0)
        background = JBUI.CurrentTheme.ToolWindow.background()
        
        taskLabel = JTextPane()
        doc = taskLabel.styledDocument
        
        configureTaskLabel()
        updateTaskLabel()

        val statusIcon = createStatusIcon()
        add(statusIcon)

        if (task.status == TaskStatus.TODO) {
            add(createExecuteButton())
        }

        add(taskLabel)
        setupContextMenu()
    }

    private fun configureTaskLabel() {
        val editorColorsManager = EditorColorsManager.getInstance()
        val currentScheme = editorColorsManager.schemeForCurrentUITheme
        val editorFontName = currentScheme.editorFontName
        val editorFontSize = currentScheme.editorFontSize

        taskLabel.apply {
            isEditable = false
            isOpaque = false
            background = JBUI.CurrentTheme.ToolWindow.background()
            border = JBUI.Borders.emptyLeft(5)
            font = Font(editorFontName, Font.PLAIN, editorFontSize)
            foreground = UIUtil.getLabelForeground()
            
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val offset = viewToModel2D(e.point)
                    for ((range, filePath) in linkMap) {
                        if (offset in range) {
                            val realPath = project.basePath + "/" + filePath
                            val virtualFile = LocalFileSystem.getInstance().findFileByPath(realPath)
                            if (virtualFile != null) {
                                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                            }
                            break
                        }
                    }
                }
                
                override fun mouseEntered(e: MouseEvent) {
                    setCursor(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR))
                }
                
                override fun mouseExited(e: MouseEvent) {
                    setCursor(java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR))
                }
            })
        }
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

    private fun createExecuteButton(): JComponent {
        val executeAction = object : AnAction(AutoDevIcons.Run) {
            override fun actionPerformed(e: AnActionEvent) {
                AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                    ui.sendInput(AutoDevBundle.message("sketch.plan.finish.task") + task.step)
                }
            }
        }
        
        val presentation = Presentation().apply {
            icon = AutoDevIcons.Run
            text = ""
            description = AutoDevBundle.message("sketch.plan.execute.tooltip", "Execute")
        }
        
        return ActionButton(executeAction, presentation, "TaskStepPanelExecuteAction", Dimension(22, 22)).apply {
            background = JBUI.CurrentTheme.ToolWindow.background()
        }
    }

    private fun updateTaskLabel() {
        doc.remove(0, doc.length)
        linkMap.clear()
        
        var text = task.step
        var currentPos = 0
        
        // Process each code file link
        task.codeFileLinks.forEach { link ->
            val linkPattern = "[${link.displayText}](${link.filePath})"
            val linkIndex = text.indexOf(linkPattern)
            
            if (linkIndex >= 0) {
                val beforeLink = text.substring(0, linkIndex)
                doc.insertString(currentPos, beforeLink, getStyleForStatus(task.status))
                currentPos += beforeLink.length
                
                val linkStyle = SimpleAttributeSet().apply {
                    StyleConstants.setForeground(this, Color(0x33, 0x66, 0xCC))
                    StyleConstants.setUnderline(this, true)
                }
                
                doc.insertString(currentPos, link.displayText, linkStyle)
                linkMap[currentPos..(currentPos + link.displayText.length)] = link.filePath
                
                currentPos += link.displayText.length
                text = text.substring(linkIndex + linkPattern.length)
            }
        }
        
        if (text.isNotEmpty()) {
            doc.insertString(currentPos, text, getStyleForStatus(task.status))
        }
    }
    
    private fun getStyleForStatus(status: TaskStatus): AttributeSet {
        val style = SimpleAttributeSet()
        
        when (status) {
            TaskStatus.COMPLETED -> {
                StyleConstants.setStrikeThrough(style, true)
            }
            TaskStatus.FAILED -> {
                StyleConstants.setForeground(style, JBColor.RED)
            }
            TaskStatus.IN_PROGRESS -> {
                StyleConstants.setForeground(style, JBColor.BLUE)
                StyleConstants.setItalic(style, true)
            }
            TaskStatus.TODO -> {
                // Default styling
            }
        }
        
        return style
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

