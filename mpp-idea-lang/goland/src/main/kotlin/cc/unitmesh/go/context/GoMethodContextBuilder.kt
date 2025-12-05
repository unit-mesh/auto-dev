package cc.unitmesh.go.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoMethodDeclaration
import com.intellij.psi.PsiElement

class GoMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {

        if (psiElement !is GoFunctionOrMethodDeclaration) {
            return null
        }


        val funcName = psiElement.name ?: ""

        val functionSignature: String = when (psiElement) {
            is GoMethodDeclaration -> {
                psiElement.signature?.text ?: ""
            }

            is GoFunctionDeclaration -> {
                psiElement.signature?.text ?: ""
            }

            else -> ""
        }
        val returnType = psiElement.signature?.resultType?.text
        val languages = psiElement.language.displayName
        val returnTypeText = returnType
        val parameterNames = psiElement.signature?.parameters?.parameterDeclarationList?.mapNotNull {
            it.paramDefinitionList.firstOrNull()?.text
        }.orEmpty()

        return MethodContext(
            psiElement, psiElement.text, funcName, functionSignature, null, languages,
            returnTypeText, parameterNames, includeClassContext, emptyList()
        )

    }
}
