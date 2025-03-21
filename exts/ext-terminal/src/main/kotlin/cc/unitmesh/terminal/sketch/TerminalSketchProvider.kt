package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.sketch.run.ProcessExecutor
import cc.unitmesh.devti.sketch.run.ShellSafetyCheck
import cc.unitmesh.devti.sketch.run.UIUpdatingWriter
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.jetbrains.ide.PooledThreadExecutor
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.LineBorder

class TerminalSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "bash" || lang == "shell"

    override fun create(project: Project, content: String): ExtensionLangSketch = TerminalLangSketch(project, content)
}

class TerminalLangSketch(val project: Project, var content: String) : ExtensionLangSketch {
    val enableAutoRunTerminal = project.coderSetting.state.enableAutoRunTerminal

    var terminalWidget: JBTerminalWidget? = null
    var mainPanel: JPanel? = null
    val actionGroup = DefaultActionGroup(createConsoleActions())
    val toolbar = ActionManager.getInstance().createActionToolbar("TerminalSketch", actionGroup, true).apply {
        targetComponent = mainPanel
        this.component.border = JBUI.Borders.empty()
    }

    val titleLabel = JLabel("Terminal").apply {
        border = JBUI.Borders.empty(0, 10)
    }

    val codeSketch = CodeHighlightSketch(project, content, CodeFence.findLanguage("bash")).apply {
        border = JBUI.Borders.empty()
    }

    val codePanel = JPanel(BorderLayout()).apply {
        add(codeSketch.getComponent(), BorderLayout.CENTER)
    }

    val resultSketch = CodeHighlightSketch(project, "", CodeFence.findLanguage("bash")).apply {
        border = JBUI.Borders.empty()
    }

