package cc.unitmesh.devti.shadow

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.sketch.AutoSketchMode
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*
import javax.swing.border.LineBorder

class IssueInputPanel(
    private val project: Project,
    private val title: String,
    private val onSubmit: (String) -> Unit,
    private val onCancel: () -> Unit
) : JPanel(BorderLayout()) {
    private val textArea: JTextArea
    private var isPlaceholderVisible = true

    init {
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1, 0, 1, 0)

        val mainPanel = JPanel(BorderLayout(8, 8)).apply {
            background = JBUI.CurrentTheme.ToolWindow.background()
        }

        textArea = JTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            font = Font("Arial", Font.PLAIN, 16)
            rows = 10
            text = title
            foreground = JBColor.GRAY

            addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                    if (isPlaceholderVisible) {
                        text = ""
                        foreground = UIManager.getColor("TextArea.foreground")
                        isPlaceholderVisible = false
                    }
                }

                override fun focusLost(e: FocusEvent?) {
                    if (text.isEmpty()) {
                        text = title
                        foreground = JBColor.GRAY
                        isPlaceholderVisible = true
                    }
                }
            })
        }

        val scrollPane = JBScrollPane(textArea).apply {
            border = null
            preferredSize = Dimension(preferredSize.width, 300)
        }

        val controlsPanel = JPanel(BorderLayout(10, 0)).apply {
            background = JBUI.CurrentTheme.ToolWindow.background()
        }

        val buttonsPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.RIGHT, 8, 0)
            background = JBUI.CurrentTheme.ToolWindow.background()
            isOpaque = false
        }

        val submitButton = JButton("Submit").apply {
            addActionListener {
                val text = if (isPlaceholderVisible) "" else textArea.text
                if (text.isNotBlank()) {
                    handlingExecute(text)
                    onSubmit(text)
                }
            }
        }

        val cancelButton = JButton("Cancel").apply {
            addActionListener {
                onCancel()
            }
        }

        buttonsPanel.add(submitButton)
        buttonsPanel.add(Box.createHorizontalStrut(4))
        buttonsPanel.add(cancelButton)

        controlsPanel.add(buttonsPanel, BorderLayout.EAST)

        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(controlsPanel, BorderLayout.SOUTH)

        add(mainPanel, BorderLayout.CENTER)
    }

    fun handlingExecute(newPlan: String) {
        AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
            AutoSketchMode.getInstance(project).enableComposerMode()
            ui.sendInput(newPlan)
        }
    }

    fun getText(): String = if (isPlaceholderVisible) "" else textArea.text

    fun setText(text: String) {
        if (text.isNotBlank()) {
            textArea.text = text
            textArea.foreground = UIManager.getColor("TextArea.foreground")
            isPlaceholderVisible = false
        } else {
            textArea.text = title
            textArea.foreground = JBColor.GRAY
            isPlaceholderVisible = true
        }
    }

    fun requestTextAreaFocus() {
        textArea.requestFocus()
    }
}
