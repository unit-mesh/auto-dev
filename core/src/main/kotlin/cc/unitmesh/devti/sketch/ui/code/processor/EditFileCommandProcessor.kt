package cc.unitmesh.devti.sketch.ui.code.processor

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.EditResult
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.ui.patch.SingleFileDiffSketch
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.launch
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Processor for handling edit file command operations
 */
class EditFileCommandProcessor(private val project: Project) {

    /**
     * Process edit file command and return UI panel with callback for adding diff sketch
     */
    fun processEditFileCommand(
        currentText: String,
        onDiffSketchCreated: (SingleFileDiffSketch) -> Unit
    ): JPanel {
        val isAutoSketchMode = AutoSketchMode.getInstance(project).isEnable

        val button = createEditButton(isAutoSketchMode)

        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(button)
        }

        val executeCommand = {
            executeEditFileCommand(currentText, button, onDiffSketchCreated)
        }

        if (isAutoSketchMode) {
            executeCommand()
        } else {
            button.addActionListener { executeCommand() }
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
        onDiffSketchCreated: (SingleFileDiffSketch) -> Unit
    ) {
        val isAutoSketchMode = AutoSketchMode.getInstance(project).isEnable

        button.isEnabled = false
        button.text = if (isAutoSketchMode) "Auto Executing..." else "Executing..."
        button.icon = AutoDevIcons.LOADING

        AutoDevCoroutineScope.scope(project).launch {
            executeEditFileCommandAsync(project, currentText) { result ->
                runInEdt {
                    handleExecutionResult(result, button, onDiffSketchCreated)
                }
            }
        }
    }

    private fun handleExecutionResult(
        result: EditResult?,
        button: JButton,
        onDiffSketchCreated: (SingleFileDiffSketch) -> Unit
    ) {
        when (result) {
            is EditResult.Success -> {
                val diffSketch = createSingleFileDiffSketch(result.targetFile, result.patch)
                onDiffSketchCreated(diffSketch)
                button.text = "Executed"
                button.icon = AllIcons.Actions.Checked
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

    private fun createSingleFileDiffSketch(virtualFile: VirtualFile, patch: TextFilePatch): SingleFileDiffSketch {
        return SingleFileDiffSketch(project, virtualFile, patch) {
        }.apply {
            this.onComplete("")
        }
    }

    private suspend fun executeEditFileCommandAsync(
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
                // the first codefence should be `/edit_file` we can skip it
                if (editRequest == null) {
                    return
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
                        callback(result)
                        return
                    }
                }
            }

            callback(EditResult.error("No valid edit_file commands found"))
        } catch (e: Exception) {
            callback(EditResult.error("Execution failed: ${e.message}"))
        }
    }
}
