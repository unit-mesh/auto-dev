package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class LLMInputField(
    project: Project,
    chatCodingService: ChatCodingService,
    component: ChatCodingComponent,
    private val listeners: List<DocumentListener>,
) : EditorTextField(project, FileTypes.PLAIN_TEXT), Disposable {
    init {
        isOneLineMode = false
        updatePlaceholderText()
        setFontInheritedFromLAF(true)
        addSettingsProvider {
            it.putUserData(IncrementalFindAction.SEARCH_DISABLED, true)
            it.colorsScheme.lineSpacing = 1.0f
            it.settings.isUseSoftWraps = true
            it.settings.isPaintSoftWraps = false
            it.isEmbeddedIntoDialogWrapper = true
            it.contentComponent.setOpaque(false)
        }

        DumbAwareAction.create {
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


        val connect: MessageBusConnection = project.messageBus.connect(this)
        val topic = AnActionListener.TOPIC

        connect.subscribe(topic, object : AnActionListener {
            override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
                if (event.dataContext.getData(CommonDataKeys.EDITOR) === this@LLMInputField.editor && action is EnterAction) {
                    // todo: move this to a service
                    val listener: (ActionEvent) -> Unit = {
                        val prompt = this@LLMInputField.text
                        this@LLMInputField.text = ""

                        val context = ChatContext(null, "", "")
                        chatCodingService.actionType = ChatActionType.REFACTOR
                        chatCodingService.handlePromptAndResponse(component, object : ContextPrompter() {
                            override fun displayPrompt() = prompt
                            override fun requestPrompt() = prompt
                        }, context)
                    }

                    listener.invoke(ActionEvent(this@LLMInputField, 0, ""))
                }
            }
        })
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

    override fun dispose() {
        listeners.forEach { editor?.document?.removeDocumentListener(it) }
    }
}