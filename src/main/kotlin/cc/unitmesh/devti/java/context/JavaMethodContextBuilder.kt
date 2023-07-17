package cc.unitmesh.devti.java.context

import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.intellij.ml.llm.context.MethodContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner

class JavaMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {
        if (psiElement is PsiMethod) {
            val returnType = processReturnTypeText(psiElement.returnType?.presentableText)
            val language = psiElement.language.displayName
            val containingClass = psiElement.containingClass
            val signatureString = getSignatureString(psiElement)
            val methodName = psiElement.name
            val methodText = psiElement.text
            val parameters = psiElement.parameters

            val parameterList = parameters.mapNotNull {
                it.name
            }

            val variableContextList = parameterList.map { it }

            val usagesList = if (gatherUsages) {
                JavaContextCollectionUtilsKt.findUsages(psiElement as PsiNameIdentifierOwner)
            } else {
                emptyList()
            }

            return MethodContext(
                psiElement,
                methodText,
                methodName,
                signatureString,
                containingClass,
                language,
                returnType,
                variableContextList,
                includeClassContext,
                usagesList
            )
        }
        return null
    }

    private fun processReturnTypeText(returnType: String?): String? {
        return if (returnType == "void") null else returnType
    }
}

fun getSignatureString(method: PsiMethod): String {
    val bodyStart = method.body?.startOffsetInParent ?: method.textLength
    val text = method.text
    val substring = text.substring(0, bodyStart)
    val trimmed = substring.replace('\n', ' ').trim()
    return trimmed
}