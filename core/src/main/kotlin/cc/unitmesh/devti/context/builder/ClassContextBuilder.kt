package cc.unitmesh.devti.context.builder

import cc.unitmesh.devti.context.ClassContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch

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

    companion object {
        fun findUsages(nameIdentifierOwner: PsiNameIdentifierOwner): List<PsiReference> {
            val project = nameIdentifierOwner.project
            val searchScope = GlobalSearchScope.allScope(project) as SearchScope

            return when (nameIdentifierOwner) {
                is PsiMethod -> {
                    MethodReferencesSearch.search(nameIdentifierOwner, searchScope, true)
                }

                else -> {
                    ReferencesSearch.search((nameIdentifierOwner as PsiElement), searchScope, true)
                }
            }.findAll().map { it as PsiReference }
        }
    }
}
