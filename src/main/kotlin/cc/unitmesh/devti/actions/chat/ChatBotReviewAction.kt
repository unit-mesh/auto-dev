package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatActionType

class ChatBotReviewAction : ChatBaseAction() {

    override fun getActionType(): ChatActionType {
        return ChatActionType.REVIEW
    }
}
