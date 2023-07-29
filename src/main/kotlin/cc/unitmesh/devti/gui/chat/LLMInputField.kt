package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class LLMInputField(
    project: Project,
    chatCodingService: ChatCodingService,
    component: ChatCodingComponent,
    private val listeners: List<DocumentListener>,
) : EditorTextField(project, FileTypes.PLAIN_TEXT) {
    init {
        isOneLineMode = false
        updatePlaceholderText()
        addSettingsProvider {
            it.colorsScheme.lineSpacing = 1.0f
            it.settings.isUseSoftWraps = true
            it.settings.isPaintSoftWraps = false
            it.isEmbeddedIntoDialogWrapper = true
            it.contentComponent.setOpaque(false)
        }

        DumbAwareAction.create { e: AnActionEvent? ->
            object : AnAction() {
                override fun actionPerformed(e1: AnActionEvent) {
                    val editor = this@LLMInputField.editor ?: return

                    CommandProcessor.getInstance().executeCommand(project, {
                        val eol = "\n"
                        val caretOffset = editor.caretModel.offset
                        editor.document.insertString(caretOffset, eol)
                        editor.caretModel.moveToOffset(caretOffset + eol.length)
                        EditorModificationUtil.scrollToCaret(editor)
                    }, null, null)
                }
            }
        }.registerCustomShortcutSet(
            CustomShortcutSet(
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), null),
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), null)
            ), this
        )
    }

    private fun updatePlaceholderText() {
        setPlaceholder(AutoDevBundle.message("chat.label.initial.text"))
        repaint()
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setVerticalScrollbarVisible(true)
        setBorder(JBUI.Borders.empty())
        editor.setShowPlaceholderWhenFocused(true)
        editor.caretModel.moveToOffset(0)
        editor.scrollPane.setBorder(border)
        editor.contentComponent.setOpaque(false)
        return editor
    }

    override fun getBackground(): Color {
        val editor = editor
        if (editor != null) {
            val colorsScheme = editor.colorsScheme
            return colorsScheme.defaultBackground
        }
        return super.getBackground()
    }
}