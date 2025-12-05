package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.custom.compile.VariableTemplateCompiler
import cc.unitmesh.devti.language.ast.variable.VariableTable
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

/**
 * Processor for variables
 */
class VariableProcessor {
    
    fun processVariable(variableStart: PsiElement, context: CompilerContext): ProcessResult {
        if (variableStart.elementType != DevInTypes.VARIABLE_START) {
            context.logger.warn("Illegal type: ${variableStart.elementType}")
            return ProcessResult(success = false, errorMessage = "Illegal variable start type")
        }
        
        val variableId = runReadAction { variableStart.nextSibling?.text }
        
        val currentEditor = context.editor ?: VariableTemplateCompiler.defaultEditor(context.project)
        val currentElement = context.element ?: VariableTemplateCompiler.defaultElement(context.project, currentEditor)
        
        if (currentElement == null) {
            val errorMsg = "${DEVINS_ERROR} No element found for variable: ${variableStart.text}"
            context.appendOutput(errorMsg)
            context.setError(true)
            return ProcessResult(success = false, errorMessage = errorMsg)
        }
        
        val lineNo = try {
            runReadAction {
                val containingFile = currentElement.containingFile
                val document: Document? = PsiDocumentManager.getInstance(variableStart.project).getDocument(containingFile)
                document?.getLineNumber(variableStart.textRange.startOffset) ?: 0
            }
        } catch (e: Exception) {
            0
        }
        
        context.variableTable.addVariable(variableId ?: "", VariableTable.VariableType.String, lineNo)
        return ProcessResult(success = true)
    }
}
