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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.CompletableFuture

class EditFileCommand(private val project: Project) {
    private val editApply = EditApply()
    private val parser = EditRequestParser()

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
        return parser.parse(content)
    }
}

@Serializable
data class EditRequest(
    @SerialName("target_file")
    val targetFile: String,
    @SerialName("instructions")
    val instructions: String,
    @SerialName("code_edit")
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