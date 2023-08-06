package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.getElementToAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.toolwindow.sendToChat
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ExplainBusinessAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.EXPLAIN_BUSINESS
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        val file = event.getData(CommonDataKeys.PSI_FILE)

        val element = event.getData(CommonDataKeys.PSI_ELEMENT)
        val actionType = getActionType()
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val elementToChat = getElementToAction(project, editor) ?: return

        val prompter = ContextPrompter.prompter(file?.language?.displayName ?: "")
        prompter.initContext(actionType, elementToChat.text, file, project, caretModel?.offset ?: 0, elementToChat)

        sendToChat(project, actionType, prompter)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

