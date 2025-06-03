package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

/**
 * Processor for code blocks
 */
class CodeProcessor : DevInElementProcessor {
    
    override suspend fun process(element: PsiElement, context: CompilerContext): ProcessResult {
        if (context.skipNextCode) {
            context.skipNextCode = false
            return ProcessResult(success = true)
        }
        
        val text = runReadAction { element.text }
        context.appendOutput(text)
        
        return ProcessResult(success = true)
    }
    
    override fun canProcess(element: PsiElement): Boolean {
        return element.elementType == DevInTypes.CODE
    }
}
