package cc.unitmesh.go.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

class GoMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {

        if (psiElement !is GoFunctionOrMethodDeclaration) {
            return null
        }

        val functionSignature = psiElement.signature?.text
        val returnType = psiElement.signature?.resultType?.text
        val languages = psiElement.language.displayName
        val returnTypeText = returnType
        val parameterNames = psiElement.signature?.parameters?.parameterDeclarationList?.mapNotNull {
            it.paramDefinitionList.firstOrNull()?.text
        }.orEmpty()

        return MethodContext(
            psiElement, psiElement.text, psiElement.name!!, functionSignature, null, languages,
            returnTypeText, parameterNames, includeClassContext, emptyList()
        )

    }
}
