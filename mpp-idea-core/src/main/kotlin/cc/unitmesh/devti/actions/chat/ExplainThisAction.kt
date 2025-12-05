package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.actions.chat.base.ChatCheckForUpdateAction
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.presentationText

class ExplainThisAction() : ChatCheckForUpdateAction() {
    init{
        presentationText("settings.autodev.rightClick.explain", templatePresentation)
    }

    override fun getActionType(): ChatActionType = ChatActionType.EXPLAIN
}
