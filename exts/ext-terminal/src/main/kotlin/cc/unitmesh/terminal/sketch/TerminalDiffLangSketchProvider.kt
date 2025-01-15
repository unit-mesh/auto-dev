package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.fullWidth
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.lang.Language
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent

class TerminalDiffLangSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "bash"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        var content = content
        return object : ExtensionLangSketch {
            var terminalWidget: JBTerminalWidget? = null
            var layout: DialogPanel? = null

            init {
                val projectDir = project.guessProjectDir()?.path
                val terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)
                terminalWidget = terminalRunner.createTerminalWidget(this, projectDir, true).also {
                    it.preferredSize = Dimension(it.preferredSize.width, 120)
                }

                layout = panel {
                    row {
                        label("Terminal").align(AlignX.CENTER)
                    }
                    row {
                        cell(terminalWidget!!.component).fullWidth()
                    }
                    row {
                        cell(JButton("Pop up Terminal").apply {
                            addMouseListener(object : MouseAdapter() {
                                override fun mouseClicked(e: MouseEvent?) {
                                    var popup: JBPopup? = null
                                    popup = JBPopupFactory.getInstance()
                                        .createComponentPopupBuilder(terminalWidget!!.component, null)
                                        .setProject(project)
                                        .setResizable(true)
                                        .setMovable(true)
                                        .setTitle("Terminal")
                                        .setCancelButton(MinimizeButton("Hide"))
                                        .setCancelOnClickOutside(true)
                                        .setCancelOnWindowDeactivation(false)
                                        .setFocusable(true)
                                        .setRequestFocus(true)
                                        .createPopup()

                                    val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
                                    popup.showInBestPositionFor(editor)
                                }
                            })
                        }).align(AlignX.RIGHT)
                    }
                }
            }

            override fun getExtensionName(): String = "Terminal"
            override fun getViewText(): String = content
            override fun updateViewText(text: String) {
                content = text
            }

            override fun doneUpdateText(text: String) {
                terminalWidget!!.terminalStarter?.sendString(content, true)
            }

            override fun getComponent(): JComponent = layout!!

            override fun updateLanguage(language: Language?, originLanguage: String?) {}

            override fun dispose() {}
        }
    }
}
