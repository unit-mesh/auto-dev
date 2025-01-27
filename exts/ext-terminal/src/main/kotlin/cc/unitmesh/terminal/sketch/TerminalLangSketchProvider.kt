package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.sketch.run.ShellUtil
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.sh.run.ShRunner
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.components.panels.HorizontalLayout
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
    override fun isSupported(lang: String): Boolean = lang == "bash" || lang == "shell"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        var content = content
        return object : ExtensionLangSketch {
            var terminalWidget: JBTerminalWidget? = null
            var panelLayout: JPanel? = null

            private var titleLabel = JLabel("Terminal")

            init {
                val projectDir = project.guessProjectDir()?.path
                val terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)
                terminalWidget = terminalRunner.createTerminalWidget(this, projectDir, true).also {
                    it.preferredSize = Dimension(it.preferredSize.width, 120)
                }

                panelLayout = object : JPanel(BorderLayout()) {
                    init {
                        add(titleLabel.also {
                            it.border = JBUI.Borders.empty(5, 0)
                        }, BorderLayout.NORTH)

                        add(terminalWidget!!.component, BorderLayout.CENTER)

                        val buttonPanel = JPanel(HorizontalLayout(JBUI.scale(10)))
                        val sendButton = JButton("Send").apply {
                            addMouseListener(object : MouseAdapter() {
                                override fun mouseClicked(e: MouseEvent?) {
                                    try {
                                        val output = terminalWidget!!::class.java.getMethod("getText")
                                            .invoke(terminalWidget) as String
                                        sendToSketch(project, output)
                                    } catch (e: Exception) {
                                        AutoDevNotifications.notify(project, "Failed to send to Sketch")
                                    }
                                }
                            })
                        }

                        val popupButton = JButton("Pop up Terminal").apply {
                            addMouseListener(executePopup(terminalWidget, project))
                        }

                        buttonPanel.add(sendButton)
                        buttonPanel.add(popupButton)
                        add(buttonPanel, BorderLayout.SOUTH)
                    }
                }

                panelLayout!!.border = JBUI.Borders.compound(
                    JBUI.Borders.empty(5, 10),
                )
            }


            private fun executePopup(terminalWidget: JBTerminalWidget?, project: Project): MouseAdapter =
                object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        var popup: JBPopup? = null
                        popup = JBPopupFactory.getInstance()
                            .createComponentPopupBuilder(terminalWidget!!.component, null)
                            .setProject(project)
                            .setResizable(true)
                            .setMovable(true)
                            .setTitle("Terminal")
                            .setCancelButton(MinimizeButton("Hide"))
                            .setCancelCallback {
                                popup?.cancel()
                                panelLayout!!.remove(terminalWidget.component)
                                panelLayout!!.add(terminalWidget.component)

                                panelLayout!!.revalidate()
                                panelLayout!!.repaint()
                                true
                            }
                            .setFocusable(true)
                            .setRequestFocus(true)
                            .createPopup()

                        val editor = FileEditorManager.getInstance(project).selectedTextEditor
                        if (editor != null) {
                            popup.showInBestPositionFor(editor)
                        } else {
                            popup.showInFocusCenter()
                        }
                    }
                }

            override fun getExtensionName(): String = "Terminal"
            override fun getViewText(): String = content
            override fun updateViewText(text: String) {
                content = text
            }

            override fun doneUpdateText(allText: String) {
                titleLabel.text = "Terminal - " + allText.substring(0, 10)
                ApplicationManager.getApplication().invokeLater {
                    Thread.sleep(2000) // todo: change to when terminal ready
                    terminalWidget!!.terminalStarter?.sendString(content, false)
                }
            }

            override fun getComponent(): JComponent = panelLayout!!

            override fun updateLanguage(language: Language?, originLanguage: String?) {}

            override fun dispose() {}
        }
    }

    private fun sendToSketch(project: Project, output: String) {
        val contentManager = ToolWindowManager.getInstance(project).getToolWindow("AutoDev")?.contentManager
        contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()?.firstOrNull().let {
            it?.sendInput(output)
        }
    }
}
