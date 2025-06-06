package cc.unitmesh.devti.command

import cc.unitmesh.devti.bridge.knowledge.lookupFile
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.sketch.ui.patch.writeText
import cc.unitmesh.devti.sketch.ui.patch.createPatchFromCode
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.util.concurrent.CompletableFuture

/**
 * Core edit file functionality that can be used across modules
 */
class EditFileCommand(private val project: Project) {

    /**
     * Apply edit to a file and return the patch content
     */
    fun executeEdit(editRequest: EditRequest): EditResult {
        val projectDir = project.guessProjectDir() ?: return EditResult.error("Project directory not found")

        val targetFile = findTargetFile(editRequest.targetFile, projectDir)
            ?: return EditResult.error("File not found: ${editRequest.targetFile}")

        return try {
            val originalContent = targetFile.readText()
            val editedContent = applyEdit(originalContent, editRequest.codeEdit)

            // Write the edited content to file using EDT
            val future = CompletableFuture<String>()
            runInEdt {
                try {
                    WriteAction.compute<Unit, Throwable> {
                        targetFile.writeText(editedContent)
                    }
                    future.complete("success")
                } catch (e: Exception) {
                    future.complete("error: ${e.message}")
                }
            }

            val writeResult = future.get()
            if (writeResult.startsWith("error:")) {
                return EditResult.error("Failed to write file: ${writeResult.substring(7)}")
            }

            // Generate patch for display
            val patch = createPatchFromCode(originalContent, editedContent)
            val patchContent = if (patch != null) {
                generatePatchContent(editRequest.targetFile, patch)
            } else {
                "No changes detected"
            }

            EditResult.success("File edited successfully: ${editRequest.targetFile}", patchContent)
        } catch (e: Exception) {
            EditResult.error("Failed to apply edit to ${editRequest.targetFile}: ${e.message}")
        }
    }

    private fun findTargetFile(targetPath: String, projectDir: VirtualFile): VirtualFile? {
        return runReadAction {
            // Try relative path first
            projectDir.findFileByRelativePath(targetPath)
                ?: project.lookupFile(targetPath)
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
                    originalLines[originalIndex].trim() == trimmedEditLine
                ) {
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

    private fun generatePatchContent(fileName: String, patch: TextFilePatch): String {
        val sb = StringBuilder()
        sb.append("--- a/$fileName\n")
        sb.append("+++ b/$fileName\n")

        patch.hunks.forEach { hunk ->
            sb.append("@@ -${hunk.startLineBefore},${hunk.endLineBefore - hunk.startLineBefore + 1} ")
            sb.append("+${hunk.startLineAfter},${hunk.endLineAfter - hunk.startLineAfter + 1} @@\n")

            hunk.lines.forEach { line ->
                when (line.type) {
                    com.intellij.openapi.diff.impl.patch.PatchLine.Type.CONTEXT -> sb.append(" ${line.text}\n")
                    com.intellij.openapi.diff.impl.patch.PatchLine.Type.REMOVE -> sb.append("-${line.text}\n")
                    com.intellij.openapi.diff.impl.patch.PatchLine.Type.ADD -> sb.append("+${line.text}\n")
                }
            }
        }

        return sb.toString()
    }

    /**
     * Parse edit_file function call format using YAML parsing for better handling of quotes and special characters
     */
    fun parseEditRequest(content: String): EditRequest? {
        return try {
            parseAsYaml(content) ?: parseAsLegacyFormat(content)
        } catch (e: Exception) {
            parseAsLegacyFormat(content)
        }
    }

    /**
     * Parse content as YAML format (recommended approach)
     */
    private fun parseAsYaml(content: String): EditRequest? {
        return try {
            val yaml = Yaml(SafeConstructor(LoaderOptions()))
            val data = yaml.load<Map<String, Any>>(content) ?: return null

            val targetFile = data["target_file"] as? String ?: return null
            val instructions = data["instructions"] as? String ?: ""
            val codeEdit = data["code_edit"] as? String ?: return null

            EditRequest(
                targetFile = targetFile,
                instructions = instructions,
                codeEdit = codeEdit
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse content using legacy regex format for backward compatibility
     */
    private fun parseAsLegacyFormat(content: String): EditRequest? {
        return try {
            // Parse the edit_file function call format - handle both JSON-like and function parameter formats
            val targetFileRegex = """target_file["\s]*[:=]["\s]*["']([^"']+)["']""".toRegex()
            val instructionsRegex =
                """instructions["\s]*[:=]["\s]*["']([^"']*?)["']""".toRegex(RegexOption.DOT_MATCHES_ALL)

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
}

data class EditRequest(
    val targetFile: String,
    val instructions: String,
    val codeEdit: String
)

sealed class EditResult {
    data class Success(val message: String, val patchContent: String) : EditResult()
    data class Error(val message: String) : EditResult()

    companion object {
        fun success(message: String, patchContent: String) = Success(message, patchContent)
        fun error(message: String) = Error(message)
    }
}
