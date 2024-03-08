package cc.unitmesh.devti.gui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

class AutoDevToolWindowFactory : ToolWindowFactory, DumbAware {
    object Util {
        const val id = "AutoDev"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatCodingService = ChatCodingService(ChatActionType.CHAT, project)
        val contentPanel = ChatCodingPanel(chatCodingService, toolWindow.disposable)
        val content =
            ContentFactory.getInstance()
                .createContent(contentPanel, AutoDevBundle.message("autodev.chat"), false)

        ApplicationManager.getApplication().invokeLater {
            toolWindow.contentManager.addContent(content)
        }
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setTitleActions(listOfNotNull(ActionUtil.getActionGroup("AutoDev.ToolWindow.Chat.TitleActions")))
    }

    companion object {
        fun getToolWindow(project: Project): ToolWindow? {
            return ToolWindowManager.getInstance(project).getToolWindow(Util.id)
        }
    }
}
