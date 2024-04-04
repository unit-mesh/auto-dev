package cc.unitmesh.terminal

import cc.unitmesh.terminal.ShellCommandSuggestAction.Companion.suggestCommand
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.content.Content
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import org.jetbrains.plugins.terminal.exp.TerminalDataContextUtils.editor

object TerminalUtil {
    fun sendMsg(project: Project, data: String, e: AnActionEvent) {
        val editor = e.editor
        if (editor == null) {
            trySendMsgInOld(project, data)
            return
        }

        suggestCommand(data, project) { string ->
            editor.document.insertString(editor.caretModel.offset, string)
        }
    }

    private fun trySendMsgInOld(project: Project, data: String): Boolean {
        val widget = getCurrentTerminalWidget(project) ?: return true
        suggestCommand(data, project) { string ->
            widget.terminalStarter?.sendString(string, true)
        }

        return false
    }

    fun getCurrentTerminalWidget(project: Project): JBTerminalWidget? {
        val content = getContent(project) ?: return null
        val widget = TerminalToolWindowManager.getWidgetByContent(content) ?: return null
        return widget
    }

    private fun getContent(project: Project): Content? {
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
        val content = toolWindow?.contentManager?.selectedContent
        return content
    }
}
