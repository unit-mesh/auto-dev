package cc.unitmesh.kotlin.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import cc.unitmesh.idea.context.JavaContextCollectionUtilsKt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KotlinMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {
        if (psiElement !is KtNamedFunction) return null

        val returnType = psiElement.getReturnTypeReference()?.text
        val containingClass = psiElement.containingClass()
        val signatureString = Util.getSignatureString(psiElement)
        val displayName = psiElement.language.displayName
        val valueParameters = psiElement.valueParameters.mapNotNull { it.name }
        val usages =
            if (gatherUsages) JavaContextCollectionUtilsKt.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return MethodContext(
            psiElement,
            psiElement.text,
            psiElement.name,
            signatureString,
            containingClass,
            displayName,
            returnType,
            valueParameters,
            includeClassContext,
            usages
        )
    }

    object Util {
        fun getSignatureString(signatureString: KtNamedFunction): String {
            val bodyBlockExpression = signatureString.bodyBlockExpression
            val startOffsetInParent = if (bodyBlockExpression != null) {
                bodyBlockExpression.startOffsetInParent
            } else {
                val bodyExpression = signatureString.bodyExpression
                bodyExpression?.startOffsetInParent ?: signatureString.textLength
            }
            val text = signatureString.text
            val substring = text.substring(0, startOffsetInParent)
            return substring.replace('\n', ' ').trim()
        }
    }

}

