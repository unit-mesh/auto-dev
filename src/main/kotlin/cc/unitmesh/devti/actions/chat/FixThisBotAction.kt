package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatBotActionType

class FixThisBotAction : ChatBaseAction() {
    override fun getActionType(): ChatBotActionType {
        return ChatBotActionType.FixIssue
    }
}
