package cc.unitmesh.devti.gui.planner

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.sketch.AutoSketchMode
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JPanel

class PlanIssueInputViewPanel(
    private val project: Project,
    private val onSubmit: (String) -> Unit,
    private val onCancel: () -> Unit
) : JPanel(BorderLayout()) {
    private var textArea: MarkdownLanguageField? = null

    init {
        textArea = MarkdownLanguageField(project, "", AutoDevBundle.message("sketch.issue.input.placeholder"), "issue.md")

        val buttonPanel = JPanel(BorderLayout()).apply {
            background = textArea?.getEditor(true)?.backgroundColor
        }

        val buttonsBox = Box.createHorizontalBox().apply {
            add(JButton(AutoDevBundle.message("sketch.issue.input.submit")).apply {
                addActionListener {
                    val text = textArea?.text ?: ""
                    if (text.isNotBlank()) {
                        handlingExecute(text)
                        onSubmit(text)
                    } else {
                        AutoDevNotifications.notify(project, AutoDevBundle.message("chat.input.tips"))
                    }
                }
            })
            add(Box.createHorizontalStrut(8))
            add(JButton(AutoDevBundle.message("sketch.issue.input.cancel")).apply {
                addActionListener {
                    onCancel()
                }
            })
        }
        buttonPanel.add(buttonsBox, BorderLayout.EAST)
        buttonPanel.border = JBUI.Borders.empty(4)

        add(JBScrollPane(textArea), BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
        setBorder(BorderFactory.createEmptyBorder())
    }

    fun handlingExecute(newPlan: String) {
        AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
            AutoSketchMode.Companion.getInstance(project).enableComposerMode()
            ui.sendInput(newPlan)
        }
    }

    fun getText(): String = textArea?.text ?: ""

    fun setText(text: String) {
        textArea?.text = text
    }

    fun requestTextAreaFocus() {
        textArea?.requestFocus()
    }

    fun dispose() {
        textArea = null
    }
}