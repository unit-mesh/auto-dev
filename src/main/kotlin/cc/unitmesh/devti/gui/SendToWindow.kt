package cc.unitmesh.devti.gui

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.wm.ToolWindowManager

fun sendToChatWindow(
    project: Project,
    actionType: ChatActionType,
    runnable: (ChatCodingPanel, ChatCodingService) -> Unit,
) {
    val chatCodingService = ChatCodingService(actionType, project)

    val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id)
    val contentManager = toolWindowManager?.contentManager
    val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager?.disposable)
    val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

    contentManager?.removeAllContents(true)
    contentManager?.addContent(content!!)

    toolWindowManager?.activate {
        runnable(contentPanel, chatCodingService)
    }
}


fun sendToChatPanel(project: Project, runnable: (ChatCodingPanel, ChatCodingService) -> Unit) {
    val actionType = ChatActionType.CHAT

    sendToChatWindow(project, actionType, runnable)
}

fun sendToChatPanel(project: Project, actionType: ChatActionType, prompter: ContextPrompter) {
    sendToChatWindow(project, actionType) { contentPanel, chatCodingService ->
        chatCodingService.handlePromptAndResponse(contentPanel, prompter)
    }
}

fun chatWithSelection(
    project: Project,
    language: @NlsSafe String,
    prefixText: @NlsSafe String,
    actionType: ChatActionType,
) {
    sendToChatWindow(project, actionType) { contentPanel, _ ->
        contentPanel.setInput("\n```$language\n$prefixText\n```")
    }
}
