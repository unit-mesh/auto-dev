package cc.unitmesh.idea.kotlin

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement

class KotlinClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        TODO("Not yet implemented")
    }
}
