package cc.unitmesh.scala.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass

class ScalaClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is ScClass) return null

        val name = psiElement.name ?: return null

        val functions = psiElement.methods.toList()
        val allFields = psiElement.fields.toList()
        val superClass = psiElement.supers.mapNotNull { it.qualifiedName }.toList()
        val usages = findUsages(psiElement as PsiNameIdentifierOwner)

        val annotations = psiElement.annotations.mapNotNull {
            it.text
        }

        val displayName = psiElement.qualifiedName ?: psiElement.name ?: ""
        return ClassContext(
            psiElement,
            psiElement.text,
            name,
            functions,
            allFields,
            superClass,
            usages,
            displayName = displayName,
            annotations
        )
    }
}

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
