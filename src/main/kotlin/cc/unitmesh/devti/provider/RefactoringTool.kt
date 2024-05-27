package cc.unitmesh.devti.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiNameIdentifierOwner

interface RefactoringTool {
    fun rename(element: PsiNameIdentifierOwner, newName: String): Boolean

    fun safeDelete(element: PsiNameIdentifierOwner): Boolean

    fun safeDelete(element: PsiNameIdentifierOwner, deleteReferences: Boolean): Boolean

    fun move(element: PsiNameIdentifierOwner, target: PsiNameIdentifierOwner): Boolean

    companion object {
        private val languageExtension: LanguageExtension<RefactoringTool> =
            LanguageExtension("cc.unitmesh.refactoringTool")

        fun forLanguage(language: Language): RefactoringTool? {
            val refactoringTool = languageExtension.forLanguage(language)
            if (refactoringTool != null) {
                return refactoringTool
            }

            return null
        }
    }
}