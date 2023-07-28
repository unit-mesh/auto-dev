package cc.unitmesh.kotlin.context

import cc.unitmesh.devti.context.VariableContext
import cc.unitmesh.devti.context.builder.VariableContextBuilder
import cc.unitmesh.idea.context.JavaContextCollectionUtilsKt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KotlinVariableContextBuilder : VariableContextBuilder {
    override fun getVariableContext(
        psiElement: PsiElement,
        includeMethodContext: Boolean,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): VariableContext? {
        when (psiElement) {
            is KtVariableDeclaration -> {
                val text = psiElement.text
                val name = psiElement.name
                val parentOfType = PsiTreeUtil.getParentOfType(psiElement, KtNamedFunction::class.java, true)
                val containingClass = psiElement.containingClass()
                val psiNameIdentifierOwner = psiElement as? PsiNameIdentifierOwner

                val usages = if (gatherUsages && psiNameIdentifierOwner != null) {
                    JavaContextCollectionUtilsKt.findUsages(psiNameIdentifierOwner)
                } else {
                    emptyList()
                }

                return VariableContext(psiElement, text, name, parentOfType, containingClass, usages, includeMethodContext, includeClassContext)
            }

            is KtParameter -> {
                val text = psiElement.text
                val name = psiElement.name
                val parentOfType = PsiTreeUtil.getParentOfType(psiElement, KtNamedFunction::class.java, true)
                val containingClass = psiElement.containingClass()
                val psiNameIdentifierOwner = psiElement as? PsiNameIdentifierOwner

                val usages = if (gatherUsages && psiNameIdentifierOwner != null) {
                    JavaContextCollectionUtilsKt.findUsages(psiNameIdentifierOwner)
                } else {
                    emptyList()
                }

                return VariableContext(psiElement, text, name, parentOfType, containingClass, usages, includeMethodContext, includeClassContext)
            }

            else -> {
                return null
            }
        }
    }
}
