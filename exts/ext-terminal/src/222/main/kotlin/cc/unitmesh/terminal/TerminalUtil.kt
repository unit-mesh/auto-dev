package cc.unitmesh.terminal

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView

object TerminalUtil {
    fun getCurrentTerminalWidget(project: Project): JBTerminalWidget? {
        // TODO: test with `e.dataContext.getData(JBTerminalWidget.TERMINAL_DATA_KEY) ?: return`
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID) ?: return null
        val content = toolWindow.contentManager.selectedContent ?: return null
        val widget = TerminalView.getWidgetByContent(content) ?: return null
        return widget
    }
}