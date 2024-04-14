package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.model.LineInfo
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.util.parser.Code
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class WriteInsCommand(val myProject: Project, val argument: String, val content: String) : InsCommand {
    private val pathSeparator = "/"

    override suspend fun execute(): String? {
        val content = Code.parse(content).text

        val range: LineInfo? = LineInfo.fromString(argument)
        val filepath = argument.split("#")[0]
        val projectDir = myProject.guessProjectDir() ?: return "$DEVINS_ERROR: Project directory not found"

        val virtualFile = myProject.lookupFile(filepath)
        if (virtualFile == null) {
            // filepath maybe just a file name, so we need to create parent directory
            val hasChildPath = filepath.contains(pathSeparator)
            if (hasChildPath) {
                val parentPath = filepath.substringBeforeLast(pathSeparator)
                var parentDir = projectDir.findChild(parentPath)
                if (parentDir == null) {
                    // parentDir maybe multiple level, so we need to create all parent directory
                    val parentDirs = parentPath.split(pathSeparator)
                    parentDir = projectDir
                    for (dir in parentDirs) {
                        if (dir.isEmpty()) continue
                        parentDir = runWriteAction { parentDir?.createChildDirectory(this, dir) }
                    }

                    if (parentDir == null) {
                        return "$DEVINS_ERROR: Create Directory failed: $parentPath"
                    }
                }

                return runWriteAction {
                    createNewContent(parentDir, filepath, content)
                        ?: return@runWriteAction "$DEVINS_ERROR: Create File failed: $argument"

                    return@runWriteAction "Create file: $argument"
                }
            } else {
                return runWriteAction {
                    createNewContent(projectDir, filepath, content)
                        ?: return@runWriteAction "$DEVINS_ERROR: Create File failed: $argument"

                    return@runWriteAction "Create file: $argument"
                }
            }
        }

        val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile)
            ?: return "$DEVINS_ERROR: File not found: $argument"

        return executeInsert(psiFile, range, content)
    }

    private fun createNewContent(parentDir: VirtualFile, filepath: String, content: String): Document? {
        val newFile = parentDir.createChildData(this, filepath.substringAfterLast(pathSeparator))
        val document = FileDocumentManager.getInstance().getDocument(newFile) ?: return null

        document.setText(content)
        return document
    }

    private fun executeInsert(
        psiFile: PsiFile,
        range: LineInfo?,
        content: String
    ): String {
        val document = runReadAction {
            PsiDocumentManager.getInstance(myProject).getDocument(psiFile)
        } ?: return "$DEVINS_ERROR: File not found: $argument"

        val startLine = range?.startLine ?: 0
        val endLine = range?.endLine ?: document.lineCount

        try {
            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine - 1)
            WriteCommandAction.runWriteCommandAction(myProject) {
                document.replaceString(startOffset, endOffset, content)
            }

            return "Writing to file: $argument"
        } catch (e: Exception) {
            return "$DEVINS_ERROR: ${e.message}"
        }
    }
}
