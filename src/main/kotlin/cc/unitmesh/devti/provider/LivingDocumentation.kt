package cc.unitmesh.devti.provider

import cc.unitmesh.devti.custom.LivingDocumentationType
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * The living documentation provider is responsible for providing the living documentation
 * 1. normal documentation
 * 2. annotated like Swagger
 * 3. living documentation
 */
interface LivingDocumentation {
    val docToolName: String

    val forbiddenRules: List<String>

    fun startEndString(type: LivingDocumentationType): Pair<String, String>

    fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor)

    fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner?

    fun findDocTargetsInSelection(root: PsiElement, selectionModel: SelectionModel): List<PsiNameIdentifierOwner>

    companion object {
        private val languageExtension: LanguageExtension<LivingDocumentation> =
            LanguageExtension("cc.unitmesh.livingDocumentation")

        fun forLanguage(language: Language): LivingDocumentation? {
            return languageExtension.forLanguage(language)
        }
    }
}