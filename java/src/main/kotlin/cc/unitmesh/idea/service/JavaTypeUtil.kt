package cc.unitmesh.idea.service

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiUtil

object JavaTypeUtil {
    private fun resolveByType(outputType: PsiType?): Map<String, PsiClass> {
        val resolvedClasses = mutableMapOf<String, PsiClass>()
        if (outputType is PsiClassReferenceType) {
            if (outputType.parameters.isNotEmpty()) {
                outputType.parameters.forEach {
                    if (it is PsiClassReferenceType && outputType.resolve() != null) {
                        resolvedClasses[it.canonicalText] = outputType.resolve()!!
                    }
                }
            }

            val canonicalText = outputType.canonicalText
            if (outputType.resolve() != null) {
                resolvedClasses[canonicalText] = outputType.resolve()!!
            }
        }

        return resolvedClasses.filter { isProjectContent(it.value) }.toMap()
    }

    fun resolveByField(element: PsiElement): Map<String, PsiClass> {
        val psiFile = element.containingFile as PsiJavaFile

        val resolvedClasses = mutableMapOf<String, PsiClass>()
        psiFile.classes.forEach { psiClass ->
            psiClass.fields.forEach { field ->
                resolvedClasses.putAll(resolveByType(field.type))
            }
        }

        return resolvedClasses.filter { isProjectContent(it.value) }.toMap()
    }

    /**
     * The resolved classes include all the classes in the method signature. For example, if the method signature is
     * Int, will return Int, but if the method signature is List<Int>, will return List and Int.
     * So, remember to filter out the classes that are not needed.
     */
    fun resolveByMethod(element: PsiElement): Map<String, PsiClass> {
        val resolvedClasses = mutableMapOf<String, PsiClass>()
        if (element is PsiMethod) {
            element.parameterList.parameters.filter {
                it.type is PsiClassReferenceType
            }.map {
                val resolve = (it.type as PsiClassReferenceType).resolve() ?: return@map null
                if (!isProjectContent(resolve)) {
                    return@map null
                }

                resolvedClasses[it.name] = resolve
            }

            val outputType = element.returnTypeElement?.type
            resolvedClasses.putAll(resolveByType(outputType))
        }

        return resolvedClasses.filter { isProjectContent(it.value) }.toMap()
    }
}

fun isProjectContent(element: PsiElement): Boolean {
    val virtualFile = PsiUtil.getVirtualFile(element)
    return virtualFile == null || ProjectFileIndex.getInstance(element.project).isInContent(virtualFile)
}
