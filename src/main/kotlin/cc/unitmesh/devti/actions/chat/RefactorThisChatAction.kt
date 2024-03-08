package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

class RefactorThisChatAction : ChatBaseAction() {

    override fun getActionType(): ChatActionType = ChatActionType.REFACTOR

    override fun getReplaceableAction(event: AnActionEvent): (response: String) -> Unit {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val project = event.getRequiredData(CommonDataKeys.PROJECT)
        val document = editor.document

        val primaryCaret = editor.caretModel.primaryCaret;
        val start = primaryCaret.selectionStart;
        val end = primaryCaret.selectionEnd

        return { response ->
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(start, end, response)
            }
        }
    }
}
