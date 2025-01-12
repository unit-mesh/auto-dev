package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.message.ChatContext
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.temporary.getElementToAction

class CodeCompleteChatAction : AnAction() {

    init{
        presentationText("settings.autodev.others.codeComplete", templatePresentation)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private val logger = logger<ChatCodingService>()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val document = e.getData(CommonDataKeys.EDITOR)?.document
        val caretModel = e.getData(CommonDataKeys.EDITOR)?.caretModel

        var prefixText = caretModel?.currentCaret?.selectedText ?: ""


        val lineEndOffset = document?.getLineEndOffset(document.getLineNumber(caretModel?.offset ?: 0)) ?: 0
        if (prefixText.isEmpty()) {
            prefixText = document?.text?.substring(0, lineEndOffset) ?: ""
        }

        val suffixText = document?.text?.substring(lineEndOffset) ?: ""

        ApplicationManager.getApplication().runReadAction {
            try {
                val prompter = ContextPrompter.prompter(file.language.displayName)

                val element = getElementToAction(project, editor)
                prompter.initContext(
                    ChatActionType.CODE_COMPLETE, prefixText, file, project, caretModel?.offset ?: 0, element
                )

                val actionType = ChatActionType.CODE_COMPLETE
                val chatCodingService = ChatCodingService(actionType, project)
                val toolWindowManager = AutoDevToolWindowFactory.getToolWindow(project) ?: run {
                        logger.warn("Tool window not found")
                        return@runReadAction
                    }

                val contentManager = toolWindowManager.contentManager
                val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager.disposable)

                val content =
                    contentManager.factory.createContent(contentPanel, chatCodingService.getLabel(), false)
                contentManager.removeAllContents(true)
                contentManager.addContent(content)

                toolWindowManager.activate {
                    val chatContext = ChatContext(null, prefixText, suffixText)
                    chatCodingService.handlePromptAndResponse(contentPanel, prompter, chatContext, true)
                }
            } catch (ignore: IndexNotReadyException) {
                return@runReadAction
            }
        }
    }
}