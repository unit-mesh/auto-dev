package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatBotActionType
import com.intellij.openapi.actionSystem.AnActionEvent

class ChatBotExplainAction : ChatBaseAction() {
    override fun actionPerformed(event: AnActionEvent) {
        super.actionPerformed(event)
    }

    override fun getActionType(): ChatBotActionType {
        return ChatBotActionType.EXPLAIN
    }
}