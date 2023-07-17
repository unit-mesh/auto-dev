package cc.unitmesh.devti.java.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import kotlin.collections.ArrayList
import kotlin.collections.toList

class JavaClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is PsiClass) {
            return null
        }

        val supers = psiElement.supers as Array<PsiClass>
        val fields = psiElement.fields.toList()
        val methods = psiElement.methods.toList()
        val className = psiElement.name
        val classText = psiElement.text

        val destination = ArrayList<String>()
        for (element in supers) {
            val name = element.name
            if (name != null) {
                destination.add(name)
            }
        }

        val usages =
            if (gatherUsages) JavaContextCollectionUtilsKt.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return ClassContext(psiElement, classText, className, methods, fields, destination, usages)
    }
}
