package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatBotActionType

class CreateDdlAction : ChatBaseAction() {
    override fun getActionType(): ChatBotActionType {
        return ChatBotActionType.CREATE_DDL
    }


}
