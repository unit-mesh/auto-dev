package cc.unitmesh.devti.context.builder

import cc.unitmesh.devti.context.ClassContext
import com.intellij.psi.PsiElement

interface ClassContextBuilder {
    fun getClassContext(paramPsiElement: PsiElement, paramBoolean: Boolean): ClassContext?
}
