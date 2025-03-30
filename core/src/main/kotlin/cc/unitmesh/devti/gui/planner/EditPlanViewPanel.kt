package cc.unitmesh.devti.gui.planner

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JPanel

class EditPlanViewPanel(
    private val project: Project,
    private val content: String,
    private val onSave: (String) -> Unit,
    private val onCancel: () -> Unit
) : JPanel(BorderLayout()) {
    private var markdownEditor: MarkdownLanguageField? = null

    init {
        markdownEditor = MarkdownLanguageField(project, content, "Edit your plan here...", "plan.md")

        val buttonPanel = JPanel(BorderLayout()).apply {
            background = markdownEditor?.getEditor(true)?.backgroundColor
        }

        val buttonsBox = Box.createHorizontalBox().apply {
            add(JButton("Save").apply {
                addActionListener {
                    val newContent = markdownEditor?.text ?: ""
                    onSave(newContent)
                }
            })
            add(Box.createHorizontalStrut(8))
            add(JButton("Cancel").apply {
                addActionListener {
                    onCancel()
                }
            })
        }
        buttonPanel.add(buttonsBox, BorderLayout.EAST)
        buttonPanel.border = JBUI.Borders.empty(4)

        add(JBScrollPane(markdownEditor), BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    fun getEditor(): MarkdownLanguageField? = markdownEditor

    fun dispose() {
        markdownEditor = null
    }
} 