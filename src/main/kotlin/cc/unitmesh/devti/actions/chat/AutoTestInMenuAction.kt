package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatActionType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class AutoTestInMenuAction : AnAction("Generate Test") {
    fun getActionType(): ChatActionType = ChatActionType.GENERATE_TEST
    override fun actionPerformed(e: AnActionEvent) {

    }
}