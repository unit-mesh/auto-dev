package cc.unitmesh.devti.gui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory

class AutoDevToolWindowFactory : ToolWindowFactory, DumbAware {
    object Util {
        const val id = "AutoDev"
    }

    private val contentFactory = ApplicationManager.getApplication().getService(ContentFactory::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val disposable = toolWindow.disposable
        val chatCodingService = ChatCodingService(ChatActionType.CHAT, project)
        val contentPanel = ChatCodingPanel(chatCodingService, disposable)
        val content = contentFactory.createContent(contentPanel, AutoDevBundle.message("autodev.flow"), false)

        val toolWindowEx = toolWindow as ToolWindowEx
        toolWindowEx.contentManager.addContent(content)
    }
}