    val resultPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty()
        add(resultSketch.getComponent(), BorderLayout.CENTER)
    }

    private val successColor = JBColor(Color(233, 255, 233), Color(0, 77, 0))
    private val errorColor = JBColor(Color(255, 233, 233), Color(77, 0, 0))

    val collapsibleResultPanel = CollapsiblePanel("Execution Results", resultPanel, initiallyCollapsed = true)

    val toolbarPanel = JPanel(BorderLayout()).apply {
        add(titleLabel, BorderLayout.WEST)
        add(toolbar.component, BorderLayout.EAST)
    }

    private var toolbarWrapper = Wrapper(JBUI.Panels.simplePanel(toolbarPanel)).also {
        it.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0)
    }

    private lateinit var executeAction: TerminalExecuteAction
    private var resizableTerminalPanel: ResizableTerminalPanel
    private var isCodePanelVisible = false

    init {
        val projectDir = project.guessProjectDir()?.path
        val terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)

        terminalWidget = terminalRunner.createTerminalWidget(this, projectDir, true).also {
            it.preferredSize = Dimension(it.preferredSize.width, 80)
        }

        resizableTerminalPanel = ResizableTerminalPanel(terminalWidget!!)

        codeSketch.getComponent().border = JBUI.Borders.empty()
        resultSketch.getComponent().border = JBUI.Borders.empty()

        mainPanel = object : JPanel(VerticalLayout(JBUI.scale(0))) {
            init {
                add(toolbarWrapper)
                add(collapsibleResultPanel)
                add(resizableTerminalPanel)
            }
        }

        mainPanel!!.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1),
            JBUI.Borders.empty()
        )
        terminalWidget!!.addMessageFilter(FrontendWebViewServerFilter(project, mainPanel!!))
    }
    
    private fun setResultStatus(success: Boolean, errorMessage: String? = null) {
        ApplicationManager.getApplication().invokeLater {
            when {
                success -> {
                    resultPanel.background = successColor
                    resultPanel.border = LineBorder(JBColor(Color(0, 128, 0), Color(0, 100, 0)), 1)
                    collapsibleResultPanel.setTitle("✅ Execution Successful")
                }
                else -> {
                    resultPanel.background = errorColor
                    resultPanel.border = LineBorder(JBColor(Color(128, 0, 0), Color(100, 0, 0)), 1)
                    val errorText = errorMessage?.let { ": $it" } ?: ""
                    collapsibleResultPanel.setTitle("❌ Execution Failed$errorText")
                }
            }
            resultPanel.repaint()
        }
    }
    
    private fun toggleCodePanel() {
        if (isCodePanelVisible) {
            mainPanel!!.remove(codePanel)
            isCodePanelVisible = false
        } else {
            // Add code panel at index 1 (after toolbar, before result panel)
            mainPanel!!.add(codePanel, 1)
            isCodePanelVisible = true
        }
        
        mainPanel!!.revalidate()
        mainPanel!!.repaint()
    }

    fun createConsoleActions(): List<AnAction> {
        executeAction = TerminalExecuteAction()

        val showCodeAction = object :
            AnAction("Show/Hide Code", "Show or hide the shell code", AutoDevIcons.View) {
            override fun actionPerformed(e: AnActionEvent) {
                toggleCodePanel()
            }
        }

        val copyAction = object :
            AnAction("Copy", AutoDevBundle.message("sketch.terminal.copy.text"), AutoDevIcons.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(getViewText())
                clipboard.setContents(selection, null)
            }
        }

        val sendAction = object :
            AnAction("Send to Chat", AutoDevBundle.message("sketch.terminal.send.chat"), AutoDevIcons.Send) {
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

        val popupAction = object :
            AnAction(
                "Popup",
                AutoDevBundle.message("sketch.terminal.popup"),
                AllIcons.Ide.External_link_arrow
            ) {
            override fun displayTextInToolbar(): Boolean = true

            override fun actionPerformed(e: AnActionEvent) {
                executePopup(terminalWidget, project).mouseClicked(null)
            }
        }

        return listOf(executeAction, showCodeAction, copyAction, sendAction, popupAction)
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
                        resizableTerminalPanel.removeAll()
                        resizableTerminalPanel.add(terminalWidget.component, BorderLayout.CENTER)
                        resizableTerminalPanel.revalidate()
                        resizableTerminalPanel.repaint()
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

    private var hasSent = false
    override fun onComplete(code: String) {
        if (hasSent) return
        codeSketch.updateViewText(code, true)
        titleLabel.text = "Terminal - ($content)"

        val (isDangerous, reason) = try {
            ShellSafetyCheck.checkDangerousCommand(content)
        } catch (e: Exception) {
            Pair(true, "Error checking command safety: ${e.message}")
        }

        if (isDangerous) {
            AutoDevNotifications.notify(project, "Auto-execution has been disabled for safety: $reason")

            ApplicationManager.getApplication().invokeLater {
                terminalWidget!!.terminalStarter?.sendString(
                    "echo \"⚠️ WARNING: $reason - Command not auto-executed for safety\"",
                    false
                )

                resultSketch.updateViewText(
                    "⚠️ WARNING: $reason\nThe command was not auto-executed for safety reasons.\nPlease review and run manually if you're sure.",
                    true
                )
                setResultStatus(false, reason)
                collapsibleResultPanel.expand()
            }
            return
        }

        hasSent = true
    }

    override fun onDoneStream(allText: String) {
        if (content.lines().size > 1) return
        ApplicationManager.getApplication().invokeLater {
            terminalWidget!!.terminalStarter?.sendString(content, false)

            if (enableAutoRunTerminal && ::executeAction.isInitialized) {
                executeAction.actionPerformed(
                    AnActionEvent.createFromAnAction(
                        executeAction,
                        null,
                        "AutoExecuteTerminal",
                        DataContext.EMPTY_CONTEXT
                    )
                )
            }
        }
    }

    override fun getComponent(): JComponent = mainPanel!!
    override fun updateLanguage(language: Language?, originLanguage: String?) {}
    override fun dispose() {
        codeSketch.dispose()
        resultSketch.dispose()
    }

    inner class TerminalExecuteAction :
        AnAction("Execute", AutoDevBundle.message("sketch.terminal.execute"), AutoDevIcons.Run) {
        override fun actionPerformed(e: AnActionEvent) {
            titleLabel.icon = AllIcons.RunConfigurations.TestState.Run
            
            val stdWriter = UIUpdatingWriter(
                onTextUpdate = { text, complete ->
                    resultSketch.updateViewText(text, complete)
                },
                onPanelUpdate = { title, _ ->
                    collapsibleResultPanel.setTitle(title)
                },
                checkCollapsed = {
                    collapsibleResultPanel.isCollapsed()
                },
                expandPanel = {
                    collapsibleResultPanel.expand()
                }
            )

            resultSketch.updateViewText("", true)
            stdWriter.setExecuting(true)
            setResultStatus(false)
            
            AutoDevCoroutineScope.scope(project).launch {
                val executor = ProcessExecutor(project)
                try {
                    val dispatcher = PooledThreadExecutor.INSTANCE.asCoroutineDispatcher()
                    val exitCode = executor.exec(getViewText(), stdWriter, stdWriter, dispatcher)
                    ApplicationManager.getApplication().invokeLater {
                        stdWriter.setExecuting(false)
                        if (collapsibleResultPanel.isCollapsed()) {
                            collapsibleResultPanel.expand()
                        }
                        // Clear the running icon.
                        titleLabel.icon = null
                        val success = exitCode == 0
                        setResultStatus(success, if (!success) "Process exited with code $exitCode" else null)
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        stdWriter.setExecuting(false)
                        // Clear the running icon.
                        titleLabel.icon = null
                        resultSketch.updateViewText("${stdWriter.getContent()}\nError: ${ex.message}", true)
                        setResultStatus(false, ex.message)
                    }
                }
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

