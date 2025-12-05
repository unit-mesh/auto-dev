package cc.unitmesh.devti.sketch.ui.code.processor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.EditResult
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.ui.patch.DiffLangSketch
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Processor for handling edit file command operations
 */
class EditFileCommandProcessor(private val project: Project) {
    private val logger = logger<EditFileCommandProcessor>()

    /**
     * Process edit file command and return UI panel with callback for adding diff sketch
     */
    fun processEditFileCommand(
        currentText: String,
        onDiffSketchCreated: (DiffLangSketch) -> Unit
    ): JPanel {
        val isAutoSketchMode = AutoSketchMode.getInstance(project).isEnable
        val button = createEditButton(isAutoSketchMode)
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(button)
        }

        if (isAutoSketchMode) {
            executeEditFileCommand(currentText, button, panel, onDiffSketchCreated)
        } else {
            button.addActionListener {
                executeEditFileCommand(currentText, button, panel, onDiffSketchCreated)
            }
        }

        return panel
    }

    private fun createEditButton(isAutoSketchMode: Boolean): JButton {
        return if (isAutoSketchMode) {
            JButton("Auto Executing...", AutoDevIcons.LOADING).apply {
                isEnabled = false
                preferredSize = JBUI.size(150, 30)
            }
        } else {
            JButton("Execute Edit File", AllIcons.Actions.Execute).apply {
                preferredSize = JBUI.size(120, 30)
            }
        }
    }

    private fun executeEditFileCommand(
        currentText: String,
        button: JButton,
        panel: JPanel,
        onDiffSketchCreated: (DiffLangSketch) -> Unit
    ) {
        val isAutoSketchMode = AutoSketchMode.getInstance(project).isEnable
        button.isEnabled = false
        button.text = if (isAutoSketchMode) "Auto Executing..." else "Executing..."
        button.icon = AutoDevIcons.LOADING

        executeEditFileCommandAsync(project, currentText) { result ->
            runInEdt {
                handleExecutionResult(result, button, panel, onDiffSketchCreated)
            }
        }
    }

    private fun handleExecutionResult(
        result: EditResult?,
        button: JButton,
        panel: JPanel,
        onDiffSketchCreated: (DiffLangSketch) -> Unit
    ) {
        when (result) {
            is EditResult.Success -> {
                val diffSketch = createDiffLangSketch(result.patch)
                onDiffSketchCreated(diffSketch)
                panel.remove(button)
                panel.revalidate()
                panel.repaint()
            }

            is EditResult.Error -> {
                button.text = "Failed"
                button.icon = AllIcons.General.Error
                AutoDevNotifications.warn(project, result.message)
            }

            null -> {
                button.text = "Failed"
                button.icon = AllIcons.General.Error
                AutoDevNotifications.warn(project, "Unknown error occurred")
            }
        }
    }

    private fun createDiffLangSketch(patch: TextFilePatch): DiffLangSketch {
        return DiffLangSketch(project, patch)
    }

    fun executeEditFileCommandAsync(
        project: Project,
        currentText: String,
        callback: (EditResult?) -> Unit
    ) {
        try {
            val codeFences = CodeFence.parseAll(currentText)
            if (codeFences.isEmpty()) {
                callback(EditResult.error("No edit_file commands found in content"))
                return
            }

            val editFileCommand = cc.unitmesh.devti.command.EditFileCommand(project)
            for (codeFence in codeFences) {
                val editRequest = editFileCommand.parseEditRequest(codeFence.text)
                if (editRequest == null) {
                    continue
                }

                val result = editFileCommand.executeEdit(editRequest)
                when (result) {
                    is EditResult.Success -> {
                        runInEdt {
                            FileEditorManager.getInstance(project).openFile(result.targetFile, true)
                        }
                        callback(result)
                        return
                    }

                    is EditResult.Error -> {
                        logger.error("编辑失败: ${result.message}")
                        callback(result)
                        return
                    }
                }
            }
            callback(EditResult.error("No valid edit_file commands found"))
        } catch (e: Exception) {
            logger.error("执行过程中发生异常", e)
            callback(EditResult.error("Execution failed: ${e.message}"))
        }
    }
}