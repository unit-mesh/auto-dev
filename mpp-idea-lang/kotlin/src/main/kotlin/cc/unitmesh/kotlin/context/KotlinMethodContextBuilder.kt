package cc.unitmesh.kotlin.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import cc.unitmesh.kotlin.util.KotlinPsiUtil
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
        val signatureString = KotlinPsiUtil.signatureString(psiElement)
        val displayName = psiElement.language.displayName
        val valueParameters = psiElement.valueParameters.mapNotNull { it.name }
        val usages =
            if (gatherUsages) ClassContextBuilder.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

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
}

