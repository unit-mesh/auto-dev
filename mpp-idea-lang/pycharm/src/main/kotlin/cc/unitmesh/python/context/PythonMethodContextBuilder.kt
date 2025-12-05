package cc.unitmesh.python.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.types.TypeEvalContext

class PythonMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {
        if (psiElement !is PyFunction) {
            return null
        }

        val returnStatementType =
            psiElement.getReturnStatementType(TypeEvalContext.codeInsightFallback(psiElement.project))
        val returnType = returnStatementType?.name ?: ""
        val language = psiElement.language.displayName
        val enclosingClass = psiElement.containingClass
        val signature = psiElement.name
        val name = psiElement.name
        val text = psiElement.text
        val paramNames = psiElement.parameterList.parameters.mapNotNull { it.name }
        val usages =
            if (gatherUsages) PythonVariableContextBuilder.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return MethodContext(
            psiElement,
            text,
            name,
            signature,
            enclosingClass,
            language,
            returnType,
            paramNames,
            includeClassContext,
            usages
        )
    }
}