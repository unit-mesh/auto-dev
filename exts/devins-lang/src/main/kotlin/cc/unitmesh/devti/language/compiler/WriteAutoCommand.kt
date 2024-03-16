package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.compiler.data.LineInfo
import cc.unitmesh.devti.language.compiler.utils.lookupFile
import cc.unitmesh.devti.util.parser.Code
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

class WriteAutoCommand(val myProject: Project, val prop: String, val content: String) : AutoCommand {
    override fun execute(): String? {
        val content = Code.parse(content).text

        val range: LineInfo? = LineInfo.fromString(prop)

        // prop name can be src/file.name#L1-L2
        val filename = prop.split("#")[0]

        try {
            val virtualFile = myProject.lookupFile(filename) ?: return "<DevliError>: File not found: $prop"
            val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile)
                ?: return "<DevliError>: File not found: $prop"
            val document = PsiDocumentManager.getInstance(myProject).getDocument(psiFile)
                ?: return "<DevliError>: File not found: $prop"

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(myProject) {
                    val startLine = range?.startLine ?: 0
                    val endLine = range?.endLine ?: document.lineCount

                    val startOffset = document.getLineStartOffset(startLine)
                    val endOffset = document.getLineEndOffset(endLine - 1)

                    document.replaceString(startOffset, endOffset, content)
                }
            }

            return "Writing to file: $prop"
        } catch (e: Exception) {
            return "<DevliError>: ${e.message}"
        }
    }
}