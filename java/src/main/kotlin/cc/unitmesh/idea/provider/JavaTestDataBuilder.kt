package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TestDataBuilder
import cc.unitmesh.idea.context.JavaContextCollection
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType

class JavaTestDataBuilder : TestDataBuilder {
    override fun inBoundData(element: PsiElement): Map<String, String> {
        if (element !is PsiMethod) return emptyMap()

        val result = mutableMapOf<String, String>()
        val parameters = element.parameterList.parameters
        for (parameter in parameters) {
            result += handleFromType(parameter)
        }

        return result
    }

    private fun handleFromType(parameter: PsiParameter): Map<@NlsSafe String, String> {
        when (val type = parameter.type) {
            is PsiClassType -> processingClassType(type)
        }

        return emptyMap()
    }

    private fun processing(returnType: PsiType): Map<@NlsSafe String, String> {
        when {
            returnType is PsiClassType -> {
                return processingClassType(returnType)
            }
        }

        return mapOf()
    }

    private fun processingClassType(type: PsiClassType): Map<@NlsSafe String, String> {
        val result = mutableMapOf<String, String>()
        when (type) {
            is PsiClassReferenceType -> {
                type.reference.typeParameters.forEach {
                    result += processing(it)
                }
            }
        }

        type.resolve()?.let {
            val qualifiedName = it.qualifiedName!!
            val simpleClassStructure = JavaContextCollection.dataStructure(it)
            result += mapOf(qualifiedName to simpleClassStructure.toString())
        }

        return result
    }

    override fun outBoundData(element: PsiElement): Map<String, String> {
        if (element !is PsiMethod) return emptyMap()

        val result = mutableMapOf<String, String>()
        val returnType = element.returnType ?: return emptyMap()

        result += processing(returnType)

        return result
    }
}