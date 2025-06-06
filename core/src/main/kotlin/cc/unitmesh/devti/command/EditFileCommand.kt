package cc.unitmesh.devti.command

import cc.unitmesh.devti.bridge.knowledge.lookupFile
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.sketch.ui.patch.writeText
import cc.unitmesh.devti.sketch.ui.patch.createPatchFromCode
import cc.unitmesh.devti.util.relativePath
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

class EditFileCommand(private val project: Project) {
    private val editApply = EditApply()

    fun executeEdit(editRequest: EditRequest): EditResult {
        val projectDir = project.guessProjectDir() ?: return EditResult.error("Project directory not found")

        val targetFile = findTargetFile(editRequest.targetFile, projectDir)
            ?: return EditResult.error("File not found: ${editRequest.targetFile}")

        return try {
            val originalContent = targetFile.readText()
            val editedContent = editApply.applyEdit(originalContent, editRequest.codeEdit)

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
                ?: return EditResult.error("No changes detected in ${editRequest.targetFile}")

            patch.beforeName = targetFile.relativePath(project)
            patch.afterName = targetFile.relativePath(project)

            EditResult.success("File edited successfully: ${editRequest.targetFile}", patch, targetFile)
        } catch (e: Exception) {
            EditResult.error("Failed to apply edit to ${editRequest.targetFile}: ${e.message}")
        }
    }

    private fun findTargetFile(targetPath: String, projectDir: VirtualFile): VirtualFile? {
        return runReadAction {
            projectDir.findFileByRelativePath(targetPath)
                ?: project.lookupFile(targetPath)
        }
    }

    fun parseEditRequest(content: String): EditRequest? {
        return try {
            parseAsYaml(content) ?: parseAsLegacyFormat(content)
        } catch (e: Exception) {
            parseAsLegacyFormat(content)
        }
    }

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

    private fun parseAsLegacyFormat(content: String): EditRequest? {
        return try {
            val targetFileRegex = """target_file["\s]*[:=]["\s]*["']([^"']+)["']""".toRegex()
            val instructionsRegex =
                """instructions["\s]*[:=]["\s]*["']([^"']*?)["']""".toRegex(RegexOption.DOT_MATCHES_ALL)

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
    data class Success(val message: String, val patch: TextFilePatch, val targetFile: VirtualFile) : EditResult()
    data class Error(val message: String) : EditResult()

    companion object {
        fun success(message: String, patch: TextFilePatch, targetFile: VirtualFile) = Success(message, patch, targetFile)
        fun error(message: String) = Error(message)
    }
}
