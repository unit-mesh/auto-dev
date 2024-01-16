package cc.unitmesh.go.context

import cc.unitmesh.devti.context.VariableContext
import cc.unitmesh.devti.context.builder.VariableContextBuilder
import com.goide.psi.GoVarOrConstDefinition
import com.goide.psi.GoVarOrConstSpec
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

class GoVariableContextBuilder : VariableContextBuilder {
    override fun getVariableContext(
        psiElement: PsiElement,
        includeMethodContext: Boolean,
        includeClassContext: Boolean,
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
