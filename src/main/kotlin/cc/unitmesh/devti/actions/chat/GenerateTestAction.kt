package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType

class GenerateTestAction : ChatBaseAction() {

    init{
        val presentation = getTemplatePresentation()
        presentation.text = AutoDevBundle.message("settings.autodev.rightClick.writeTest")
    }
    override fun getActionType(): ChatActionType = ChatActionType.GENERATE_TEST
}
