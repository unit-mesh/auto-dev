package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.provider.PsiElementDataBuilder
import cc.unitmesh.idea.service.isProjectContent
import cc.unitmesh.kotlin.context.KotlinClassContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getContentRange

class KotlinPsiElementDataBuilder : PsiElementDataBuilder {
    override fun baseRoute(element: PsiElement): String {
        if (element !is KtNamedFunction) return ""

        val clazz = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        if (clazz !is KtClass) return ""

        clazz.annotationEntries.forEach {
            when {
                it.shortName?.asString() == "RequestMapping" -> {
                    return when (val value = it.valueArguments.firstOrNull()?.getArgumentExpression()) {
                        is KtStringTemplateExpression -> value.literalContents() ?: value.text
                        is KtSimpleNameExpression -> value.getReferencedName()
                        else -> ""
                    }
                }
            }
        }

        return ""
    }

    override fun inboundData(element: PsiElement): Map<String, String> {
        if (element !is KtNamedFunction) return emptyMap()

        return handleParameters(element.valueParameters)
    }

    private fun handleParameters(ktParameters: MutableList<KtParameter>): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        ktParameters.map { parameter ->
            result += handleFromType(parameter)
        }

        return result
    }

    private fun handleFromType(parameter: KtParameter): Map<String, String> {
        val map = when (val type = parameter.typeReference?.typeElement) {
            is KtClass -> processingClassType(type)
            else -> emptyMap()
        }

        return map
    }


    private fun processingClassType(type: KtClass): Map<String, String> {
        if (!isProjectContent(type)) return emptyMap()

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

        return processing(returnType, element)
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
            val parameters = handleParameters(method.valueParameters)
            result += parameters

            val returnType = method.getReturnTypeReference() ?: continue
            result += processing(returnType, element)
        }

        return result
    }

    private fun processing(returnType: KtTypeReference, element: PsiElement): Map<String, String> {
        val result = mutableMapOf<String, String>()
        when (val typeElement = returnType.typeElement) {
            is KtUserType -> {
                val referenceExpression = typeElement.referenceExpression?.resolveMainReference()
                if (referenceExpression is KtClass) {
                    result += processingClassType(referenceExpression)
                }

                typeElement.typeArgumentsAsTypes.forEach {
                    result += processing(it, element)
                }
            }
        }

        return result
    }
}

internal fun KtStringTemplateExpression.literalContents(): String? {
    val escaper = createLiteralTextEscaper()
    val ssb = StringBuilder()
    return when (escaper.decode(getContentRange(), ssb)) {
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
