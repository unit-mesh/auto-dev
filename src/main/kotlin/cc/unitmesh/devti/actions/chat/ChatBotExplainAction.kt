package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType

class ChatBotExplainAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.EXPLAIN
}
