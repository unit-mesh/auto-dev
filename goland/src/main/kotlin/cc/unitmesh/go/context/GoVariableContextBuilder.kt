package cc.unitmesh.go.context

import cc.unitmesh.devti.context.VariableContext
import cc.unitmesh.devti.context.builder.VariableContextBuilder
import com.goide.psi.GoVarOrConstDefinition
import com.intellij.psi.PsiElement

class GoVariableContextBuilder : VariableContextBuilder {
    override fun getVariableContext(
        psiElement: PsiElement,
        withMethodContext: Boolean,
        withClassContext: Boolean,
        gatherUsages: Boolean
    ): VariableContext? {
        if (psiElement !is GoVarOrConstDefinition) {
            return null
        }

        val name = psiElement.name

        return VariableContext(
            psiElement, psiElement.text, name, null, null, emptyList(), false, false
        )
    }
}
