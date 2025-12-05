package cc.unitmesh.ide.javascript.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch

class JavaScriptClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        when (psiElement) {
            is JSClass -> {
                val methods: List<PsiElement> = psiElement.functions.toList()
                val fields: List<PsiElement> = psiElement.fields.toList()

                val references: List<PsiReference> = if (gatherUsages) {
                    findUsages(psiElement)
                } else {
                    emptyList()
                }

                val supers = psiElement.supers
                val superClasses = supers.filterIsInstance<JSClass>().mapNotNull { it.name }

                return ClassContext(psiElement, psiElement.text, psiElement.name, methods, fields, superClasses, references)
            }
            else -> return null
        }
    }
}

fun findUsages(psiElement: PsiElement): List<PsiReference> {
    val globalSearchScope = GlobalSearchScope.allScope(psiElement.project)

    return ReferencesSearch.search(psiElement, globalSearchScope, true)
        .findAll()
        .toList()
}