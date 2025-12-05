package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.language.ast.variable.VariableTable
import cc.unitmesh.devti.language.compiler.DevInsCompiledResult
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * Context object that holds all the state needed during compilation
 */
data class CompilerContext(
    val project: Project,
    val file: DevInFile,
    val editor: Editor? = null,
    val element: PsiElement? = null,
    val output: StringBuilder = StringBuilder(),
    val result: DevInsCompiledResult = DevInsCompiledResult(),
    val variableTable: VariableTable = VariableTable(),
    val logger: Logger,
    var skipNextCode: Boolean = false
) {
    fun appendOutput(text: String) {
        output.append(text)
    }
    
    fun setError(hasError: Boolean) {
        result.hasError = hasError
    }
    
    fun isError(): Boolean = result.hasError
}

/**
 * Result of processing a PSI element
 */
data class ProcessResult(
    val success: Boolean = true,
    val errorMessage: String? = null,
    val shouldContinue: Boolean = true
)
