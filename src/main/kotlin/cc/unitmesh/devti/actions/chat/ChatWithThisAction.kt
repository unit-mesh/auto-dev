package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.temporary.getElementToAction

class ChatWithThisAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType = ChatActionType.CHAT

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        var prefixText = caretModel?.currentCaret?.selectedText ?: ""

        if (prefixText.isEmpty()) {
            val element = getElementToAction(project, editor)
            if (element != null) {
                selectElement(element, editor)
                prefixText = element.text
            }
        }

        val language = event.getData(CommonDataKeys.PSI_FILE)?.language?.displayName ?: ""

        sendToChatWindow(project, getActionType()) { contentPanel, _ ->
            contentPanel.setInput(
                trimMargin = """
                ```$language
                $prefixText
                ```"""
            )
        }
    }
}

