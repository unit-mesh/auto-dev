package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.temporary.getElementToAction

class CodeCompleteChatAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val document = e.getData(CommonDataKeys.EDITOR)?.document
        val caretModel = e.getData(CommonDataKeys.EDITOR)?.caretModel

        var prefixText = caretModel?.currentCaret?.selectedText ?: ""

        val file = e.getData(CommonDataKeys.PSI_FILE)

        val lineEndOffset = document?.getLineEndOffset(document.getLineNumber(caretModel?.offset ?: 0)) ?: 0
        if (prefixText.isEmpty()) {
            prefixText = document?.text?.substring(0, lineEndOffset) ?: ""
        }
        val suffixText = document?.text?.substring(lineEndOffset) ?: ""

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val prompter = ContextPrompter.prompter(file?.language?.displayName ?: "")

        val element = runReadAction { getElementToAction(project, editor) }
        prompter.initContext(ChatActionType.CODE_COMPLETE, prefixText, file, project, caretModel?.offset ?: 0, element)

        sendToChatPanel(project) { panel, service ->
            val chatContext = ChatContext(
                null,
                prefixText,
                suffixText
            )
            service.handlePromptAndResponse(panel, prompter, chatContext)
        }
    }
}