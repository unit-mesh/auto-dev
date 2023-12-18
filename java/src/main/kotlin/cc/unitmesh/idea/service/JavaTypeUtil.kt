package cc.unitmesh.idea.service

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType

object JavaTypeUtil {
    private fun resolveByType(outputType: PsiType?): MutableMap<String, PsiClass?> {
        val resolvedClasses = mutableMapOf<String, PsiClass?>()
        if (outputType is PsiClassReferenceType) {
            if (outputType.parameters.isNotEmpty()) {
                outputType.parameters.forEach {
                    if (it is PsiClassReferenceType) {
                        resolvedClasses[it.canonicalText] = outputType.resolve()
                    }
                }
            }

            val canonicalText = outputType.canonicalText
            resolvedClasses[canonicalText] = outputType.resolve()
        }

        return resolvedClasses
    }

    fun resolveByField(element: PsiElement): Map<out String, PsiClass?> {
        val psiFile = element.containingFile as PsiJavaFile

        val resolvedClasses = mutableMapOf<String, PsiClass?>()
        psiFile.classes.forEach { psiClass ->
            psiClass.fields.forEach { field ->
                resolvedClasses.putAll(resolveByType(field.type))
            }
        }

        return resolvedClasses
    }

    /**
     * The resolved classes include all the classes in the method signature. For example, if the method signature is
     * Int, will return Int, but if the method signature is List<Int>, will return List and Int.
     * So, remember to filter out the classes that are not needed.
     */
    fun resolveByMethod(element: PsiElement): MutableMap<String, PsiClass?> {
        val resolvedClasses = mutableMapOf<String, PsiClass?>()
        if (element is PsiMethod) {
            element.parameterList.parameters.filter {
                it.type is PsiClassReferenceType
            }.map {
                val resolve = (it.type as PsiClassReferenceType).resolve() ?: return@map null
                if (!JavaRelatedContext.isProjectContent(resolve)) {
                    return@map null
                }

                resolvedClasses[it.name] = JavaRelatedContext.cleanUp(resolve)
            }

            val outputType = element.returnTypeElement?.type
            resolvedClasses.putAll(resolveByType(outputType))
        }

        return resolvedClasses
    }
}