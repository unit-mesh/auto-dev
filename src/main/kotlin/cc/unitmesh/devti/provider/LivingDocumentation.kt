package cc.unitmesh.devti.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
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
interface LivingDocumentation {
    fun updateDoc(psiElement: PsiElement, str: String)

    fun findExampleDoc(psiNameIdentifierOwner: PsiNameIdentifierOwner): String

    fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner?

    /**
     * Find the documentation targets in the selection, like the method, class
     */
    fun findDocTargetsInSelection(psiElement: PsiElement, selectionModel: SelectionModel): List<PsiNameIdentifierOwner>

    companion object {
        private val languageExtension: LanguageExtension<LivingDocumentation> =
            LanguageExtension("cc.unitmesh.livingDocumentation")

        fun forLanguage(language: Language): LivingDocumentation? {
            return languageExtension.forLanguage(language)
        }
    }
}