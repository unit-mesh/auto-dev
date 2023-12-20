package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.temporary.getElementToAction

class CodeCompleteChatAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val document = e.getData(CommonDataKeys.EDITOR)?.document
        val caretModel = e.getData(CommonDataKeys.EDITOR)?.caretModel

        var prefixText = caretModel?.currentCaret?.selectedText ?: ""

        val file = e.getData(CommonDataKeys.PSI_FILE)

        val lineEndOffset = document?.getLineEndOffset(document.getLineNumber(caretModel?.offset ?: 0)) ?: 0
        if (prefixText.isEmpty()) {
            prefixText = document?.text?.substring(0, lineEndOffset) ?: ""
        }
        val suffixText = document?.text?.substring(lineEndOffset) ?: ""

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        ApplicationManager.getApplication().runReadAction {
            try {
                val prompter = ContextPrompter.prompter(file?.language?.displayName ?: "")

                val element = getElementToAction(project, editor)
                prompter.initContext(
                    ChatActionType.CODE_COMPLETE, prefixText, file, project, caretModel?.offset ?: 0, element
                )

                val actionType = ChatActionType.CODE_COMPLETE
                val chatCodingService = ChatCodingService(actionType, project)
                val toolWindowManager =
                    ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id) ?: run {
                        logger<ChatCodingService>().warn("Tool window not found")
                        return@runReadAction
                    }

                val contentManager = toolWindowManager.contentManager
                val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager.disposable)

                val content =
                    contentManager.factory.createContent(contentPanel, chatCodingService.getLabel(), false)
                contentManager.removeAllContents(true)
                contentManager.addContent(content)

                toolWindowManager.activate {
                    val chatContext = ChatContext(
                        null, prefixText, suffixText
                    )
                    chatCodingService.handlePromptAndResponse(contentPanel, prompter, chatContext)
                }
            } catch (ignore: IndexNotReadyException) {
                return@runReadAction
            }
        }
    }
}