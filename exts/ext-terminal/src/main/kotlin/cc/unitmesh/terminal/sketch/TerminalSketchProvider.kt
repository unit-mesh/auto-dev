package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.agent.view.WebViewWindow
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.execution.filters.Filter
import com.intellij.icons.AllIcons
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
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
            var mainPanel: JPanel? = null
            val actionGroup = DefaultActionGroup(createConsoleActions())
            val toolbar = ActionManager.getInstance().createActionToolbar("TerminalSketch", actionGroup, true).apply {
                targetComponent = mainPanel
            }

            val titleLabel = JLabel("Terminal").apply {
                border = JBUI.Borders.empty(0, 10)
            }

            val codeSketch = CodeHighlightSketch(project, content, CodeFence.findLanguage("bash"))

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

                mainPanel = object : JPanel(VerticalLayout(JBUI.scale(0))) {
                    init {
                        add(toolbarWrapper)
                        add(codeSketch.getComponent())
                        add(terminalWidget!!.component)
                    }
                }

                mainPanel!!.border = JBUI.Borders.empty(0, 8)
                terminalWidget!!.addMessageFilter(FrontendWebViewServerFilter(project, mainPanel!!))
            }

            fun createConsoleActions(): List<AnAction> {
                val clearAction = object : AnAction("Clear", "Clear Terminal", AllIcons.Actions.GC) {
                    override fun actionPerformed(e: AnActionEvent) {
                        terminalWidget?.terminalStarter?.sendString("clear\n", false)
                    }
                }

                val copyAction = object : AnAction("Copy", "Copy text", AllIcons.Actions.Copy) {
                    override fun actionPerformed(e: AnActionEvent) {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        val selection = StringSelection(getViewText())
                        clipboard.setContents(selection, null)
                    }
                }

                val sendAction = object : AnAction("Send to Chat", "Send to Chat", AutoDevIcons.Send) {
                    override fun actionPerformed(e: AnActionEvent) {
                        try {
                            val output = terminalWidget!!::class.java.getMethod("getText")
                                .invoke(terminalWidget) as String
                            sendToSketch(project, output)
                        } catch (e: Exception) {
                            AutoDevNotifications.notify(project, "Failed to send to Sketch")
                        }
                    }
                }

                val popupAction = object : AnAction("Popup", "Popup terminal", AllIcons.Ide.External_link_arrow) {
                    override fun displayTextInToolbar(): Boolean = true

                    override fun actionPerformed(e: AnActionEvent) {
                        executePopup(terminalWidget, project).mouseClicked(null)
                    }
                }

                return listOf(copyAction, clearAction, sendAction, popupAction)
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
                                mainPanel!!.remove(terminalWidget.component)
                                mainPanel!!.add(terminalWidget.component)
                                mainPanel!!.revalidate()
                                mainPanel!!.repaint()
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
            override fun updateViewText(text: String, complete: Boolean) {
                codeSketch.updateViewText(text, complete)
                content = text
            }

            override fun onDoneStream(allText: String) {
                var isAlreadySent = false
                if (content.lines().size > 1) return

                titleLabel.text = "Terminal - ($content)"

                ApplicationManager.getApplication().invokeLater {
                    terminalWidget!!.terminalStarter?.sendString(content, true)
                    terminalWidget!!.revalidate()
                    terminalWidget!!.repaint()
                }

                isAlreadySent = true
            }

            override fun getComponent(): JComponent = mainPanel!!

            override fun updateLanguage(language: Language?, originLanguage: String?) {}

            override fun dispose() {
                codeSketch.dispose()
            }
        }
    }

    private fun sendToSketch(project: Project, output: String) {
        val contentManager = ToolWindowManager.getInstance(project).getToolWindow("AutoDev")?.contentManager
        contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()?.firstOrNull().let {
            it?.sendInput(output)
        }
    }
}

class FrontendWebViewServerFilter(val project: Project, val mainPanel: JPanel) : Filter {
    var isAlreadyStart = false
    val regex = """Local:\s+(http://localhost:\d+)""".toRegex()

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (isAlreadyStart) return null

        if (!line.contains("Local:")) return null
        val matchResult = regex.find(line)
        if (matchResult == null) return null

        val url = matchResult.groupValues[1]
        ApplicationManager.getApplication().invokeLater {
            val webViewWindow = WebViewWindow().apply {
                loadURL(url)
            }

            var additionalPanel = JPanel(BorderLayout()).apply {
                add(webViewWindow.component, BorderLayout.CENTER)
            }

            mainPanel.add(additionalPanel)
            mainPanel.revalidate()
            mainPanel.repaint()
        }

        isAlreadyStart = true

        return null
    }
}
