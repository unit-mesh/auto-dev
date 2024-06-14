package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.VariableContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import cc.unitmesh.devti.context.builder.VariableContextBuilder
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.*

class JavaVariableContextBuilder : VariableContextBuilder {
    override fun getVariableContext(
        psiElement: PsiElement,
        withMethodContext: Boolean,
        withClassContext: Boolean,
        gatherUsages: Boolean
    ): VariableContext? {
        if (psiElement !is PsiVariable) return null

        val containingMethod = runReadAction {psiElement.getContainingMethod()  }
        val containingClass = runReadAction {  psiElement.getContainingClass()}

        val references =
            if (gatherUsages) ClassContextBuilder.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return runReadAction {  VariableContext(
            psiElement,
            psiElement.text ?: "",
            psiElement.name,
            containingMethod,
            containingClass,
            references,
            withMethodContext,
            withClassContext
        )}
    }

    private fun PsiElement.getContainingMethod(): PsiMethod? {
        var context: PsiElement? = this.context
        while (context != null) {
            if (context is PsiMethod) return context

            context = context.context
        }

        return null
    }

    private fun PsiElement.getContainingClass(): PsiClass? {
        var context: PsiElement? = this.context
        while (context != null) {
            if (context is PsiClass) return context
            if (context is PsiMember) return context.containingClass

            context = context.context
        }

        return null
    }
}
