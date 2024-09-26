package cc.unitmesh.devti.gui

import cc.unitmesh.devti.gui.pair.AutoDevPairToolWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class AutoDevPairToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val disposable = toolWindow.disposable
        val panel = AutoDevPairToolWindow(project, disposable)

        val contentManager = toolWindow.contentManager

        val content = contentManager.factory.createContent(panel, "", false)
        contentManager.addContent(content)
    }
}
