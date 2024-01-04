package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TestDataBuilder
import cc.unitmesh.idea.context.JavaContextCollection
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

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

    private fun processingClassType(type: PsiClassType): Map<@NlsSafe String, String> {
        type.resolve()?.let {
            val qualifiedName = it.qualifiedName!!
            val simpleClassStructure = JavaContextCollection.dataStructure(it)
            return mapOf(qualifiedName to simpleClassStructure.toString())
        }

        return emptyMap()
    }

    override fun outBoundData(element: PsiElement): Map<String, String> {
        if (element !is PsiMethod) return emptyMap()

        val result = mutableMapOf<String, String>()
        val returnType = element.returnType ?: return emptyMap()

//        val typeParameters = (returnType as PsiClassReferenceType).reference.typeParameters
//        typeParameters.forEach {
//            val qualifiedName = it.canonicalText
//            val simpleClassStructure = JavaContextCollection.dataStructure(it)
//            result[qualifiedName] = simpleClassStructure.toString()
//        }

        when {
            returnType is PsiClassType -> {
                result += processingClassType(returnType)
            }
        }

        return result
    }
}