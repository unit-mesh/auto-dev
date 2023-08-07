package cc.unitmesh.csharp.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpBlock

class CsharpClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is CSharpBlock) return null

        return null
    }

}
