package cc.unitmesh.devti.language.compiler.processor

import com.intellij.psi.PsiElement

/**
 * Interface for processing different types of DevIn PSI elements
 */
interface DevInElementProcessor {
    /**
     * Process a PSI element within the given context
     * @param element The PSI element to process
     * @param context The compilation context
     * @return ProcessResult indicating success/failure and whether to continue
     */
    suspend fun process(element: PsiElement, context: CompilerContext): ProcessResult
    
    /**
     * Check if this processor can handle the given element
     * @param element The PSI element to check
     * @return true if this processor can handle the element
     */
    fun canProcess(element: PsiElement): Boolean
}
