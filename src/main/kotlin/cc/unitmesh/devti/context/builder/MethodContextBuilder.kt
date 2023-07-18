package cc.unitmesh.devti.context.builder

import cc.unitmesh.devti.context.MethodContext
import com.intellij.psi.PsiElement

interface MethodContextBuilder {
    fun getMethodContext(psiElement: PsiElement, includeClassContext: Boolean, gatherUsages: Boolean): MethodContext?
}
