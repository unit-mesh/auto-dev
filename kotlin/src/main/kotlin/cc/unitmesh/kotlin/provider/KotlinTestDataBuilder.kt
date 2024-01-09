package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.provider.TestDataBuilder
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

class KotlinTestDataBuilder : TestDataBuilder {
    override fun inboundData(element: PsiElement): Map<String, String> {
        if (element !is KtNamedFunction) return emptyMap()

        val result = mutableMapOf<String, String>()
        val parameters = element.valueParameters
        for (parameter in parameters) {
            result += handleFromType(parameter)
        }
        return result
    }

    private fun handleFromType(parameter: KtParameter): Map<@NlsSafe String, String> {
        when (val type = parameter.typeReference?.typeElement) {
            is KtClass -> processingClassType(type)
        }

        return emptyMap()
    }


    private fun processingClassType(type: KtClass): Map<@NlsSafe String, String> {
        val result = mutableMapOf<String, String>()
        return result
    }
}
