package cc.unitmesh.devti.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner

interface RefactoringTool {
    fun rename(project: Project, psiFile: PsiFile, element: PsiNameIdentifierOwner, newName: String): Boolean

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

            return null
        }
    }
}