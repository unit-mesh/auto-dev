package cc.unitmesh.devti.inline

import cc.unitmesh.devti.custom.compile.VariableTemplateCompiler
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import cc.unitmesh.devti.provider.devins.LanguagePromptProcessor
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.temporary.getElementToAction
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

@Service(Service.Level.APP)
class AutoDevInlineChatService : Disposable {
    private val allChats: ConcurrentHashMap<String, AutoDevInlineChatPanel> = ConcurrentHashMap()

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

    val templateRender = TemplateRender(GENIUS_CODE)
    private val template = templateRender.getTemplate("inline-chat.vm")

    fun prompt(project: Project, userInput: String, editor: Editor): String {
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        val psiFile = runReadAction {
            return@runReadAction file?.let { file ->
                return@let PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            }
        }

        val element = getElementToAction(project, editor)
        val creationContext =
            ChatCreationContext(ChatOrigin.Intention, ChatActionType.SKETCH, psiFile, listOf(), element = element)
        val contextItems: List<ChatContextItem> = runBlocking {
            return@runBlocking ChatContextProvider.collectChatContextList(project, creationContext)
        }
        val frameworkContext = contextItems.joinToString("\n") { it.text }

        val variableCompile = VariableTemplateCompiler.create(project)
        if (variableCompile == null) {
            templateRender.addVariable("input", userInput)
            templateRender.addVariable("frameworkContext", frameworkContext)
            return templateRender.renderTemplate(template)
        }

        val finalPrompt: String
        val postProcessors = LanguagePromptProcessor.instance("DevIn").firstOrNull()
        if (postProcessors != null) {
            val compileTemplate = postProcessors.compile(project, template)
            variableCompile.set("input", userInput)
            variableCompile.set("frameworkContext", frameworkContext)
            finalPrompt = variableCompile.compile(compileTemplate)
        } else {
            variableCompile.set("input", userInput)
            variableCompile.set("frameworkContext", frameworkContext)
            finalPrompt = variableCompile.compile(userInput)
        }

        return finalPrompt
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