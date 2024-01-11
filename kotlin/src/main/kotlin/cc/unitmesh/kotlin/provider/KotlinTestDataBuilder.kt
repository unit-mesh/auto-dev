package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.provider.TestDataBuilder
import cc.unitmesh.kotlin.context.KotlinClassContextBuilder
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getContentRange

class KotlinTestDataBuilder : TestDataBuilder {
    override fun baseRoute(element: PsiElement): String {
        if (element !is KtNamedFunction) return ""

        val clazz = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        if (clazz !is KtClass) return ""

        clazz.annotationEntries.forEach {
            if (it.shortName?.asString() == "RequestMapping") {
                return when (val value = it.valueArguments.firstOrNull()?.getArgumentExpression()) {
                    is KtStringTemplateExpression -> {
                        value.literalContents() ?: value.text
                    }

                    is KtSimpleNameExpression -> {
                        value.getReferencedName()
                    }

                    else -> {
                        "null"
                    }
                }
            }
        }

        return ""
    }

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
        val fqn = type.fqName?.asString() ?: return result

        KotlinClassContextBuilder().getClassContext(type, false)?.format()?.let {
            result += mapOf(fqn to it)
        }

        return result
    }

    override fun outboundData(element: PsiElement): Map<String, String> {
        if (element is KtClass) {
            return processingMethodsOutbound(element)
        }
        if (element !is KtNamedFunction) return emptyMap()

        val returnType = element.getReturnTypeReference() ?: return emptyMap()

        val result = mutableMapOf<String, String>()

        result += processing(returnType)

        return result
    }

    /**
     * Processes the outbound methods of a Kotlin class and returns a map of method names and their return types.
     *
     * @param element The Kotlin class element to process.
     * @return A map of method names and their return types.
     */
    private fun processingMethodsOutbound(element: KtClass): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val methods = element.declarations.filterIsInstance<KtNamedFunction>()
        for (method in methods) {
            val returnType = method.getReturnTypeReference() ?: continue
            result += processing(returnType)
        }

        return result
    }

    private fun processing(returnType: KtTypeReference): Map<String, String> {
        val result = mutableMapOf<String, String>()
        when (val typeElement = returnType.typeElement) {
            is KtUserType -> {
                val referenceExpression = typeElement.referenceExpression?.resolveMainReference()
                if (referenceExpression is KtClass) {
                    result += processingClassType(referenceExpression)
                }
            }
        }

        return result
    }
}

internal fun KtStringTemplateExpression.literalContents(): String? {
    val escaper = createLiteralTextEscaper()
    val ssb = StringBuilder()
    return when(escaper.decode(getContentRange(), ssb)) {
        true -> ssb.toString()
        false -> null
    }
}

fun KtReferenceExpression.resolveMainReference(): PsiElement? =
    try {
        mainReference.resolve()
    } catch (e: Exception) {
        null
    }
