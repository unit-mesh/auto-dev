package cc.unitmesh.kotlin.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters

object KotlinTypeResolver {
    fun resolveByElement(element: PsiElement): MutableMap<String, KtClass?> {
        if (element !is KtNamedDeclaration) return mutableMapOf()

        val resolvedClasses: MutableMap<String, KtClass?> = resolveByMethod(element)
        when (element) {
            is KtClassOrObject -> {
                KotlinPsiUtil.getFunctions(element).forEach {
                    resolvedClasses.putAll(resolveByMethod(it))
                }

                resolvedClasses.putAll(resolveByFields(element))
            }

            is KtFile -> {
                KotlinPsiUtil.getClasses(element).forEach {
                    resolvedClasses.putAll(resolveByFields(it))
                }
            }
        }
        return resolvedClasses
    }


    fun resolveByFields(element: KtClassOrObject): Map<out String, KtClass?> {
        val resolvedClasses = mutableMapOf<String, KtClass?>()
        element.primaryConstructorParameters.forEach {
            val typeReference = it.typeReference
            val elements = resolveType(typeReference)
            elements.forEach { element ->
                if (element is KtClass) {
                    resolvedClasses[element.name!!] = element
                }
            }
        }

        return resolvedClasses
    }

    fun resolveByMethod(element: PsiElement): MutableMap<String, KtClass?> {
        val resolvedClasses = mutableMapOf<String, KtClass?>()
        when (element) {
            is KtNamedDeclaration -> {
                element.getValueParameters().map {
                    val typeReference = it.typeReference
                    resolveType(typeReference)
                }.forEach {
                    if (it is KtClass) {
                        resolvedClasses[it.name!!] = it
                    }
                }

                // with Generic returnType, like: ResponseEntity<List<Item>>
                element.getReturnTypeReferences().forEach { returnType ->
                    resolveType(returnType).filterIsInstance<KtClass>().forEach {
                        resolvedClasses[it.name!!] = it
                    }
                }
            }
        }

        return resolvedClasses
    }

    private fun resolveType(typeReference: KtTypeReference?): List<PsiElement> {
        if (typeReference == null) return emptyList()
        val result = mutableListOf<PsiElement>()
        when (val ktTypeElement = typeReference.typeElement) {
            is KtUserType -> {
                ktTypeElement.typeArguments.forEach {
                    result += resolveType(it.typeReference)
                }

                val typeElementReference = ktTypeElement.referenceExpression?.mainReference?.resolve()
                if (typeElementReference is KtClass) {
                    result += typeElementReference
                }
            }
        }

        return result
    }
}