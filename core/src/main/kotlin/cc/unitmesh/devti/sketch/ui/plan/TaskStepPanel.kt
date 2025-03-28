package cc.unitmesh.devti.sketch.ui.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.observer.plan.AgentPlanStep
import cc.unitmesh.devti.observer.plan.TaskStatus
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.text.AttributeSet
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
    private var statusLabel: JLabel? = null
    private val MAX_TEXT_LENGTH = 100 // Maximum characters to display before truncating

    init {
        layout = BorderLayout()
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(0xE5E5E5, 0x323232)),
            JBUI.Borders.empty(3, 2)  // Reduced padding for more compact view
        )

        // Left panel for status icon with reduced spacing
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)).apply {
            isOpaque = false
        }

        val statusIcon = createStatusIcon()
        leftPanel.add(statusIcon)

        // Center panel for task description with links
        taskLabel = createTaskTextPane()
        doc = taskLabel.styledDocument
        updateTaskLabel()

        val centerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(taskLabel, BorderLayout.CENTER)
        }

        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
            isOpaque = false
        }

        statusLabel = JLabel(getStatusText(task.status)).apply {
            foreground = getStatusColor(task.status)
            font = font.deriveFont(Font.PLAIN, 9f)
            border = JBUI.Borders.empty(1, 3)
        }
        rightPanel.add(statusLabel)

        if (task.status == TaskStatus.TODO || task.status == TaskStatus.FAILED) {
            val executeButton = JButton(AutoDevIcons.Run).apply {
                preferredSize = Dimension(20, 20)
                margin = JBUI.emptyInsets()
                isBorderPainted = false
                isContentAreaFilled = false
                toolTipText = "Execute this step"
                addActionListener { executeTask() }
            }
            rightPanel.add(executeButton)
        }

        if (task.status == TaskStatus.FAILED) {
            val retryButton = JButton("Retry").apply {
                margin = JBUI.insets(0, 2)  // Less margin
                font = font.deriveFont(Font.PLAIN, 9f)  // Smaller font size
                addActionListener { executeTask() }
            }
            rightPanel.add(retryButton)
        }

        add(leftPanel, BorderLayout.WEST)
        add(centerPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        setupContextMenu()

        // Set preferred size for compact view
        preferredSize = Dimension(preferredSize.width, Math.min(preferredSize.height, 28))
    }

    private fun createStatusIcon(): JComponent {
        return when (task.status) {
            TaskStatus.COMPLETED -> JLabel(AutoDevIcons.Checked).apply {
                preferredSize = Dimension(16, 16)
            }

            TaskStatus.FAILED -> JLabel(AutoDevIcons.Error).apply {
                preferredSize = Dimension(16, 16)
            }

            TaskStatus.IN_PROGRESS -> JLabel(AutoDevIcons.Build).apply {
                preferredSize = Dimension(16, 16)
            }

            TaskStatus.TODO -> JCheckBox().apply {
                isSelected = task.completed
                addActionListener {
                    task.completed = isSelected
                    task.updateStatus(if (isSelected) TaskStatus.COMPLETED else TaskStatus.TODO)
                    updateTaskLabel()
                    updateStatusLabel()
                    onStatusChange()
                }
                isBorderPainted = false
                isContentAreaFilled = false
                background = JBUI.CurrentTheme.ToolWindow.background()
                preferredSize = Dimension(16, 16)
            }
        }
    }

    private fun createTaskTextPane(): JTextPane {
        val editorColorsManager = EditorColorsManager.getInstance()
        val currentScheme = editorColorsManager.schemeForCurrentUITheme
        val editorFontName = currentScheme.editorFontName
        val editorFontSize = currentScheme.editorFontSize - 1  // Slightly smaller font

        return JTextPane().apply {
            isEditable = false
            isOpaque = false
            background = JBUI.CurrentTheme.ToolWindow.background()
            border = JBUI.Borders.emptyLeft(3)  // Less left padding
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
                    cursor = Cursor(Cursor.HAND_CURSOR)
                }

                override fun mouseExited(e: MouseEvent) {
                    cursor = Cursor(Cursor.DEFAULT_CURSOR)
                }
            })
        }
    }

    private fun updateTaskLabel() {
        doc.remove(0, doc.length)
        linkMap.clear()

        var text = task.step
        var currentPos = 0

        taskLabel.toolTipText = text
        val needsTruncation = text.length > MAX_TEXT_LENGTH && task.codeFileLinks.isEmpty()
        if (needsTruncation && task.codeFileLinks.isEmpty()) {
            val truncatedText = text.take(MAX_TEXT_LENGTH) + "..."
            doc.insertString(0, truncatedText, getStyleForStatus(task.status))
            return
        }

        // If we have links or the text is short enough, use the normal rendering logic
        task.codeFileLinks.forEach { link ->
            val linkPattern = "[${link.displayText}](${link.filePath})"
            val linkIndex = text.indexOf(linkPattern)

            if (linkIndex >= 0) {
                val beforeLink = text.substring(0, linkIndex)
                doc.insertString(currentPos, beforeLink, getStyleForStatus(task.status))
                currentPos += beforeLink.length

                val linkStyle = SimpleAttributeSet().apply {
                    StyleConstants.setForeground(this, JBColor(0x3366CC, 0x589DF6))
                    StyleConstants.setUnderline(this, true)
                }

                doc.insertString(currentPos, link.displayText, linkStyle)
                linkMap[currentPos..(currentPos + link.displayText.length)] = link.filePath

                currentPos += link.displayText.length
                text = text.substring(linkIndex + linkPattern.length)
            }
        }

        if (text.isNotEmpty()) {
            if (text.length > MAX_TEXT_LENGTH) {
                text = text.take(MAX_TEXT_LENGTH) + "..."
            }
            doc.insertString(currentPos, text, getStyleForStatus(task.status))
        }
    }

    private fun getStyleForStatus(status: TaskStatus): AttributeSet {
        val style = SimpleAttributeSet()

        when (status) {
            TaskStatus.COMPLETED -> {
                StyleConstants.setStrikeThrough(style, true)
                StyleConstants.setForeground(style, JBColor(0x808080, 0x999999))
            }

            TaskStatus.FAILED -> {
                StyleConstants.setForeground(style, JBColor(0xD94F4F, 0xFF6B68))
            }

            TaskStatus.IN_PROGRESS -> {
                StyleConstants.setForeground(style, JBColor(0x3592C4, 0x589DF6))
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
                updateStatusLabel()
                onStatusChange()
                refreshPanel()
            }
            taskPopupMenu.add(menuItem)
        }

        taskLabel.componentPopupMenu = taskPopupMenu
    }

    private fun executeTask() {
        task.updateStatus(TaskStatus.IN_PROGRESS)
        updateTaskLabel()
        updateStatusLabel()
        onStatusChange()

        // Send to AI for execution
        AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
            ui.sendInput(AutoDevBundle.message("sketch.plan.finish.task") + task.step)
        }

        refreshPanel()
    }

    private fun updateStatusLabel() {
        statusLabel?.text = getStatusText(task.status)
        statusLabel?.foreground = getStatusColor(task.status)
    }

    private fun refreshPanel() {
        removeAll()

        // Rebuild the panel components
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            isOpaque = false
            add(createStatusIcon())
        }

        val centerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(taskLabel, BorderLayout.CENTER)
        }

        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
            isOpaque = false
            add(statusLabel)

            if (task.status == TaskStatus.TODO || task.status == TaskStatus.FAILED) {
                val executeButton = JButton(AutoDevIcons.Run).apply {
                    preferredSize = Dimension(24, 24)
                    margin = JBUI.insets(0)
                    isBorderPainted = false
                    isContentAreaFilled = false
                    toolTipText = "Execute this step"
                    addActionListener { executeTask() }
                }
                add(executeButton)
            }

            if (task.status == TaskStatus.FAILED) {
                val retryButton = JButton("Retry").apply {
                    margin = JBUI.insets(0, 3)
                    font = font.deriveFont(Font.PLAIN, 10f)
                    addActionListener { executeTask() }
                }
                add(retryButton)
            }
        }

        add(leftPanel, BorderLayout.WEST)
        add(centerPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        revalidate()
        repaint()
    }

    private fun getStatusText(status: TaskStatus): String {
        return when (status) {
            TaskStatus.COMPLETED -> "Completed"
            TaskStatus.FAILED -> "Failed"
            TaskStatus.IN_PROGRESS -> "In Progress"
            TaskStatus.TODO -> "To Do"
        }
    }

    private fun getStatusColor(status: TaskStatus): JBColor {
        return when (status) {
            TaskStatus.COMPLETED -> JBColor(0x59A869, 0x59A869) // Green
            TaskStatus.FAILED -> JBColor(0xD94F4F, 0xD94F4F) // Red
            TaskStatus.IN_PROGRESS -> JBColor(0x3592C4, 0x3592C4) // Blue
            TaskStatus.TODO -> JBColor(0x808080, 0x808080) // Gray
        }
    }
}
