package cc.unitmesh.devti.language.provider

import cc.unitmesh.devti.language.provider.terminal.TerminalHandler
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.awt.Component

interface TerminalLocationExecutor {
    fun getComponent(e: AnActionEvent): Component?
    fun bundler(project: Project, userInput: String): TerminalHandler?

    companion object {
        private val EP_NAME: ExtensionPointName<TerminalLocationExecutor> =
            ExtensionPointName.create("cc.unitmesh.shireTerminalExecutor")

        fun provide(project: Project): TerminalLocationExecutor? {
            return EP_NAME.extensionList.firstOrNull()
        }
    }
}

class ShireTerminalExecutor : TerminalLocationExecutor {
    override fun getComponent(e: AnActionEvent): Component? {
        return e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT)
    }

    override fun bundler(project: Project, userInput: String): TerminalHandler? {
        val content = getContent(project) ?: return null
        return trySendMsgInOld(project, userInput, content)
    }

    private fun trySendMsgInOld(project: Project, userInput: String, content: Content): TerminalHandler? {
        val widget = TerminalToolWindowManager.getWidgetByContent(content) ?: return null
        return TerminalHandler(
            userInput,
            project,
            onChunk = { string ->
                widget.terminalStarter?.sendString(string, true)
            },
            onFinish = {})
    }

    private fun getContent(project: Project): Content? {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
        return toolWindow?.contentManager?.selectedContent
    }
}
