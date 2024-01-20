package cc.unitmesh.csharp.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpDeclaration

class CSharpClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is CSharpDeclaration) return null

        return ClassContext(
            psiElement,
            psiElement.text,
            psiElement.text,
            methods = listOf(),
            fields = listOf(),
            usages = listOf(),

            )
    }

}
