package cc.unitmesh.kotlin.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import cc.unitmesh.idea.context.JavaContextCollectionUtilsKt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

class KotlinClassContextBuilder : ClassContextBuilder {
    private fun getDeclaredFields(kotlinClass: KtClassOrObject): List<KtNamedFunction> {
        return kotlinClass.getDeclarations().filterIsInstance<KtNamedFunction>()
    }

    private fun getPrimaryConstructorFields(kotlinClass: KtClassOrObject): List<KtParameter> {
        return kotlinClass.getPrimaryConstructorParameters().filter { it.hasValOrVar() }
    }

    @Nullable
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is KtClassOrObject) return null

        val text = psiElement.text
        val name = psiElement.name
        val declaredFields = getDeclaredFields(psiElement)
        val primaryConstructorFields = getPrimaryConstructorFields(psiElement)
        val allFields = declaredFields + primaryConstructorFields
        val usages =
            if (gatherUsages) JavaContextCollectionUtilsKt.findUsages(psiElement as PsiNameIdentifierOwner) else emptyList()

        return ClassContext(psiElement, text, name, declaredFields, allFields, null, usages)
    }
}
