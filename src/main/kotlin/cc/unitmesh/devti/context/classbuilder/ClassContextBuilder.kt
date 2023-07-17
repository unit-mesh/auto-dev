package cc.unitmesh.devti.context.classbuilder

import cc.unitmesh.devti.context.ClassContext
import com.intellij.psi.PsiElement

interface ClassContextBuilder {
    fun getClassContext(paramPsiElement: PsiElement, paramBoolean: Boolean): ClassContext?
}
