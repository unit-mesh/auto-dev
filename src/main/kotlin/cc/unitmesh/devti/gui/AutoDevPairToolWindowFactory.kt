package cc.unitmesh.devti.gui

import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.provider.architecture.LayeredArchProvider
import cc.unitmesh.devti.util.parser.Code
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.temporary.gui.block.*
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
                label("Layered Architecture")
            }
            row {
                val text = layeredArch?.print() ?: "No Layered Arch"
                val block = CodeBlock(SimpleMessage(text, text, ChatRole.User))
                block.code = Code(PlainTextLanguage.INSTANCE, text, true)

                val codeBlockView = CodeBlockView(block, project, disposable)
                fullWidthCell(codeBlockView.getComponent())
            }
        }

        setContent(panel)
    }

    override fun isNull(): Boolean {
        return false
    }
}
