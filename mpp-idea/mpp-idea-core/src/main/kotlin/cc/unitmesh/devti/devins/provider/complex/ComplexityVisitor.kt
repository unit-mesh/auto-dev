/**
 * The MIT License (MIT)
 * <p>
 *     https://github.com/nikolaikopernik/code-complexity-plugin
 *  </p>
 */
package cc.unitmesh.devti.devins.provider.complex

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor

abstract class ComplexityVisitor: PsiRecursiveElementVisitor(true) {
    override fun visitElement(element: PsiElement) {
        processElement(element)
        if (shouldVisitElement(element)) {
            super.visitElement(element)
        }
        postProcess(element)
    }

    /**
     * Increases complexity and nesting
     */
    protected abstract fun processElement(element: PsiElement)

    /**
     * Decreases nesting
     */
    protected abstract fun postProcess(element: PsiElement)

    /**
     * @return true if PsiElement is Binary Expression with operations || or &&
     */
    protected abstract fun shouldVisitElement(element: PsiElement): Boolean
}