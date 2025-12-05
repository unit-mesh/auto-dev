package cc.unitmesh.ide.javascript.util

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecma6.TypeScriptInterface
import com.intellij.lang.javascript.psi.ecma6.TypeScriptSingleType
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.util.JSStubBasedPsiTreeUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement

object JSTypeResolver {
    fun resolveByElement(element: PsiElement): List<JSClass> =
        ReadAction.compute<List<JSClass>, Throwable> {
            val elements = mutableListOf<JSClass>()
            when (element) {
                is JSClass -> {
                    element.functions.map {
                        elements += resolveByFunction(it).values
                    }
                }

                is JSFunction -> {
                    elements += resolveByFunction(element).values
                }

                else -> {}
            }

            return@compute elements
        }

    private fun resolveByFunction(jsFunction: JSFunction): Map<String, JSClass> {
        val result = mutableMapOf<String, JSClass>()
        jsFunction.parameterList?.parameters?.map {
            it.typeElement?.let { typeElement ->
                result += resolveByType(typeElement, it.typeElement!!.text)
            }
        }

        result += jsFunction.returnTypeElement?.let {
            resolveByType(it, jsFunction.returnType!!.resolvedTypeText)
        } ?: emptyMap()

        return result
    }

    private fun resolveByType(
        returnType: PsiElement?,
        typeName: String,
    ): MutableMap<String, JSClass> {
        val result = mutableMapOf<String, JSClass>()
        when (returnType) {
            is TypeScriptSingleType -> {
                when (val referenceLocally = JSStubBasedPsiTreeUtil.resolveLocally(typeName, returnType)) {
                    is TypeScriptInterface -> {
                        result += mapOf(typeName to referenceLocally)
                    }
                }
            }
        }

        return result
    }
}