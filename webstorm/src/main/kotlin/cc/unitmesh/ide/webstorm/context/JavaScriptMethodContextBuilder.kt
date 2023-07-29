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
        if (psiElement !is JSFunction) {
            return null
        }
        val jsFunction = psiElement

        val functionSignature = JSFormatUtil.buildFunctionSignaturePresentation(jsFunction)
        val containingClass: PsiElement = JSUtils.getMemberContainingClass(jsFunction)
        val languageDisplayName = jsFunction.language.displayName
        val returnType = jsFunction.returnType
        val returnTypeText = returnType?.substitute()?.getTypeText(JSType.TypeTextFormat.CODE)

        val parameterNames = jsFunction.parameters.mapNotNull { it.name }

        val usages =
            if (gatherUsages) JavaScriptClassContextBuilder.findUsages(jsFunction as PsiNameIdentifierOwner) else emptyList()

        return MethodContext(
            jsFunction, jsFunction.text, jsFunction.name!!, functionSignature, containingClass, languageDisplayName,
            returnTypeText, parameterNames, includeClassContext, usages
        )
    }
}