package cc.unitmesh.ide.idea.context

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch

object JavaContextCollectionUtilsKt {
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
