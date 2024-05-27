package cc.unitmesh.idea.service

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiUtil

object JavaTypeUtil {
    private fun resolveByType(outputType: PsiType?): Map<String, PsiClass> {
        val resolvedClasses = mutableMapOf<String, PsiClass>()
        if (outputType is PsiClassReferenceType) {
            val resolveClz = outputType.resolve()
            outputType.parameters.filterIsInstance<PsiClassReferenceType>().forEach {
                if (resolveClz != null) {
                    resolvedClasses[it.canonicalText] = resolveClz
                }
            }

            val canonicalText = outputType.canonicalText
            if (resolveClz != null) {
                resolvedClasses[canonicalText] = resolveClz
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
        if (element !is PsiMethod) {
            return emptyMap()
        }

        runReadAction {
            element.parameterList.parameters.filter {
                it.type is PsiClassReferenceType
            }.map { parameter ->
                val type = parameter.type as PsiClassReferenceType
                val resolve: PsiClass = type.resolve() ?: return@map null
                val typeParametersTypeList: List<PsiType> = getTypeParametersType(type)

                val relatedClass = mutableListOf(parameter.type)
                relatedClass.addAll(typeParametersTypeList)

                relatedClass
                    .filter { isProjectContent((it as PsiClassReferenceType).resolve() ?: return@filter false) }
                    .forEach { resolvedClasses.putAll(resolveByType(it)) }

                resolvedClasses[parameter.name] = resolve
            }

            val outputType = element.returnTypeElement?.type
            resolvedClasses.putAll(resolveByType(outputType))
        }

        return runReadAction {resolvedClasses.filter { isProjectContent(it.value) }.toMap()}
    }

    private fun getTypeParametersType(
        psiType: PsiClassReferenceType
    ): List<PsiType> {
        val result = psiType.resolveGenerics()
        val psiClass = result.element;
        if (psiClass != null) {
            val substitutor = runReadAction { result.substitutor }
            return runReadAction {
                psiClass.typeParameters.mapNotNull {
                    substitutor.substitute(it)
                }
            }
        }
        return emptyList()
    }
}

fun isProjectContent(element: PsiElement): Boolean {
    val virtualFile = PsiUtil.getVirtualFile(element)
    val project = runReadAction { element.project }
    return virtualFile == null || ProjectFileIndex.getInstance(project).isInContent(virtualFile)
}
