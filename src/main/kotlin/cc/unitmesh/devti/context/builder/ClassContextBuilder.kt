package cc.unitmesh.devti.context.builder

import cc.unitmesh.devti.context.ClassContext
import com.intellij.psi.PsiElement

/**
 * The ClassContextBuilder interface provides a method to retrieve the class context for a given PsiElement.
 * The class context represents the surrounding context of a class, including its imports, package declaration,
 * and any other relevant information.
 */
interface ClassContextBuilder {
    /**
     * Retrieves the class context for the given [psiElement].
     *
     * @param psiElement the PSI element for which to retrieve the class context
     * @param gatherUsages specifies whether to gather usages of the class
     * @return the class context for the given [psiElement], or null if the class context cannot be determined
     */
    fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext?
}
