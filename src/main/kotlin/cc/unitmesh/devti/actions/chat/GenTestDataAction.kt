package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType

class GenTestDataAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.GENERATE_TEST_DATA
}
