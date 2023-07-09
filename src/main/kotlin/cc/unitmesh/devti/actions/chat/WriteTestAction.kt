package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatBotActionType

class WriteTestAction : ChatBaseAction() {
    override fun getActionType(): ChatBotActionType {
        return ChatBotActionType.WRITE_TEST
    }

}
