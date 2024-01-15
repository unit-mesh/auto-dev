package cc.unitmesh.ide.webstorm.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.intellij.lang.javascript.presentable.JSFormatUtil
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.util.JSUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

class JavaScriptMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {
        if (psiElement !is JSFunction) return null

        val functionSignature = JSFormatUtil.buildFunctionSignaturePresentation(psiElement)
        val containingClass: PsiElement? = JSUtils.getMemberContainingClass(psiElement)
        val languageDisplayName = psiElement.language.displayName
        val returnType = psiElement.returnType
        val returnTypeText = returnType?.substitute()?.getTypeText(JSType.TypeTextFormat.CODE)

        val parameterNames = psiElement.parameters.mapNotNull { it.name }

        val usages =
            if (gatherUsages) JavaScriptClassContextBuilder.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return MethodContext(
            psiElement, psiElement.text, psiElement.name!!, functionSignature, containingClass, languageDisplayName,
            returnTypeText, parameterNames, includeClassContext, usages
        )
    }
}