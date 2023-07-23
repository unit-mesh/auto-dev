package cc.unitmesh.idea.java.context

import cc.unitmesh.devti.context.VariableContext
import cc.unitmesh.devti.context.builder.VariableContextBuilder
import com.intellij.psi.*

class JavaVariableContextBuilder : VariableContextBuilder {
    override fun getVariableContext(
        psiElement: PsiElement,
        includeMethodContext: Boolean,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): VariableContext? {
        if (psiElement !is PsiVariable) return null

        val containingMethod = psiElement.getContainingMethod()
        val containingClass = psiElement.getContainingClass()

        val references =
            if (gatherUsages) JavaContextCollectionUtilsKt.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return VariableContext(
            psiElement,
            psiElement.text,
            psiElement.name,
            containingMethod,
            containingClass,
            references,
            includeMethodContext,
            includeClassContext
        )
    }
}

fun PsiElement.getContainingMethod(): PsiMethod? {
    var context: PsiElement? = this.context
    while (context != null) {
        if (context is PsiMethod) return context

        context = context.context
    }

    return null
}

fun PsiElement.getContainingClass(): PsiClass? {
    var context: PsiElement? = this.context
    while (context != null) {
        if (context is PsiClass) return context
        if (context is PsiMember) return context.containingClass

        context = context.context
    }

    return null
}

