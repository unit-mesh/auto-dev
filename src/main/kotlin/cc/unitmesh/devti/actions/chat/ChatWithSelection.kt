package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.wm.ToolWindowManager

fun chatWithSelection(
    project: Project,
    language: @NlsSafe String,
    prefixText: @NlsSafe String,
    chatActionType: ChatActionType
) {
    val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id)
    val contentManager = toolWindowManager?.contentManager

    val chatCodingService = ChatCodingService(chatActionType, project)
    val contentPanel = ChatCodingComponent(chatCodingService)
    val content = contentManager?.factory?.createContent(contentPanel, "Chat with this", false)

    contentManager?.removeAllContents(true)
    contentManager?.addContent(content!!)

    toolWindowManager?.activate {
        contentPanel.setContent(
            "\n" + """
                |```$language
                |$prefixText
                |```""".trimMargin()
        )
    }
}