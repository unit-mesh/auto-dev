package cc.unitmesh.go.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.goide.psi.GoFile
import com.intellij.psi.PsiElement

class GoClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is GoFile) return null

        val className = psiElement.name
        val classText = psiElement.text

        return ClassContext(psiElement, classText, className, emptyList(), emptyList(), emptyList(), emptyList())
    }
}