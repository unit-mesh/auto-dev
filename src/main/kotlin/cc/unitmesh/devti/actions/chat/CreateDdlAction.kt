package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatBotActionType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE

class CreateDdlAction : ChatBaseAction() {
    override fun getActionType(): ChatBotActionType {
        return ChatBotActionType.CREATE_DDL
    }

    override fun actionPerformed(event: AnActionEvent) {
        val fileType = event.getData(PSI_FILE)?.fileType?.name
        if (fileType != "SQL") {
            return
        }

        super.actionPerformed(event)
    }
}
