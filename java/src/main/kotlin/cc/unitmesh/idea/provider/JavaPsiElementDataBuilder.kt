package cc.unitmesh.idea.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.PsiElementDataBuilder
import cc.unitmesh.idea.context.JavaClassContextBuilder
import cc.unitmesh.idea.context.JavaContextCollection
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope

open class JavaPsiElementDataBuilder : PsiElementDataBuilder {
    /**
     * Returns the base route of a given Kotlin language method.
     *
     * This method takes a PsiElement as input and checks if it is an instance of PsiMethod. If it is not, an empty string is returned.
     * If the input element is a PsiMethod, the method checks if its containing class has the annotation "@RequestMapping" from the Spring Framework.
     * If the annotation is found, the method retrieves the value attribute of the annotation and returns it as a string.
     * If the value attribute is not a PsiLiteralExpression, an empty string is returned.
     *
     * @param element the PsiElement representing the Kotlin language method
     * @return the base route of the method as a string, or an empty string if the method does not have a base route or if the input element is not a PsiMethod
     */
    override fun baseRoute(element: PsiElement): String {
        if (element !is PsiMethod) return ""

        val containingClass = element.containingClass ?: return ""
        containingClass.annotations.forEach {
            if (it.qualifiedName?.endsWith("RequestMapping") == true) {
                val value = it.findAttributeValue("value") ?: return ""
                if (value is PsiLiteralExpression) {
                    return value.value as String
                }
            }
        }

        return ""
    }

    override fun inboundData(element: PsiElement): Map<String, String> {
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
            is PsiClassType -> return processingClassType(type)
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
            JavaContextCollection.dataStructure(it)?.let { simpleClassStructure ->
                result += mapOf(qualifiedName to simpleClassStructure.toString())
            }
        }

        return result
    }

    override fun outboundData(element: PsiElement): Map<String, String> {
        if (element !is PsiMethod) return emptyMap()

        val result = mutableMapOf<String, String>()
        val returnType = element.returnType ?: return emptyMap()

        result += processing(returnType)

        return result
    }

    override fun lookupElement(project: Project, canonicalName: String): ClassContext? {
        val psiClass: PsiClass = JavaPsiFacade.getInstance(project).findClass(canonicalName, GlobalSearchScope.projectScope(project)) ?: return null
        return JavaClassContextBuilder().getClassContext(psiClass, false)
    }
}