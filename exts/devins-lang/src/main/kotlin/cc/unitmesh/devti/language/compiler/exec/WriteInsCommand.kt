package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.model.LineInfo
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.util.parser.Code
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

class WriteInsCommand(val myProject: Project, val argument: String, val content: String) : InsCommand {
    override suspend fun execute(): String? {
        val content = Code.parse(content).text

        val range: LineInfo? = LineInfo.fromString(argument)
        val filename = argument.split("#")[0]

        val virtualFile = myProject.lookupFile(filename) ?: return "$DEVINS_ERROR: File not found: $argument"
        val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile)
            ?: return "$DEVINS_ERROR: File not found: $argument"

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