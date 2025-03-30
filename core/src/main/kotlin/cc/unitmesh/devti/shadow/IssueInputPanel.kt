package cc.unitmesh.devti.shadow

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.sketch.AutoSketchMode
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBUI
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
    private val textArea: EditorTextField
    private var isPlaceholderVisible = true

    init {
        textArea = createEditor(project).apply {
            setPlaceholder(placeholder)
            setShowPlaceholderWhenFocused(true)
        }

        val buttonsPanel = createActionButtons()

        add(textArea, BorderLayout.CENTER)
        add(buttonsPanel, BorderLayout.SOUTH)

        background = textArea.editor?.component?.background ?: JBUI.CurrentTheme.ToolWindow.background()
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
        return buttonsPanel
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
            isPlaceholderVisible = false
        } else {
            textArea.text = placeholder
            isPlaceholderVisible = true
        }
    }

    fun requestTextAreaFocus() {
        textArea.requestFocus()
    }
}

private fun createEditor(project: Project): EditorTextField {
    val features: MutableSet<EditorCustomization?> = HashSet<EditorCustomization?>()

    features.add(SoftWrapsEditorCustomization.ENABLED)
    features.add(AdditionalPageAtBottomEditorCustomization.DISABLED)
    ContainerUtil.addIfNotNull<EditorCustomization?>(
        features,
        SpellCheckingEditorCustomizationProvider.getInstance().enabledCustomization
    )

    val editorField =
        EditorTextFieldProvider.getInstance().getEditorField(FileTypes.PLAIN_TEXT.language, project, features)

    editorField.setFontInheritedFromLAF(false)
    return editorField
}

