package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevNotifications
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
import com.intellij.openapi.observable.util.onceWhenFocusGained
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton
import com.intellij.terminal.JBTerminalWidget
import com.intellij.util.ui.JBUI
import com.jediterm.terminal.ui.TerminalWidgetListener
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
            var panelLayout: JPanel? = null

            init {
                val projectDir = project.guessProjectDir()?.path
                val terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)
                terminalWidget = terminalRunner.createTerminalWidget(this, projectDir, true).also {
                    it.preferredSize = Dimension(it.preferredSize.width, 120)
                }

                panelLayout = object : JPanel(BorderLayout()) {
                    init {
                        add(JLabel("Terminal").also {
                            it.border = JBUI.Borders.empty(5, 0)
                        }, BorderLayout.NORTH)

                        add(terminalWidget!!.component, BorderLayout.CENTER)

                        val buttonPanel = JPanel(BorderLayout())
                        buttonPanel.add(JButton(AllIcons.Toolwindows.ToolWindowRun).apply {
                            addMouseListener(executeShellScriptOnClick(project, content))
                        }, BorderLayout.WEST)
                        buttonPanel.add(JButton("Pop up Terminal").apply {
                            addMouseListener(executePopup(terminalWidget, project))
                        }, BorderLayout.EAST)

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

            override fun doneUpdateText(text: String) {
                ApplicationManager.getApplication().invokeLater {
                    Thread.sleep(1000) // todo: change to when terminal ready
                    terminalWidget!!.terminalStarter?.sendString(content, false)
                }
            }

            override fun getComponent(): JComponent = panelLayout!!

            override fun updateLanguage(language: Language?, originLanguage: String?) {}

            override fun dispose() {}
        }
    }

    fun executeShellScriptOnClick(
        project: Project,
        content: String
    ): MouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            val commandLine = createCommandLineForScript(project, content)
            val processBuilder = commandLine.toProcessBuilder()
            val process = processBuilder.start()
            val processHandler = KillableProcessHandler(process, commandLine.commandLineString)
            processHandler.startNotify()

            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    AutoDevNotifications.notify(project, "Process terminated with exit code ${event.exitCode}, ${event.text}")
                    processHandler.destroyProcess()
                }
            })
        }
    }

    fun createCommandLineForScript(project: Project, scriptText: String): GeneralCommandLine {
        val workingDirectory = project.basePath
        val commandLine = PtyCommandLine()
        commandLine.withConsoleMode(false)
        commandLine.withInitialColumns(120)
        commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        commandLine.setWorkDirectory(workingDirectory!!)
        commandLine.withExePath(ShellUtil.detectShells().first())
        commandLine.withParameters("-c")
        commandLine.withParameters(scriptText)
        return commandLine
    }
}
