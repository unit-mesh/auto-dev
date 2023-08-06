package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.getElementToAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import kotlinx.coroutines.launch

class ExplainBusinessAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.EXPLAIN_BUSINESS
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE)

        val actionType = getActionType()
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val elementToChat = getElementToAction(project, editor) ?: return
        val elementText = elementToChat.text
        val creationContext = ChatCreationContext(ChatOrigin.ChatAction, actionType, file, listOf(), elementToChat)

        val lang = file?.language?.displayName ?: ""
        val instruction = actionType.instruction(lang)

        LLMCoroutineScopeService.scope(project).launch {
            var chatContext = ""

            val contextItems = ChatContextProvider.collectChatContextList(project, creationContext)
            contextItems.forEach {
                chatContext += it.text + "\n" + elementText + "\n"
            }

            if (chatContext.isEmpty()) {
                chatContext = elementText
            }

            println(instruction)
        }
    }
}

