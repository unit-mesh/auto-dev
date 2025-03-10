package cc.unitmesh.devti.gui

import cc.unitmesh.devti.bridge.BridgeToolWindow
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.NormalChatCodingPanel
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.inline.AutoDevInlineChatProvider
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.sketch.SketchToolWindow
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
        initInlineChatForIdea223(project)
        ApplicationManager.getApplication().invokeLater {
            val normalChatPanel =
                toolWindow.contentManager.component.components?.filterIsInstance<NormalChatCodingPanel>()?.firstOrNull()

            if (normalChatPanel == null) {
                createNormalChatWindow(project, toolWindow)
            } else {
                normalChatPanel.resetChatSession()
            }


            val sketchWindow =
                toolWindow.contentManager.component.components?.filterIsInstance<SketchToolWindow>()?.firstOrNull()

            if (sketchWindow == null) {
                createSketchToolWindow(project, toolWindow)
            } else {
                sketchWindow.resetSketchSession()
            }

            val bridgeWindow =
                toolWindow.contentManager.component.components?.filterIsInstance<BridgeToolWindow>()?.firstOrNull()

            if (bridgeWindow == null) {
                createBridgeToolWindow(project, toolWindow)
            } else {
                bridgeWindow.resetSketchSession()
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
//        toolWindow.setTitleActions(listOfNotNull(ActionUtil.getActionGroup("AutoDev.ToolWindow.Chat.TitleActions")))
    }

    companion object {
        fun getToolWindow(project: Project): ToolWindow? {
            return ToolWindowManager.getInstance(project).getToolWindow(Util.id)
        }

        fun setInitialDisplayName(content: Content) {
            componentStateChanged("autodev.chat", content, 2) { c, d -> c.displayName = d }
        }

        fun getSketchWindow(project: Project): SketchToolWindow? {
            return getToolWindow(project)?.contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()
                ?.firstOrNull()
        }

        fun createNormalChatWindow(project: Project, toolWindow: ToolWindow) {
            val chatCodingService = ChatCodingService(ChatActionType.CHAT, project)
            val contentPanel = NormalChatCodingPanel(chatCodingService, toolWindow.disposable)

            val chatPanel = ContentFactory.getInstance().createContent(contentPanel, "AutoDev Chat", false).apply {
                setInitialDisplayName(this)
            }

            toolWindow.contentManager.addContent(chatPanel)
        }

        fun createSketchToolWindow(project: Project, toolWindow: ToolWindow) {
            val sketchView = SketchToolWindow(project, null, true, ChatActionType.SKETCH)
            val sketchPanel = ContentFactory.getInstance().createContent(sketchView, "Sketch", true)
            toolWindow.contentManager.addContent(sketchPanel)
        }

        fun createBridgeToolWindow(project: Project, toolWindow: ToolWindow) {
            val sketchView = BridgeToolWindow(project, null, true)
            val sketchPanel = ContentFactory.getInstance().createContent(sketchView, "Bridge", true)
            toolWindow.contentManager.addContent(sketchPanel)
        }
    }
}
