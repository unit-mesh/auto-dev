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