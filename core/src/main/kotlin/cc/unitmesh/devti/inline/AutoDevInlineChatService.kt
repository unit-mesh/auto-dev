package cc.unitmesh.devti.inline

import cc.unitmesh.devti.prompting.SimpleDevinPrompter
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.wm.IdeFocusManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

@Service(Service.Level.APP)
class AutoDevInlineChatService : Disposable, SimpleDevinPrompter() {
    private val allChats: ConcurrentHashMap<String, AutoDevInlineChatPanel> = ConcurrentHashMap()
    override val templateRender = TemplateRender(GENIUS_CODE)
    override val template = templateRender.getTemplate("inline-chat.devin")

    fun showInlineChat(editor: Editor) {
        var canShowInlineChat = true
        if (allChats.containsKey(editor.fileUrl())) {
            val chatPanel: AutoDevInlineChatPanel = this.allChats[editor.fileUrl()]!!
            canShowInlineChat = chatPanel.inlay?.offset != editor.caretModel.primaryCaret.offset
            closeInlineChat(editor)
        }

        if (canShowInlineChat) {
            if (editor.component is AutoDevInlineChatPanel) return

            val panel = AutoDevInlineChatPanel(editor)
            editor.contentComponent.add(panel)
            panel.setInlineContainer(editor.contentComponent)

            val offset = if (editor.selectionModel.hasSelection()) {
                editor.selectionModel.selectionStart
            } else {
                editor.caretModel.primaryCaret.offset
            }
            panel.createInlay(offset)

            IdeFocusManager.getInstance(editor.project).requestFocus(panel.inputPanel.getInputComponent(), true)
            allChats[editor.fileUrl()] = panel
        }
    }

    override fun dispose() {
        allChats.values.forEach {
            closeInlineChat(it.editor)
        }

        allChats.clear()
    }

    fun closeInlineChat(editor: Editor) {
        val chatPanel = this.allChats[editor.fileUrl()] ?: return

        chatPanel.dispose()

        editor.contentComponent.remove(chatPanel)
        editor.contentComponent.revalidate()
        editor.contentComponent.repaint()
        allChats.remove(editor.fileUrl())
    }

    companion object {
        fun getInstance(): AutoDevInlineChatService {
            return ApplicationManager.getApplication().getService(AutoDevInlineChatService::class.java)
        }
    }
}

fun Editor.fileUrl(): String {
    return FileDocumentManager.getInstance().getFile(this.document)?.url ?: ""
}