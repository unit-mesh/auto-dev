package cc.unitmesh.devti.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface RefactoringTool {
    fun lookupFile(path: String): PsiFile?

    fun rename(sourceName: String, targetName: String, psiFile: PsiFile?): Boolean

    fun safeDelete(element: PsiElement): Boolean

    /**
     * In Java the canonicalName is the fully qualified name of the target package.
     * In Kotlin the canonicalName is the fully qualified name of the target package or class.
     */
    fun move(element: PsiElement, canonicalName: String): Boolean

    companion object {
        private val languageExtension: LanguageExtension<RefactoringTool> =
            LanguageExtension("cc.unitmesh.refactoringTool")

        fun forLanguage(language: Language): RefactoringTool? {
            val refactoringTool = languageExtension.forLanguage(language)
            if (refactoringTool != null) {
                return refactoringTool
            }

            // If no refactoring tool is found for the specified language, return java
            val javaLanguage = Language.findLanguageByID("JAVA") ?: return null
            return languageExtension.forLanguage(javaLanguage)
        }
    }
}

data class RefactorInstElement(
    val isClass: Boolean,
    val isMethod: Boolean,
    val methodName: String,
    val canonicalName: String,
    val className: String,
    val pkgName: String
)
