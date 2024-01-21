package cc.unitmesh.devti.gui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class AutoDevPairToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = AutoDevPairToolWindow(project)
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(panel, "", false)
        contentManager.addContent(content)
    }
}

class AutoDevPairToolWindow(val project: Project) : JComponent() {
    init {
        // A table like todo list
        panel {
            row {
                label("Hello World")
            }
        }
    }
}
