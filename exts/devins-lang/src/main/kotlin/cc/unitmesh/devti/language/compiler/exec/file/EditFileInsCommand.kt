package cc.unitmesh.devti.language.compiler.exec.file

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.EditFileCommand
import cc.unitmesh.devti.command.EditResult
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.sketch.AutoSketchMode
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class EditFileInsCommand(val myProject: Project, val prop: String, val codeContent: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.EDIT_FILE

    override suspend fun execute(): String? {
        val editFileCommand = EditFileCommand(myProject)
        val editRequest = editFileCommand.parseEditRequest(codeContent)

        if (editRequest == null) {
            val shouldShowNotification = shouldShowParseErrorNotification()
            if (shouldShowNotification) {
                AutoDevNotifications.warn(myProject, "Failed to parse edit_file request from content")
            }
            return "$DEVINS_ERROR: Failed to parse edit_file request"
        }

        val result = editFileCommand.executeEdit(editRequest)

        return when (result) {
            is EditResult.Success -> {
                // Open the file in editor using EDT
                runInEdt {
                    FileEditorManager.getInstance(myProject).openFile(result.targetFile, true)
                }

                if (AutoSketchMode.getInstance(myProject).isEnable) {
                    ""
                } else {
                    result.message
                }
            }
            is EditResult.Error -> {
                "$DEVINS_ERROR: ${result.message}"
            }
        }
    }



    private fun shouldShowParseErrorNotification(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastErrorTime > ERROR_TIME_WINDOW_MS) {
            parseErrorCount = 0
            lastErrorTime = currentTime
        }

        parseErrorCount++

        return parseErrorCount <= MAX_ERRORS_IN_WINDOW
    }

    companion object {
        private const val MAX_ERRORS_IN_WINDOW = 3
        private const val ERROR_TIME_WINDOW_MS = 60000L
        private var parseErrorCount = 0
        private var lastErrorTime = 0L
    }
}
