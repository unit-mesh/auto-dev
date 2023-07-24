package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatActionType

class WriteTestAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.WRITE_TEST

    }
}
