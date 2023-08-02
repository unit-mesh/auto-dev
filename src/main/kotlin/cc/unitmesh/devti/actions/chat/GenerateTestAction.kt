package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatActionType

class GenerateTestAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.GENERATE_TEST
    }
}
