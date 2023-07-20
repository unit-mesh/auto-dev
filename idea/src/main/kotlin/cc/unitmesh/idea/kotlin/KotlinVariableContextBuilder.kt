package cc.unitmesh.idea.kotlin

import cc.unitmesh.devti.context.VariableContext
import cc.unitmesh.devti.context.builder.VariableContextBuilder
import com.intellij.psi.PsiElement

class KotlinVariableContextBuilder : VariableContextBuilder {
    override fun getVariableContext(
        psiElement: PsiElement,
        includeMethodContext: Boolean,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): VariableContext? {
        return null
    }
}
