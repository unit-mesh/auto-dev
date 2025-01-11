package cc.unitmesh.devti.gui

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.inline.InlineChatPanelView
import cc.unitmesh.devti.settings.LanguageChangedCallback.componentStateChanged
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.impl.ContentManagerImpl

class AutoDevToolWindowFactory : ToolWindowFactory, DumbAware {
    object Util {
        const val id = "AutoDev"
    }

    private val contentFactory = ContentFactory.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        cc.unitmesh.devti.inline.ShireInlineChatProvider.addListener(project)

        val chatCodingService = ChatCodingService(ChatActionType.CHAT, project)
        val contentPanel = ChatCodingPanel(chatCodingService, toolWindow.disposable)
        val chatPanel = contentFactory.createContent(contentPanel, "Chat", false)
        val sketchView = InlineChatPanelView(project, null)
        val sketchPanel = contentFactory.createContent(sketchView, "Sketch", false)

        ApplicationManager.getApplication().invokeLater {
            val contentManager = toolWindow.contentManager
            contentManager.addContent(sketchPanel)
            contentManager.addContent(chatPanel)
        }
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setTitleActions(listOfNotNull(ActionUtil.getActionGroup("AutoDev.ToolWindow.Chat.TitleActions")))
    }


    companion object {
        fun getToolWindow(project: Project): ToolWindow? {
            return ToolWindowManager.getInstance(project).getToolWindow(Util.id)
        }

        fun setInitialDisplayName(content: Content) {
            componentStateChanged("autodev.chat", content, 2) { c, d -> c.displayName = d }
        }
    }
}
