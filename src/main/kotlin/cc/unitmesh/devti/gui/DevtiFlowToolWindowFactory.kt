package cc.unitmesh.devti.gui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class DevtiFlowToolWindowFactory : ToolWindowFactory, DumbAware {
    companion object {
        val id = "DevTiFlow"
    }

    private val contentFactory = ApplicationManager.getApplication().getService(
        ContentFactory::class.java
    )

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatCodingService = ChatCodingService(ChatBotActionType.EXPLAIN, project!!)
        val contentPanel = ChatCodingComponent(chatCodingService)
        val content = contentFactory.createContent(contentPanel, AutoDevBundle.message("autodev.flow"), false)
        toolWindow.contentManager.addContent(content)
    }
}
