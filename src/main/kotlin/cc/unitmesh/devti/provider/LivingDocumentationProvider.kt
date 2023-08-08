package cc.unitmesh.devti.provider

import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

enum class LivingDocumentationProviderType {
    NORMAL,
    ANNOTATED,
    LIVING
}

/**
 * The living documentation provider is responsible for providing the living documentation
 * 1. normal documentation
 * 2. annotated like Swagger
 * 3. living documentation
 */
interface LivingDocumentationProvider {
    fun updateDoc(psiElement: PsiElement, str: String)

    fun findExampleDoc(psiNameIdentifierOwner: PsiNameIdentifierOwner): String

    fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner?

    fun findDocTargetsInSelection(psiElement: PsiElement, selectionModel: SelectionModel): List<PsiNameIdentifierOwner?>
}