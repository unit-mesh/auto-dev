package cc.unitmesh.terminal

import cc.unitmesh.terminal.ShellCommandSuggestAction.Companion.suggestCommand
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.content.Content
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import org.jetbrains.plugins.terminal.exp.TerminalOutputController

object TerminalUtil {
    fun sendMsg(project: Project, data: String, e: AnActionEvent) {
        val content = getContent(project) ?: return
        val findWidgetByContent = TerminalToolWindowManager.findWidgetByContent(content) ?: return
        val editor = tryGetBlockTerminalEditor(findWidgetByContent)
        if (editor == null) {
            trySendMsgInOld(project, data, content)
            return
        }

        suggestCommand(data, project) { string ->
            runInEdt {
                editor.document.insertString(editor.caretModel.offset, string)
            }
        }
    }

    private fun tryGetBlockTerminalEditor(findWidgetByContent: TerminalWidget): EditorEx? {
        val terminalView = (findWidgetByContent.component as Wrapper).targetComponent
        if (terminalView is DataProvider) {
            val controller = terminalView.getData(TerminalOutputController.KEY.name)
            return (controller as? TerminalOutputController)?.outputModel?.editor
        }

        return null
    }

    private fun trySendMsgInOld(project: Project, data: String, content: Content): Boolean {
        val widget = TerminalToolWindowManager.getWidgetByContent(content) ?: return true
        suggestCommand(data, project) { string ->
            widget.terminalStarter?.sendString(string, true)
        }

        return false
    }

    private fun getContent(project: Project): Content? {
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
        val content = toolWindow?.contentManager?.selectedContent
        return content
    }
}
