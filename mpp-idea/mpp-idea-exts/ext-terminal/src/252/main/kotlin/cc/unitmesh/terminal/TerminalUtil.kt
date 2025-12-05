package cc.unitmesh.terminal

import cc.unitmesh.terminal.ShellCommandSuggestAction.Companion.suggestCommand
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.content.Content
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

object TerminalUtil {
    fun sendMsg(project: Project, data: String, e: AnActionEvent) {
        val content = getContent(project) ?: return
        val findWidgetByContent = TerminalToolWindowManager.findWidgetByContent(content) ?: return
//        val controller = lookupTerminalPromptControllerByView(findWidgetByContent)
        trySendMsgInOld(project, data, content)
    }

    private fun lookupTerminalPromptControllerByView(findWidgetByContent: TerminalWidget): Unit? {
//        val terminalView = (findWidgetByContent.component as? Wrapper)?.targetComponent ?: return null
//        if (terminalView is DataProvider) {
//            val controller = terminalView.getData(TerminalPromptController.KEY.name)
//            return (controller as? TerminalPromptController)
//        }

        return null
    }

    private fun trySendMsgInOld(project: Project, data: String, content: Content): Boolean {
        val widget = TerminalToolWindowManager.getWidgetByContent(content) ?: return true
        suggestCommand(data, project, { string ->
            widget.terminalStarter?.sendString(string, true)
        }, {})

        return false
    }

    private fun getContent(project: Project): Content? {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
        return toolWindow?.contentManager?.selectedContent
    }
}
