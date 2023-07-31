package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

fun sendToChat(project: Project, actionType: ChatActionType, prompter: ContextPrompter) {
    val toolWindowManager =
        ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id) ?: return
    val chatCodingService = ChatCodingService(actionType, project)
    val contentPanel = ChatCodingComponent(chatCodingService)
    val contentManager = toolWindowManager.contentManager
    val content = contentManager.factory.createContent(contentPanel, chatCodingService.getLabel(), false)

    contentManager.removeAllContents(true)
    contentManager.addContent(content)

    toolWindowManager.activate {
        chatCodingService.handlePromptAndResponse(contentPanel, prompter)
    }
}