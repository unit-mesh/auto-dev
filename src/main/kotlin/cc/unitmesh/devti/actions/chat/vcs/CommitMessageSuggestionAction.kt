package cc.unitmesh.devti.actions.chat.vcs

import cc.unitmesh.devti.actions.chat.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType

class CommitMessageSuggestionAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.GEN_COMMIT_MESSAGE
    }
}
