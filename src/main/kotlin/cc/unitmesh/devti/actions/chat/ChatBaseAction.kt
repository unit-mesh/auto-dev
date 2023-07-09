package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ActionPromptFormatter
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager

abstract class ChatBaseAction: AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val toolWindowManager = ToolWindowManager.getInstance(project!!).getToolWindow("DevTiFlow")
        val contentManager = toolWindowManager?.contentManager

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        val selectedText = caretModel?.currentCaret?.selectedText ?: ""
        val lang = event.getData(CommonDataKeys.PSI_FILE)?.language?.displayName ?: ""
        val file = event.getData(CommonDataKeys.PSI_FILE)

        val chatCodingService = ChatCodingService(getActionType())
        val contentPanel = ChatCodingComponent(chatCodingService)
        val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

        contentManager?.removeAllContents(true)
        contentManager?.addContent(content!!)
        toolWindowManager?.activate(null)

        chatCodingService.handlePromptAndResponse(
            contentPanel,
            ActionPromptFormatter(chatCodingService.actionType, lang, selectedText, file, project),
            getReplaceableAction(event)
        )

    }

    open fun getReplaceableAction(event: AnActionEvent): ((response: String) -> Unit)? {
        return null
    }

    abstract fun getActionType(): ChatBotActionType
}