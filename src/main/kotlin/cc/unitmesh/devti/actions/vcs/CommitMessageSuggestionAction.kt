package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType

class CommitMessageSuggestionAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.GEN_COMMIT_MESSAGE
    }
}
