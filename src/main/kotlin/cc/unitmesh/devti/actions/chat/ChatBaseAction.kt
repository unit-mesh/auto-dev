package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.DevtiFlowToolWindowFactory
import cc.unitmesh.devti.prompting.JavaActionPrompting
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager

abstract class ChatBaseAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        executeAction(event)
    }

    open fun executeAction(event: AnActionEvent) {
        val project = event.project
        val toolWindowManager = ToolWindowManager.getInstance(project!!).getToolWindow(DevtiFlowToolWindowFactory.id)
        val contentManager = toolWindowManager?.contentManager
        val document = event.getData(CommonDataKeys.EDITOR)?.document

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        var prefixText = caretModel?.currentCaret?.selectedText ?: ""

        val lang = event.getData(CommonDataKeys.PSI_FILE)?.language?.displayName ?: ""
        val file = event.getData(CommonDataKeys.PSI_FILE)

        val chatCodingService = ChatCodingService(getActionType())
        val contentPanel = ChatCodingComponent(chatCodingService)
        val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

        contentManager?.removeAllContents(true)
        contentManager?.addContent(content!!)
        toolWindowManager?.activate(null)

        val lineEndOffset = document?.getLineEndOffset(document.getLineNumber(caretModel?.offset ?: 0)) ?: 0
        // if selectedText is empty, then we use the cursor position to get the text
        if (prefixText.isEmpty()) {
            prefixText = document?.text?.substring(0, lineEndOffset) ?: ""
        }
        // suffixText is the text after the selectedText, which is the text after the cursor position
        val suffixText = document?.text?.substring(lineEndOffset) ?: ""

        chatCodingService.handlePromptAndResponse(
            contentPanel,
            JavaActionPrompting(chatCodingService.actionType, lang, prefixText, file, project),
            getReplaceableAction(event),
            prefixText,
            suffixText
        )
    }

    open fun getReplaceableAction(event: AnActionEvent): ((response: String) -> Unit)? {
        return null
    }

    abstract fun getActionType(): ChatBotActionType
}