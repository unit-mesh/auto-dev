package cc.unitmesh.devti.gui

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.inline.AutoDevInlineChatProvider
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class AutoDevToolWindowFactory : ToolWindowFactory, DumbAware {
    object Util {
        const val id = "AutoDev"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatCodingService = ChatCodingService(ChatActionType.CHAT, project)
        val contentPanel = ChatCodingPanel(chatCodingService, toolWindow.disposable)

        val chatPanel = ContentFactory.getInstance().createContent(contentPanel, "AutoDev Chat", false).apply {
            setInitialDisplayName(this)
        }

        contentPanel.resetChatSession()
        initInlineChatForIdea223(project)

        ApplicationManager.getApplication().invokeLater {
            toolWindow.contentManager.addContent(chatPanel)

            val hasSketch =
                toolWindow.contentManager.component.components?.filterIsInstance<SketchToolWindow>()?.firstOrNull()

            if (hasSketch == null) {
                createSketchToolWindow(project, toolWindow)
            }
        }
    }

    /**
     * for idea 223 (aka 2022.3) which don't have [com.intellij.openapi.startup.ProjectActivity]
     */
    private fun initInlineChatForIdea223(project: Project) {
        AutoDevInlineChatProvider.addListener(project)
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

        fun createSketchToolWindow(project: Project, toolWindow: ToolWindow) {
            val sketchView = SketchToolWindow(project, null, true)
            val sketchPanel = ContentFactory.getInstance().createContent(sketchView, "Sketch", true)
            toolWindow.contentManager.addContent(sketchPanel)
        }
    }
}
