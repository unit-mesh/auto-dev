package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.execution.filters.Filter
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * TerminalSketch provide a support for `bash` and `shell` language in terminal.
 */
class TerminalSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "bash" || lang == "shell"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        var content = content
        return object : ExtensionLangSketch {
            var terminalWidget: JBTerminalWidget? = null
            var panelLayout: JPanel? = null

            val actionGroup = DefaultActionGroup(createConsoleActions())
            val toolbar = ActionManager.getInstance().createActionToolbar("TerminalSketch", actionGroup, false).apply {
                targetComponent = panelLayout
            }

            val titleLabel = JLabel("Terminal").apply {
                border = JBUI.Borders.empty(0, 10)
            }

            val toolbarPanel = JPanel(BorderLayout()).apply {
                add(titleLabel, BorderLayout.WEST)
                add(toolbar.component, BorderLayout.EAST)
            }

            private var toolbarWrapper = Wrapper(JBUI.Panels.simplePanel(toolbarPanel)).also {
                it.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1, 1, 1, 1)
            }

            init {
                val projectDir = project.guessProjectDir()?.path
                val terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)
                terminalWidget = terminalRunner.createTerminalWidget(this, projectDir, true).also {
                    it.preferredSize = Dimension(it.preferredSize.width, 120)
                }

                terminalWidget!!.addMessageFilter(object : Filter {
                    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
                        if (line.contains("Local:")) {
                            val regex = """Local:\s+(http://localhost:\d+)""".toRegex()
                            val matchResult = regex.find(line)
                            if (matchResult != null) {
                                val url = matchResult.groupValues[1]
                                AutoDevNotifications.notify(project, "Local server started at $url")
                            }
                        }

                        return null
                    }
                })

                panelLayout = object : JPanel(BorderLayout()) {
                    init {
                        add(toolbarWrapper, BorderLayout.NORTH)
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
            }

            fun createConsoleActions(): List<AnAction> {
                val clearAction = object : AnAction("Clear", "Clear Terminal", null) {
                    override fun actionPerformed(p0: AnActionEvent) {
                        Thread.sleep(2000)
                        terminalWidget?.terminalStarter?.sendString("clear\n", false)
                    }
                }
                return listOf(clearAction)
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
                var isAlreadySent = false
                terminalWidget?.addMessageFilter(object : Filter {
                    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
                        if (isAlreadySent) return null

                        ApplicationManager.getApplication().invokeLater {
                            terminalWidget!!.terminalStarter?.sendString(content, false)
                        }

                        isAlreadySent = true
                        return null
                    }
                })
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
