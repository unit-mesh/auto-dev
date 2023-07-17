package cc.unitmesh.devti.context.variablebuilder

import cc.unitmesh.devti.context.VariableContext
import com.intellij.psi.PsiElement

interface VariableContextBuilder {
    fun getVariableContext(
        psiElement: PsiElement,
        includeMethodContext: Boolean,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): VariableContext?
}
