package cc.unitmesh.devti.gui

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

fun sendToChatWindow(
    project: Project,
    actionType: ChatActionType,
    runnable: (ChatCodingPanel, ChatCodingService) -> Unit,
) {
    val chatCodingService = ChatCodingService(actionType, project)

    val toolWindowManager = AutoDevToolWindowFactory.getToolWindow(project) ?: run {
            logger<ChatCodingService>().warn("Tool window not found")
            return
        }

    val contentManager = toolWindowManager.contentManager
    val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager.disposable)
    val content = contentManager.factory.createContent(contentPanel, chatCodingService.getLabel(), false)

    ApplicationManager.getApplication().invokeLater {
        contentManager.removeAllContents(false)
        contentManager.addContent(content)

        toolWindowManager.activate {
            runnable(contentPanel, chatCodingService)
        }
    }
}

fun sendToChatPanel(project: Project, runnable: (ChatCodingPanel, ChatCodingService) -> Unit) {
    val actionType = ChatActionType.CHAT
    sendToChatWindow(project, actionType, runnable)
}

fun sendToChatPanel(project: Project, actionType: ChatActionType, runnable: (ChatCodingPanel, ChatCodingService) -> Unit) {
    sendToChatWindow(project, actionType, runnable)
}

fun sendToChatPanel(project: Project, actionType: ChatActionType, prompter: ContextPrompter) {
    sendToChatWindow(project, actionType) { contentPanel, chatCodingService ->
        chatCodingService.handlePromptAndResponse(contentPanel, prompter, keepHistory = true)
    }
}
