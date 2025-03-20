package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.sketch.run.ProcessExecutor
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence
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
            val codePanel = JPanel(BorderLayout()).apply {
                add(codeSketch.getComponent(), BorderLayout.CENTER)
            }
            
            val isSingleLine = content.lines().filter { it.trim().isNotEmpty() }.size <= 1
            val collapsibleCodePanel = CollapsiblePanel("Shell Code", codePanel, initiallyCollapsed = isSingleLine)

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

                codeSketch.getComponent().border = JBUI.Borders.empty()
                mainPanel = object : JPanel(VerticalLayout(JBUI.scale(0))) {
                    init {
                        add(toolbarWrapper)
                        add(collapsibleCodePanel)
                        add(terminalWidget!!.component)
                    }
                }

                mainPanel!!.border = JBUI.Borders.empty(0, 8)
                terminalWidget!!.addMessageFilter(FrontendWebViewServerFilter(project, mainPanel!!))
            }

            fun createConsoleActions(): List<AnAction> {
                val executeAction = object : AnAction("Execute", "Execute command", AllIcons.Actions.Execute) {
                    override fun actionPerformed(e: AnActionEvent) {
                        ProcessExecutor(project).executeCode(getViewText())
                    }
                }

                val clearAction = object : AnAction("Clear", "Clear terminal", AllIcons.Actions.GC) {
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

                val sendAction = object : AnAction("Send to Chat", "Send to chat", AutoDevIcons.Send) {
                    override fun actionPerformed(e: AnActionEvent) {
                        try {
                            val output = terminalWidget!!::class.java.getMethod("getText")
                                .invoke(terminalWidget) as String
                            sendToSketch(project, "Help me to solve this issue:\n```bash\n$output\n```\n")
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

                return listOf(executeAction, copyAction, clearAction, sendAction, popupAction)
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

            override fun onComplete(code: String) {
                titleLabel.text = "Terminal - ($content)"
            }

            override fun onDoneStream(allText: String) {
                if (content.lines().size > 1) return
                ApplicationManager.getApplication().invokeLater {
                    terminalWidget!!.terminalStarter?.sendString(content, false)
                }
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
            it?.isUserScrolling = false
            it?.scrollToBottom()
            it?.sendInput(output)
        }
    }
}

