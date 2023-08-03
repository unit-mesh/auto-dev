package cc.unitmesh.devti.toolwindow

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.wm.ToolWindowManager

fun sendToChat(project: Project, runnable: (ChatCodingPanel) -> Unit) {
    val chatCodingService = ChatCodingService(ChatActionType.CHAT, project)

    val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id)
    val contentManager = toolWindowManager?.contentManager
    val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager?.disposable,)

    val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

    contentManager?.removeAllContents(true)
    contentManager?.addContent(content!!)

    toolWindowManager?.activate {
        runnable(contentPanel)
    }
}


fun sendToChat(project: Project, actionType: ChatActionType, prompter: ContextPrompter) {
    val toolWindowManager =
        ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id) ?: return
    val chatCodingService = ChatCodingService(actionType, project)

    val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager?.disposable)

    val contentManager = toolWindowManager.contentManager
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
    chatActionType: ChatActionType
) {
    val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id)
    val contentManager = toolWindowManager?.contentManager

    val chatCodingService = ChatCodingService(chatActionType, project)
    val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager?.disposable)
    val content = contentManager?.factory?.createContent(contentPanel, "Chat with this", false)

    contentManager?.removeAllContents(true)
    contentManager?.addContent(content!!)

    toolWindowManager?.activate {
        contentPanel.setContent("\n```$language\n$prefixText\n```")
    }
}
