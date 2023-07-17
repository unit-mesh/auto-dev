package cc.unitmesh.devti.context.builder

import com.intellij.ml.llm.context.MethodContext
import com.intellij.psi.PsiElement

interface MethodContextBuilder {
    fun getMethodContext(psiElement: PsiElement, includeClassContext: Boolean, gatherUsages: Boolean): MethodContext?
}
