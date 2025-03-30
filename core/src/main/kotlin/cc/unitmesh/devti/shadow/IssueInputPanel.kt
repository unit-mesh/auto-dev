package cc.unitmesh.devti.shadow

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.sketch.AutoSketchMode
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.border.LineBorder

class IssueInputPanel(
    private val project: Project,
    private val title: String,
    private val onSubmit: (String) -> Unit,
    private val onCancel: () -> Unit
) : JPanel(BorderLayout()) {
    private val textArea: JTextArea

    init {
        background = JBUI.CurrentTheme.ToolWindow.background()

        val mainPanel = JPanel(BorderLayout(8, 8)).apply {
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.empty(8),
                BorderFactory.createCompoundBorder(LineBorder(Gray._230, 1, true),
                    JBUI.Borders.empty(8)
                )
            )
            background = JBUI.CurrentTheme.ToolWindow.background()
        }

        val headerLabel = JLabel(title).apply {
            font = font.deriveFont(font.size + 2f)
            border = JBUI.Borders.emptyBottom(10)
        }

        textArea = JTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            border = null
            font = Font("Arial", Font.PLAIN, 16)
            rows = 10
        }

        val scrollPane = JBScrollPane(textArea).apply {
            border = null
            preferredSize = Dimension(400, 300)
        }

        val controlsPanel = JPanel(BorderLayout(10, 0)).apply {
            border = JBUI.Borders.emptyTop(10)
            background = JBUI.CurrentTheme.ToolWindow.background()
        }

        val buttonsPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.RIGHT, 8, 0)
            isOpaque = false
        }

        val submitButton = JButton("Submit").apply {
            addActionListener {
                val text = textArea.text
                if (text.isNotBlank()) {
                    handlingExecute(text)
                    onSubmit(text)
                }
            }

            preferredSize = Dimension(90, 32)
        }

        val cancelButton = JButton("Cancel").apply {
            addActionListener {
                onCancel()
            }

            preferredSize = Dimension(90, 32)
        }

        buttonsPanel.add(submitButton)
        buttonsPanel.add(Box.createHorizontalStrut(5))
        buttonsPanel.add(cancelButton)

        controlsPanel.add(buttonsPanel, BorderLayout.EAST)

        mainPanel.add(headerLabel, BorderLayout.NORTH)
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

    fun getText(): String = textArea.text

    fun setText(text: String) {
        textArea.text = text
    }

    fun requestTextAreaFocus() {
        textArea.requestFocus()
    }
}
