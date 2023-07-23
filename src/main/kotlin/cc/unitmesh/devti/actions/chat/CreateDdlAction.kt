package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatActionType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE

class CreateDdlAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.CREATE_DDL
    }

    override fun actionPerformed(event: AnActionEvent) {
        val fileType = event.getData(PSI_FILE)?.fileType?.name
        if (fileType != "SQL") {
            return
        }

        super.actionPerformed(event)
    }
}
