package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.run.ProcessExecutor
import cc.unitmesh.devti.sketch.run.UIUpdatingWriter
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.jetbrains.ide.PooledThreadExecutor

class TerminalExecuteAction(
    private val sketch: TerminalLangSketch
) : AnAction("Execute", AutoDevBundle.message("sketch.terminal.execute"), AutoDevIcons.RUN) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (sketch.isExecuting) {
            e.presentation.icon = AutoDevIcons.STOP
            e.presentation.text = "Stop"
            e.presentation.description = AutoDevBundle.message("sketch.terminal.stop")
        } else {
            e.presentation.icon = AutoDevIcons.RUN
            e.presentation.text = "Execute"
            e.presentation.description = AutoDevBundle.message("sketch.terminal.execute")
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (sketch.isExecuting) {
            sketch.currentExecutionJob?.cancel()

            ApplicationManager.getApplication().invokeLater {
                sketch.isExecuting = false
                sketch.titleLabel.icon = null

                sketch.resultSketch.updateViewText(sketch.lastExecutionResults + "\n\n[执行已手动停止]", true)
                sketch.setResultStatus(TerminalExecutionState.TERMINATED)

                sketch.toolbar.updateActionsImmediately()
            }
            return
        }

        sketch.isExecuting = true
        sketch.titleLabel.icon = AllIcons.RunConfigurations.TestState.Run

        sketch.toolbar.updateActionsImmediately()

        sketch.hasExecutionResults = false
        sketch.lastExecutionResults = ""

        val stdWriter = UIUpdatingWriter(
            onTextUpdate = { text, complete ->
                sketch.resultSketch.updateViewText(text, complete)
                sketch.lastExecutionResults = text
            },
            onPanelUpdate = { title, _ ->
                sketch.collapsibleResultPanel.setTitle(title)
            },
            checkCollapsed = {
                sketch.collapsibleResultPanel.isCollapsed()
            },
            expandPanel = {
                sketch.collapsibleResultPanel.expand()
            }
        )

        sketch.resultSketch.updateViewText("", true)
        stdWriter.setExecuting(true)
        sketch.setResultStatus(TerminalExecutionState.EXECUTING)

        sketch.currentExecutionJob = AutoDevCoroutineScope.Companion.scope(sketch.project).launch {
            val executor = sketch.project.getService(ProcessExecutor::class.java)
            try {
                val dispatcher = PooledThreadExecutor.INSTANCE.asCoroutineDispatcher()
                val exitCode = executor.exec(sketch.getViewText(), stdWriter, stdWriter, dispatcher)
                ApplicationManager.getApplication().invokeLater {
                    stdWriter.setExecuting(false)
                    if (sketch.collapsibleResultPanel.isCollapsed()) {
                        sketch.collapsibleResultPanel.expand()
                    }

                    val content = stdWriter.getContent()
                    sketch.lastExecutionResults = content
                    sketch.hasExecutionResults = true

                    sketch.titleLabel.icon = null
                    sketch.isExecuting = false

                    sketch.toolbar.updateActionsImmediately()

                    val success = exitCode == 0
                    sketch.setResultStatus(
                        if (success) TerminalExecutionState.SUCCESS else TerminalExecutionState.FAILED,
                        if (!success) "进程退出码 $exitCode" else null
                    )
                }
            } catch (ex: Exception) {
                AutoDevNotifications.notify(sketch.project, "执行命令时出错: ${ex.message}")
                ApplicationManager.getApplication().invokeLater {
                    stdWriter.setExecuting(false)

                    sketch.titleLabel.icon = null
                    sketch.isExecuting = false

                    sketch.toolbar.updateActionsImmediately()

                    sketch.resultSketch.updateViewText("${stdWriter.getContent()}\n错误: ${ex.message}", true)
                    sketch.setResultStatus(TerminalExecutionState.FAILED, ex.message)
                }
            }
        }
    }
}