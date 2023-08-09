package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.LivingDocumentation
import cc.unitmesh.devti.provider.LivingDocumentationType
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil

class JavaLivingDocumentation : LivingDocumentation {
    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return when (type) {
            LivingDocumentationType.NORMAL -> Pair("/**", "*/")
            LivingDocumentationType.ANNOTATED -> Pair("", "")
            LivingDocumentationType.LIVING -> Pair("", "")
        }
    }

    override fun updateDoc(psiElement: PsiElement, str: String) {
        TODO("Not yet implemented")
    }

    override fun findExampleDoc(psiNameIdentifierOwner: PsiNameIdentifierOwner): String {
        return ""
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        if (psiElement is PsiMethod || psiElement is PsiClass) return psiElement as PsiNameIdentifierOwner

        val closestIdentifierOwner = PsiTreeUtil.getParentOfType(psiElement, PsiNameIdentifierOwner::class.java)
        if (closestIdentifierOwner !is PsiMethod) {
            return PsiTreeUtil.getParentOfType(psiElement, PsiMethod::class.java) ?: closestIdentifierOwner
        }

        return closestIdentifierOwner

    }

    override fun findDocTargetsInSelection(
        psiElement: PsiElement,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        TODO("Not yet implemented")
    }

}