package cc.unitmesh.devti.gui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.bridge.BridgeToolWindow
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.NormalChatCodingPanel
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.inline.AutoDevInlineChatProvider
import cc.unitmesh.devti.llm2.GithubCopilotDetector
import cc.unitmesh.devti.llm2.GithubCopilotManager
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.sketch.SketchToolWindow
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val NORMAL_CHAT = "AutoDev Chat"
private const val SKETCH_TITLE = "Sketch"
private const val BRIDGE_TITLE = "Bridge"
private const val CHAT_KEY = "autodev.chat"

class AutoDevToolWindowFactory : ToolWindowFactory, DumbAware {
    object AutoDevToolUtil {
        const val ID = "AutoDev"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        initForIdea223(project)
        ApplicationManager.getApplication().invokeLater {
            val normalChatTitle = AutoDevBundle.messageWithLanguage(CHAT_KEY, LanguageChangedCallback.language)
            val normalChatPanel =
                toolWindow.contentManager.findContent(normalChatTitle)?.component as? NormalChatCodingPanel

            if (normalChatPanel == null) {
                createNormalChatWindow(project, toolWindow)
            }

            val sketchWindow = toolWindow.contentManager.findContent(SKETCH_TITLE)?.component as? SketchToolWindow

            if (sketchWindow == null) {
                createSketchToolWindow(project, toolWindow)
            }

            val bridgeWindow = toolWindow.contentManager.findContent(BRIDGE_TITLE)?.component as? BridgeToolWindow

            if (bridgeWindow == null) {
                createBridgeToolWindow(project, toolWindow)
            }
        }
    }

    /**
     * for idea 223 (aka 2022.3) which don't have [com.intellij.openapi.startup.ProjectActivity]
     */
    private fun initForIdea223(project: Project) {
        AutoDevInlineChatProvider.addListener(project)
        AgentObserver.register(project)
        initializeGithubCopilotModelsForIdea223(project)
    }

    /**
     * Initialize GitHub Copilot models for IDEA 223 compatibility
     */
    private fun initializeGithubCopilotModelsForIdea223(project: Project) {
        // 使用协程在后台线程执行初始化
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            if (ApplicationManager.getApplication().isUnitTestMode) return@launch

            // 只有当用户已配置 GitHub Copilot 时才进行初始化
            if (GithubCopilotDetector.isGithubCopilotConfigured()) {
                // 获取服务实例并初始化
                val manager = GithubCopilotManager.getInstance()
                manager.initialize()
            }
        }
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setTitleActions(listOfNotNull(ActionUtil.getActionGroup("AutoDev.ToolWindow.Chat.TitleActions")))
    }

    companion object {
        fun labelNormalChat(
            toolWindowManager: ToolWindow,
            chatCodingService: ChatCodingService
        ): NormalChatCodingPanel {
            val contentManager = toolWindowManager.contentManager
            val contentPanel = NormalChatCodingPanel(chatCodingService, toolWindowManager.disposable)
            val label = chatCodingService.getLabel()

            contentManager.findContent(label)?.let {
                contentManager.removeContent(it, true)
            }

            val content = contentManager.factory.createContent(contentPanel, label, false)
            runInEdt {
                contentManager.addContent(content)
                contentManager.setSelectedContent(content)
            }

            return contentPanel
        }

        fun labelNormalChat(chatCodingService: ChatCodingService): NormalChatCodingPanel? {
            val toolWindow =
                ToolWindowManager.getInstance(chatCodingService.project).getToolWindow(AutoDevToolUtil.ID) ?: return null
            return labelNormalChat(toolWindow, chatCodingService)
        }

        fun setInitialDisplayName(content: Content) {
            componentStateChanged(CHAT_KEY, content, 2) { c, d -> c.displayName = d }
        }

        fun getSketchWindow(project: Project): SketchToolWindow? {
            return ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolUtil.ID)?.contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()
                ?.firstOrNull()
        }

        fun createNormalChatWindow(project: Project, toolWindow: ToolWindow) {
            val chatCodingService = ChatCodingService(ChatActionType.CHAT, project)
            val contentPanel = NormalChatCodingPanel(chatCodingService, toolWindow.disposable)

            val chatPanel = ContentFactory.getInstance().createContent(contentPanel, NORMAL_CHAT, false).apply {
                setInitialDisplayName(this)
            }

            toolWindow.contentManager.addContent(chatPanel)
        }

        fun createSketchToolWindow(project: Project, toolWindow: ToolWindow): SketchToolWindow {
            val sketchView = SketchToolWindow(project, null, true, ChatActionType.SKETCH)
            val sketchPanel = ContentFactory.getInstance().createContent(sketchView, SKETCH_TITLE, true)
            toolWindow.contentManager.addContent(sketchPanel)
            return sketchView
        }

        fun createBridgeToolWindow(project: Project, toolWindow: ToolWindow) {
            val sketchView = BridgeToolWindow(project, null, true)
            val sketchPanel = ContentFactory.getInstance().createContent(sketchView, BRIDGE_TITLE, true)
            toolWindow.contentManager.addContent(sketchPanel)
        }

        fun sendToSketchToolWindow(
            project: Project,
            actionType: ChatActionType,
            runnable: (SketchToolWindow, ChatCodingService) -> Unit,
        ) {
            val chatCodingService = ChatCodingService(actionType, project)
            val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolUtil.ID) ?: run {
                logger<ChatCodingService>().warn("Tool window not found")
                return
            }

            var sketchWindow = toolWindowManager.contentManager.findContent(SKETCH_TITLE)
                ?.component as? SketchToolWindow

            if (sketchWindow == null) {
                sketchWindow = createSketchToolWindow(project, toolWindowManager)
            }

            val content = toolWindowManager.contentManager.getContent(sketchWindow)
            toolWindowManager.contentManager.setSelectedContent(content)

            toolWindowManager.activate {
                invokeLater {
                    runnable(sketchWindow, chatCodingService)
                }
            }
        }
    }
}
