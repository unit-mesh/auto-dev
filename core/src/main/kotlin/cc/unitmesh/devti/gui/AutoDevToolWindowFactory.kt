package cc.unitmesh.devti.gui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.bridge.BridgeToolWindow
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.NormalChatCodingPanel
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.inline.AutoDevInlineChatProvider
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.util.relativePath
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.search.ProjectScope
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.messages.MessageBusConnection

private const val NORMAL_CHAT = "AutoDev Chat"
private const val SKETCH_TITLE = "Sketch"
private const val BRIDGE_TITLE = "Bridge"
private const val CHAT_KEY = "autodev.chat"

class AutoDevToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
    object Util {
        const val id = "AutoDev"
    }

    private var connection: MessageBusConnection? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        initInlineChatForIdea223(project)
        initObservers(project)
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

    private fun initObservers(project: Project) {
        connection = project.messageBus.connect()
        val searchScope = ProjectScope.getProjectScope(project)
        connection?.subscribe(SMTRunnerEventsListener.TEST_STATUS, object : SMTRunnerEventsAdapter() {
            override fun onSuiteFinished(suite: SMTestProxy, nodeId: String?) {
                logger<AutoDevToolWindowFactory>().info(suite.toString())
            }

            override fun onTestFailed(test: SMTestProxy) {
                val sourceCode = test.getLocation(project, searchScope)
                runInEdt {
                    sendToChatWindow(project, ChatActionType.CHAT) { contentPanel, _ ->
                        val psiElement = sourceCode?.psiElement
                        val language = psiElement?.language?.displayName ?: ""
                        val filepath = psiElement?.containingFile?.virtualFile?.relativePath(project) ?: ""
                        val code = runReadAction { psiElement?.text ?: "" }
                        contentPanel.setInput(
                            """Help me fix follow test issue:
                           | ErrorMessage:
                           |```
                           |${test.errorMessage}
                           |```
                           |stacktrace details: 
                           |${test.stacktrace}
                           |
                           |// filepath: $filepath
                           |origin code:
                           |```$language
                           |$code
                           |```
                           |""".trimMargin()
                        )
                    }
                }
            }

            override fun onTestFinished(test: SMTestProxy, nodeId: String?) {
                logger<AutoDevToolWindowFactory>().info(nodeId)
            }
        })
    }

    override fun dispose() {
        connection?.disconnect()
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

            val content =
                contentManager.factory.createContent(contentPanel, label, false)
            contentManager.addContent(content)

            contentManager.setSelectedContent(content)

            return contentPanel
        }

        fun labelNormalChat(chatCodingService: ChatCodingService): NormalChatCodingPanel? {
            val toolWindow = getToolWindow(chatCodingService.project) ?: return null
            return labelNormalChat(toolWindow, chatCodingService)
        }

        fun setInitialDisplayName(content: Content) {
            componentStateChanged(CHAT_KEY, content, 2) { c, d -> c.displayName = d }
        }

        fun getSketchWindow(project: Project): SketchToolWindow? {
            return getToolWindow(project)?.contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()
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

        fun createSketchToolWindow(project: Project, toolWindow: ToolWindow) {
            val sketchView = SketchToolWindow(project, null, true, ChatActionType.SKETCH)
            val sketchPanel = ContentFactory.getInstance().createContent(sketchView, SKETCH_TITLE, true)
            toolWindow.contentManager.addContent(sketchPanel)
        }

        fun createBridgeToolWindow(project: Project, toolWindow: ToolWindow) {
            val sketchView = BridgeToolWindow(project, null, true)
            val sketchPanel = ContentFactory.getInstance().createContent(sketchView, BRIDGE_TITLE, true)
            toolWindow.contentManager.addContent(sketchPanel)
        }
    }
}
