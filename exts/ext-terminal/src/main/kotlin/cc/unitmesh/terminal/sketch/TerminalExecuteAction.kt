package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.run.ProcessExecutor
import cc.unitmesh.devti.sketch.run.UIUpdatingWriter
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.jetbrains.ide.PooledThreadExecutor

class TerminalExecuteAction(
    private val sketch: TerminalLangSketch
) : AnAction("Execute", AutoDevBundle.message("sketch.terminal.execute"), AutoDevIcons.RUN) {

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (sketch.isExecuting) {
            e.presentation.icon = AllIcons.Actions.Suspend
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
            // 如果正在执行，则停止执行
            sketch.currentExecutionJob?.cancel()

            ApplicationManager.getApplication().invokeLater {
                sketch.isExecuting = false
                sketch.titleLabel.icon = null

                // 更新UI以反映停止状态
                sketch.resultSketch.updateViewText(sketch.lastExecutionResults + "\n\n[执行已手动停止]", true)
                sketch.setResultStatus(false, "执行已手动停止")

                // 更新工具栏
                sketch.actionGroup.update(e)
                sketch.toolbar.updateActionsImmediately()
            }
            return
        }

        // 开始执行
        sketch.isExecuting = true
        sketch.titleLabel.icon = AllIcons.RunConfigurations.TestState.Run

        // 更新工具栏以显示停止按钮
        sketch.actionGroup.update(e)
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
        sketch.setResultStatus(false)

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

                    // 更新工具栏以显示执行按钮
                    sketch.actionGroup.update(e)
                    sketch.toolbar.updateActionsImmediately()

                    val success = exitCode == 0
                    sketch.setResultStatus(success, if (!success) "Process exited with code $exitCode" else null)
                }
            } catch (ex: Exception) {
                AutoDevNotifications.notify(sketch.project, "Error executing command: ${ex.message}")
                ApplicationManager.getApplication().invokeLater {
                    stdWriter.setExecuting(false)
                    // Clear the running icon.
                    sketch.titleLabel.icon = null
                    sketch.isExecuting = false

                    // 更新工具栏以显示执行按钮
                    sketch.actionGroup.update(e)
                    sketch.toolbar.updateActionsImmediately()

                    sketch.resultSketch.updateViewText("${stdWriter.getContent()}\nError: ${ex.message}", true)
                    sketch.setResultStatus(false, ex.message)
                }
            }
        }
    }
}