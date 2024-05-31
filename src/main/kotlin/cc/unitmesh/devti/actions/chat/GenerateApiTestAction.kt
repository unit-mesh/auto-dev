package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.template.context.TemplateContext

class GenerateApiTestAction : ChatBaseAction() {
    init {
        val presentation = getTemplatePresentation()
        presentation.text = AutoDevBundle.message("settings.autodev.rightClick.genApiTest")
    }

    override fun getActionType(): ChatActionType = ChatActionType.GENERATE_TEST_DATA
}
