package cc.unitmesh.python.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.types.TypeEvalContext

class PythonClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is PyClass) {
            return null
        }

        val text = psiElement.text
        val name = psiElement.name
        val methods = psiElement.methods
        val methodList: List<PyFunction> = methods.toList()
        val classAttributes = psiElement.classAttributes
        val ancestorClasses =
            psiElement.getAncestorClasses(TypeEvalContext.codeInsightFallback(psiElement.project)).toList()
        val qualifiedNames = ancestorClasses.mapNotNull { it.qualifiedName }
        val usages =
            if (gatherUsages) PythonVariableContextBuilder.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return ClassContext(psiElement, text, name, methodList, classAttributes, qualifiedNames, usages)
    }

}
