package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import cc.unitmesh.idea.service.JavaRelatedContext.cleanUp
import cc.unitmesh.idea.service.JavaTypeUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner

class JavaMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean,
    ): MethodContext? {
        if (psiElement !is PsiMethod) {
            return null
        }

        val parameterList = runReadAction { psiElement.parameters.mapNotNull { it.name } }
        val variableContextList = parameterList.map { it }

        val usagesList = if (gatherUsages) {
            ClassContextBuilder.findUsages(psiElement as PsiNameIdentifierOwner)
        } else {
            emptyList()
        }

        val ios: List<PsiElement> = try {
            JavaTypeUtil.resolveByMethod(psiElement).values.mapNotNull { it }
        } catch (e: Exception) {
            emptyList()
        }

        return runReadAction {
            MethodContext(
                psiElement,
                text = psiElement.text,
                name = psiElement.name,
                signature = getSignatureString(psiElement),
                enclosingClass = psiElement.containingClass,
                language = psiElement.language.displayName,
                returnType = processReturnTypeText(psiElement.returnType?.presentableText),
                variableContextList,
                includeClassContext,
                usagesList,
                ios
            )
        }
    }

    private fun processReturnTypeText(returnType: String?): String? {
        return if (returnType == "void") null else returnType
    }

    private fun getSignatureString(method: PsiMethod): String {
        val text = runReadAction {
            cleanUp(method).text
        }
        val trimmed = text.replace('\n', ' ').trim()
        return trimmed
    }
}
