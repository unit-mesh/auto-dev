package cc.unitmesh.devti.vcs

import cc.unitmesh.devti.actions.chat.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatBotActionType

class CommitMessageSuggestionAction: ChatBaseAction() {
    override fun getActionType(): ChatBotActionType {
        return ChatBotActionType.GEN_COMMIT_MESSAGE
    }
}
