package cc.unitmesh.devti.toolwindow

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.wm.ToolWindowManager

fun sendToChatPanel(project: Project, runnable: (ChatCodingPanel, ChatCodingService) -> Unit) {
    val actionType = ChatActionType.CHAT

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


fun sendToChatPanel(project: Project, actionType: ChatActionType, prompter: ContextPrompter) {
    val chatCodingService = ChatCodingService(actionType, project)

    val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id)
    val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager?.disposable)
    val contentManager = toolWindowManager?.contentManager!!
    val content = contentManager.factory.createContent(contentPanel, chatCodingService.getLabel(), false)

    contentManager.removeAllContents(true)
    contentManager.addContent(content)

    toolWindowManager.activate {
        chatCodingService.handlePromptAndResponse(contentPanel, prompter)
    }
}

fun chatWithSelection(
    project: Project,
    language: @NlsSafe String,
    prefixText: @NlsSafe String,
    actionType: ChatActionType,
) {
    val chatCodingService = ChatCodingService(actionType, project)

    val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id)
    val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager?.disposable)
    val contentManager = toolWindowManager?.contentManager
    val content = contentManager?.factory?.createContent(contentPanel, "Chat with this", false)

    contentManager?.removeAllContents(true)
    contentManager?.addContent(content!!)

    toolWindowManager?.activate {
        contentPanel.setInput("\n```$language\n$prefixText\n```")
    }
}
