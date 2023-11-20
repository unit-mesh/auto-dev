package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TestDataBuilder
import cc.unitmesh.idea.context.JavaContextCollection
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class JavaTestDataBuilder : TestDataBuilder {
    override fun inBoundData(element: PsiElement): Map<String, String> {
        if (element !is PsiMethod) return emptyMap()

        val result = mutableMapOf<String, String>()
        val parameters = element.parameterList.parameters
        for (parameter in parameters) {
            val type = parameter.type
            if (type is PsiClassType) {
                type.resolve()?.let {
                    val qualifiedName = it.qualifiedName!!
                    val simpleClassStructure = JavaContextCollection.dataStructure(it)
                    result[qualifiedName] = simpleClassStructure.toString()
                }
            }
        }

        return result
    }

    override fun outBoundData(element: PsiElement): Map<String, String> {
        if (element !is PsiMethod) return emptyMap()

        val returnType = element.returnType ?: return emptyMap()
        when {
            returnType is PsiClassType -> {
                returnType.resolve()?.let {
                    val qualifiedName = it.qualifiedName!!
                    val simpleClassStructure = JavaContextCollection.dataStructure(it)
                    return mapOf(qualifiedName to simpleClassStructure.toString())
                }
            }
        }

        return emptyMap()
    }
}