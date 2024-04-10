package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.model.LineInfo
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.util.parser.Code
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File

class WriteInsCommand(val myProject: Project, val argument: String, val content: String) : InsCommand {
    override suspend fun execute(): String? {
        val content = Code.parse(content).text

        val range: LineInfo? = LineInfo.fromString(argument)
        val filepath = argument.split("#")[0]
        val projectDir = myProject.guessProjectDir() ?: return "$DEVINS_ERROR: Project directory not found"

        val virtualFile = myProject.lookupFile(filepath)
        if (virtualFile == null) {
            return runWriteAction {
                val parentPath = filepath.substringBeforeLast(File.separator)
                var parentDir = projectDir.findFileOrDirectory(parentPath)
                if (parentDir == null) {
                    parentDir = projectDir.createChildDirectory(this, parentPath)
                }
                val newFile = parentDir.createChildData(this, filepath.substringAfterLast(File.separator))
                val document = FileDocumentManager.getInstance().getDocument(newFile)
                    ?: return@runWriteAction "$DEVINS_ERROR: Create File failed: $argument"

                document.setText(content)

                return@runWriteAction "Create file: $argument"
            }
        }

        val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile)
            ?: return "$DEVINS_ERROR: File not found: $argument"
        return executeInsert(psiFile, range, content)
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
