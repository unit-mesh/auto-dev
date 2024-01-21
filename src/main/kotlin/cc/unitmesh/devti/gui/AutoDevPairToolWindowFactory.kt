package cc.unitmesh.devti.gui

import cc.unitmesh.devti.provider.architecture.LayeredArchProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.dsl.builder.panel

class AutoDevPairToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val disposable = toolWindow.disposable
        val panel = AutoDevPairToolWindow(project, disposable)

        val contentManager = toolWindow.contentManager

        val content = contentManager.factory.createContent(panel, "", false)
        contentManager.addContent(content)
    }
}

class AutoDevPairToolWindow(val project: Project, val disposable: Disposable) : SimpleToolWindowPanel(true, true),
    NullableComponent {
    init {
        val layeredArch = LayeredArchProvider.find(project)?.getLayeredArch(project)
        val panel = panel {
            row {
                label("Hello World")
            }
            row {
                // show a tree in a table
                label(layeredArch.toString())
            }
        }

        setContent(panel)
    }

    override fun isNull(): Boolean {
        return false
    }
}
