package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevColors
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.sketch.run.ShellSafetyCheck
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence
import cc.unitmesh.terminal.service.TerminalRunnerService
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
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
import kotlinx.coroutines.Job
import java.awt.BorderLayout
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

    var lastExecutionResults: String = ""
    var hasExecutionResults: Boolean = false

    var isExecuting = false
    var currentExecutionJob: Job? = null

    val titleLabel = JLabel("Terminal").apply {
        border = JBUI.Borders.empty(0, 10)
    }

    val codeSketch = CodeHighlightSketch(project, content, CodeFence.findLanguage("bash"), showToolbar = false).apply {
        border = JBUI.Borders.empty()
    }

    val codePanel = JPanel(BorderLayout()).apply {
        add(codeSketch.getComponent(), BorderLayout.CENTER)
    }

    val resultSketch = CodeHighlightSketch(project, "", CodeFence.findLanguage("bash")).apply {
        border = JBUI.Borders.empty()
    }

    val resultPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1, 0, 0, 0)
        add(resultSketch.getComponent(), BorderLayout.CENTER)
    }

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
    private var isTerminalPanelVisible = false

    var currentExecutionState: TerminalExecutionState = TerminalExecutionState.READY

    init {
        val projectDir = project.guessProjectDir()?.path

        terminalWidget = TerminalRunnerService.getInstance(project)
            .createTerminalWidget(this, projectDir, true).also {
                it.preferredSize = Dimension(it.preferredSize.width, 80)
            }

        resizableTerminalPanel = ResizableTerminalPanel(terminalWidget!!).apply {
            isVisible = isTerminalPanelVisible
        }

        codeSketch.getComponent().border = JBUI.Borders.empty()
        resultSketch.getComponent().border = JBUI.Borders.empty()

        mainPanel = object : JPanel(VerticalLayout(JBUI.scale(0))) {
            init {
                add(toolbarWrapper)
                add(codePanel)
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

    fun setResultStatus(state: TerminalExecutionState, message: String? = null) {
        ApplicationManager.getApplication().invokeLater {
            when (state) {
                TerminalExecutionState.READY -> {
                    resultPanel.background = UIUtil.getPanelBackground()
                    resultPanel.border = JBUI.Borders.emptyTop(1)
                    collapsibleResultPanel.setTitle("准备执行")
                }

                TerminalExecutionState.EXECUTING -> {
                    resultPanel.background = UIUtil.getPanelBackground()
                    resultPanel.border = LineBorder(AutoDevColors.EXECUTION_RUNNING_BORDER, 1)
                    collapsibleResultPanel.setTitle("⏳ 正在执行...")
                }

                TerminalExecutionState.SUCCESS -> {
                    resultPanel.background = AutoDevColors.EXECUTION_SUCCESS_BACKGROUND
                    resultPanel.border = LineBorder(AutoDevColors.EXECUTION_SUCCESS_BORDER, 1)
                    collapsibleResultPanel.setTitle("✅ 执行成功")
                }

                TerminalExecutionState.FAILED -> {
                    resultPanel.background = AutoDevColors.EXECUTION_ERROR_BACKGROUND
                    resultPanel.border = LineBorder(AutoDevColors.EXECUTION_ERROR_BORDER, 1)
                    val errorText = message?.let { ": $it" } ?: ""
                    collapsibleResultPanel.setTitle("❌ 执行失败$errorText")
                }

                TerminalExecutionState.TERMINATED -> {
                    resultPanel.background = UIUtil.getPanelBackground()
                    resultPanel.border = LineBorder(AutoDevColors.EXECUTION_WARNING_BORDER, 1)
                    collapsibleResultPanel.setTitle("⚠️ 执行已终止")
                }
            }

            currentExecutionState = state
            resultPanel.repaint()
        }
    }

    private fun toggleTerminalAction() {
        resizableTerminalPanel.isVisible = !resizableTerminalPanel.isVisible
        resizableTerminalPanel.revalidate()

        isTerminalPanelVisible = resizableTerminalPanel.isVisible
    }

    fun createConsoleActions(): List<AnAction> {
        executeAction = TerminalExecuteAction(this)

        val showText = AutoDevBundle.message("sketch.terminal.show.hide")
        val showTerminalAction = object : AnAction(showText, showText, AutoDevIcons.TERMINAL) {
            override fun actionPerformed(e: AnActionEvent) {
                toggleTerminalAction()
            }
        }

        val copyText = AutoDevBundle.message("sketch.terminal.copy.text")
        val copyAction = object : AnAction(copyText, copyText, AutoDevIcons.COPY) {
            override fun actionPerformed(e: AnActionEvent) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val textToCopy = if (hasExecutionResults) {
                    lastExecutionResults
                } else {
                    getViewText()
                }

                val selection = StringSelection(textToCopy)
                clipboard.setContents(selection, null)
            }
        }

        val sendText = AutoDevBundle.message("sketch.terminal.send.chat")
        val sendAction = object : AnAction(sendText, sendText, AutoDevIcons.SEND) {
            override fun actionPerformed(e: AnActionEvent) {
                try {
                    val output = if (hasExecutionResults) {
                        lastExecutionResults
                    } else {
                        terminalWidget!!::class.java.getMethod("getText")
                            .invoke(terminalWidget) as String
                    }
                    sendToSketch(project, "Help me to solve this issue:\n```bash\n$output\n```\n")
                } catch (e: Exception) {
                    AutoDevNotifications.notify(project, "Failed to send to Sketch")
                }
            }
        }

        val popup = AutoDevBundle.message("sketch.terminal.popup")
        val popupAction = object : AnAction(popup, popup, AllIcons.Ide.External_link_arrow) {
            override fun displayTextInToolbar(): Boolean = true

            override fun actionPerformed(e: AnActionEvent) {
                executePopup(terminalWidget, project).mouseClicked(null)
            }
        }

        return listOf(executeAction, showTerminalAction, copyAction, sendAction, popupAction)
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
        val title = content.split("\n").firstOrNull() ?: content
        titleLabel.text = "Terminal - ($title)"

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
                setResultStatus(TerminalExecutionState.TERMINATED, reason)
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

            if (!enableAutoRunTerminal || !::executeAction.isInitialized) {
                return@invokeLater
            }

            ActionUtil.invokeAction(executeAction, SimpleDataContext.getProjectContext(project), "AutoExecuteTerminal", null , null)
        }
    }

    override fun getComponent(): JComponent = mainPanel!!
    override fun updateLanguage(language: Language?, originLanguage: String?) {}
    override fun dispose() {
        codeSketch.dispose()
        resultSketch.dispose()
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
