package cc.unitmesh.devti.shadow

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.planner.MarkdownLanguageField
import cc.unitmesh.devti.inline.AutoDevLineBorder
import cc.unitmesh.devti.sketch.AutoSketchMode
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI.Borders.emptyLeft
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JPanel

class IssueInputPanel(
    private val project: Project,
    private val placeholder: String,
    private val onSubmit: (String) -> Unit,
    private val onCancel: () -> Unit
) : JPanel(BorderLayout()) {
    private val textArea: MarkdownLanguageField = MarkdownLanguageField(project, "", placeholder, "issue.md")
        .apply {
            border = AutoDevLineBorder(JBColor.namedColor("Focus.borderColor", JBColor.BLUE), 1, true, 4)
        }

    init {
        val buttonsPanel = createActionButtons()

        add(textArea, BorderLayout.CENTER)
        add(buttonsPanel, BorderLayout.SOUTH)

        background = JBColor.WHITE
        setBorder(BorderFactory.createEmptyBorder())
    }

    private fun createActionButtons(): JPanel {
        val buttonsPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.RIGHT, 8, 0)
            isOpaque = false
            border = null
        }

        val submitButton = JButton("Submit").apply {
            addActionListener {
                if (text.isNotBlank()) {
                    handlingExecute(text)
                    onSubmit(text)
                } else {
                    AutoDevNotifications.notify(project, "Input cannot be empty")
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
        return buttonsPanel
    }

    fun handlingExecute(newPlan: String) {
        AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
            AutoSketchMode.getInstance(project).enableComposerMode()
            ui.sendInput(newPlan)
        }
    }

    fun getText(): String = textArea.text

    fun setText(text: String) {
        if (text.isNotBlank()) {
            textArea.text = text
        } else {
            textArea.text = placeholder
        }
    }

    fun requestTextAreaFocus() {
        textArea.requestFocus()
    }
}
