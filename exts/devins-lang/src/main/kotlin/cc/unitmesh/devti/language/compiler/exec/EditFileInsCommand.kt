package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.sketch.ui.patch.writeText
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile

class EditFileInsCommand(val myProject: Project, val prop: String, val codeContent: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.EDIT_FILE

    override suspend fun execute(): String? {
        val editRequest = parseEditRequest(codeContent)
        if (editRequest == null) {
            val shouldShowNotification = shouldShowParseErrorNotification()
            if (shouldShowNotification) {
                AutoDevNotifications.warn(myProject, "Failed to parse edit_file request from content")
            }
            return "$DEVINS_ERROR: Failed to parse edit_file request"
        }

        val disposable = Disposer.newCheckedDisposable()
        var result: String? = null

        runInEdtAsync(disposable) {
            val projectDir = myProject.guessProjectDir()
            if (projectDir == null) {
                result = "$DEVINS_ERROR: Project directory not found"
                return@runInEdtAsync
            }

            val targetFile = findTargetFile(editRequest.targetFile, projectDir)
            if (targetFile == null) {
                result = "$DEVINS_ERROR: File not found: ${editRequest.targetFile}"
                return@runInEdtAsync
            }

            try {
                val originalContent = targetFile.readText()
                val editedContent = applyEdit(originalContent, editRequest.codeEdit)
                
                targetFile.writeText(editedContent)
                FileEditorManager.getInstance(myProject).openFile(targetFile, true)
                
                result = "File edited successfully: ${editRequest.targetFile}"
            } catch (e: Exception) {
                result = "$DEVINS_ERROR: Failed to apply edit to ${editRequest.targetFile}: ${e.message}"
            }
        }

        if (AutoSketchMode.getInstance(myProject).isEnable) {
            result = ""
        }

        return result
    }

    private fun findTargetFile(targetPath: String, projectDir: VirtualFile): VirtualFile? {
        return runReadAction {
            // Try relative path first
            projectDir.findFileByRelativePath(targetPath) 
                ?: myProject.lookupFile(targetPath)
        }
    }

    private fun applyEdit(originalContent: String, codeEdit: String): String {
        val originalLines = originalContent.lines()
        val editLines = codeEdit.lines()

        // Simple approach: replace the entire content with the edit
        // The edit should contain the complete intended content with markers
        val result = mutableListOf<String>()
        var originalIndex = 0

        for (editLine in editLines) {
            val trimmedEditLine = editLine.trim()

            // Check if this is an "existing code" marker
            if (isExistingCodeMarker(trimmedEditLine)) {
                // Find the next non-marker line in the edit to know where to stop copying
                val nextEditLineIndex = findNextNonMarkerLine(editLines, editLines.indexOf(editLine) + 1)
                val nextEditLine = if (nextEditLineIndex >= 0) editLines[nextEditLineIndex].trim() else null

                // Copy original lines until we find the next edit line or reach the end
                while (originalIndex < originalLines.size) {
                    val originalLine = originalLines[originalIndex]
                    result.add(originalLine)
                    originalIndex++

                    // If we found the next edit line in the original, stop copying
                    if (nextEditLine != null && originalLine.trim() == nextEditLine) {
                        originalIndex-- // Back up one so the next edit line replaces this one
                        break
                    }
                }
            } else {
                // This is an actual edit line - add it and skip any matching original line
                result.add(editLine)

                // Skip the corresponding original line if it matches
                if (originalIndex < originalLines.size &&
                    originalLines[originalIndex].trim() == trimmedEditLine) {
                    originalIndex++
                }
            }
        }

        return result.joinToString("\n")
    }

    private fun isExistingCodeMarker(line: String): Boolean {
        return line.startsWith("//") &&
               (line.contains("existing code") || line.contains("... existing code ..."))
    }

    private fun findNextNonMarkerLine(lines: List<String>, startIndex: Int): Int {
        for (i in startIndex until lines.size) {
            if (!isExistingCodeMarker(lines[i].trim())) {
                return i
            }
        }
        return -1
    }

    private fun parseEditRequest(content: String): EditRequest? {
        return try {
            // Parse the edit_file function call format - handle both JSON-like and function parameter formats
            val targetFileRegex = """target_file["\s]*[:=]["\s]*["']([^"']+)["']""".toRegex()
            val instructionsRegex = """instructions["\s]*[:=]["\s]*["']([^"']*?)["']""".toRegex(RegexOption.DOT_MATCHES_ALL)

            // For code_edit, we need to handle multiline content more carefully
            val codeEditPattern = """code_edit["\s]*[:=]["\s]*["'](.*?)["']""".toRegex(RegexOption.DOT_MATCHES_ALL)

            val targetFileMatch = targetFileRegex.find(content)
            val instructionsMatch = instructionsRegex.find(content)
            val codeEditMatch = codeEditPattern.find(content)

            if (targetFileMatch != null && codeEditMatch != null) {
                val codeEditContent = codeEditMatch.groupValues[1]
                    .replace("\\n", "\n")  // Handle escaped newlines
                    .replace("\\\"", "\"") // Handle escaped quotes
                    .replace("\\'", "'")   // Handle escaped single quotes

                EditRequest(
                    targetFile = targetFileMatch.groupValues[1],
                    instructions = instructionsMatch?.groupValues?.get(1) ?: "",
                    codeEdit = codeEditContent
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
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

data class EditRequest(
    val targetFile: String,
    val instructions: String,
    val codeEdit: String
)
