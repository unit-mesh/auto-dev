package cc.unitmesh.devti.gui.pair

import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.gui.chat.AutoDevInputListener
import cc.unitmesh.devti.gui.chat.AutoDevInputSection
import cc.unitmesh.devti.gui.chat.AutoDevInputTrigger
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.pair.arch.ProjectPackageTree
import cc.unitmesh.devti.pair.tasking.Tasking
import cc.unitmesh.devti.provider.architecture.LayeredArchProvider
import cc.unitmesh.devti.util.parser.Code
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.temporary.gui.block.CodeBlock
import com.intellij.temporary.gui.block.CodeBlockView
import com.intellij.temporary.gui.block.SimpleMessage
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

data class PairContext(
    var input: String = "",
    var tasks: List<Tasking> = listOf(),
    var arch: ProjectPackageTree? = null,
)

class AutoDevPairToolWindow(val project: Project, val disposable: Disposable) : SimpleToolWindowPanel(true, true),
    NullableComponent {
    init {
        val layeredArch = LayeredArchProvider.find(project)?.getLayeredArch(project)
        val model = PairContext(arch = layeredArch)

        // https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html
        val panel = panel {
            row { text("Your context-aware pair!") }
            row {
                val inputSection = AutoDevInputSection(project, disposable)
                inputSection.addListener(object : AutoDevInputListener {
                    override fun onSubmit(component: AutoDevInputSection, trigger: AutoDevInputTrigger) {
                        val prompt = component.text
                        println("prompt: $prompt")
                    }
                })

                fullWidthCell(inputSection)
            }
            row { text("Debug context!") }
            row {
                val text = layeredArch?.print() ?: "No Layered Arch"
                val block = CodeBlock(SimpleMessage(text, text, ChatRole.User))
                block.code = Code(PlainTextLanguage.INSTANCE, text, true)

                val codeBlockView = CodeBlockView(block, project, disposable)
                val component = codeBlockView.getComponent()

                fullWidthCell(component)
            }
        }

        setContent(panel)
    }

    override fun isNull(): Boolean {
        return false
    }
}