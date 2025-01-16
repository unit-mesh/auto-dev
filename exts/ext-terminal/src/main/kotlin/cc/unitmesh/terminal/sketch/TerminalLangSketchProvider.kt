package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.provider.RunService
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class TerminalLangSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "bash"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        var content = content
        return object : ExtensionLangSketch {
            var terminalWidget: JBTerminalWidget? = null
            var layout: JPanel? = null

            init {
                val projectDir = project.guessProjectDir()?.path
                val terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)
                terminalWidget = terminalRunner.createTerminalWidget(this, projectDir, true).also {
                    it.preferredSize = Dimension(it.preferredSize.width, 120)
                }

                layout = JPanel(VerticalLayout(JBUI.scale(10))).apply {
                    add(JLabel("Terminal"), BorderLayout.NORTH)
                    add(terminalWidget!!.component, BorderLayout.CENTER)
                    val buttonPanel = JPanel(BorderLayout())
                    buttonPanel.add(JButton("Run").apply {
                        addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent?) {
                                val tempShellName = "temp" + System.currentTimeMillis() + ".sh"
                                val language = Language.findLanguageByID("Shell Script")!!
                                val scratchFile = ScratchRootType.getInstance()
                                    .createScratchFile(project, tempShellName, language, content)
                                    ?: return

                                try {
                                    RunService.provider(project, scratchFile)
                                        ?.runFile(project, scratchFile, null, isFromToolAction = true)
                                        ?: AutoDevNotifications.notify(project, "Run Failed, no provider")
                                } catch (e: Exception) {
                                    AutoDevNotifications.notify(project, "Run Failed: ${e.message}")
                                }
                            }
                        })
                    }, BorderLayout.WEST)
                    buttonPanel.add(JButton("Pop up Terminal").apply {
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
                                    .setCancelCallback {
                                        layout!!.addLayoutComponent("terminal", terminalWidget!!.component)
                                        true
                                    }
                                    .setFocusable(true)
                                    .setRequestFocus(true)
                                    .createPopup()

                                val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
                                popup.showInBestPositionFor(editor)
                            }
                        })
                    }, BorderLayout.EAST)
                    add(buttonPanel, BorderLayout.SOUTH)
                }

                layout!!.border = JBUI.Borders.compound(
                    JBUI.Borders.empty(0, 10),
                    JBUI.Borders.customLine(JBColor.border())
                )
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
