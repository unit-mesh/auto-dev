package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.toolwindow.chatWithSelection
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ChatWithThisAction: ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.CHAT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        val prefixText = caretModel?.currentCaret?.selectedText ?: ""
        val language = event.getData(CommonDataKeys.PSI_FILE)?.language?.displayName ?: ""

        chatWithSelection(project, language, prefixText, getActionType())
    }
}

