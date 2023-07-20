package cc.unitmesh.idea.kotlin

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.intellij.psi.PsiElement

class KotlinMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {
        return null
    }
}
