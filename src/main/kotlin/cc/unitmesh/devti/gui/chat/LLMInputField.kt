package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class LLMInputField(
    project: Project,
    chatCodingService: ChatCodingService,
    component: ChatCodingComponent,
    private val listeners: List<DocumentListener>,
) : EditorTextField(project, FileTypes.PLAIN_TEXT) {
    init {
        isOneLineMode = false
        addSettingsProvider {
            it.colorsScheme.lineSpacing = 1.0f
            it.settings.isUseSoftWraps = true
            it.settings.isPaintSoftWraps = false
            it.isEmbeddedIntoDialogWrapper = true
            it.contentComponent.setOpaque(false)
        }

        val listener: (ActionEvent) -> Unit = {
            val prompt = text
            text = ""
            val context = ChatContext(null, "", "")

            chatCodingService.actionType = ChatActionType.REFACTOR
            chatCodingService.handlePromptAndResponse(component, object : ContextPrompter() {
                override fun displayPrompt() = prompt
                override fun requestPrompt() = prompt
            }, context)
        }

        this.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    listener.invoke(ActionEvent(e.source, e.id, e.paramString()))
                }
            }
        })
    }
}