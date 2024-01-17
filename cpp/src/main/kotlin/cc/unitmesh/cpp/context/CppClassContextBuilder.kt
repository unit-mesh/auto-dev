package cc.unitmesh.cpp.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.execution.debugger.evaluation.renderers.CxxNameParser

class CppClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is CxxNameParser) {
            return null
        }

        return null
    }
}
