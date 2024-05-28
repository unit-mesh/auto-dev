package cc.unitmesh.devti.provider

import com.intellij.codeInsight.daemon.impl.quickfix.SafeDeleteFix
import com.intellij.codeInspection.MoveToPackageFix
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * RefactoringTool is an interface that defines operations for performing various code refactoring tasks.
 * It provides functionality to work with PsiFiles and PsiElements, such as looking up files, renaming,
 * safely deleting elements, and moving elements or packages to a new location.
 */
interface RefactoringTool {
    /**
     * Looks up and retrieves a PsiFile from the given file path.
     *
     * @param path The path of the file to be looked up. It should be a valid file path within the project.
     * @return A PsiFile object if the file is found and successfully loaded, or null if the file doesn't exist or cannot be loaded.
     */
    fun lookupFile(path: String): PsiFile?

    /**
     * Renames a given source name to a target name within the provided PSI file.
     *
     * @param sourceName The original name to be renamed.
     * @param targetName The new name to replace the original name with.
     * @param psiFile The PSI file where the renaming will take place; can be null if not applicable.
     * @return A boolean value indicating whether the renaming was successful. Returns true if the
     *         renaming was successful, false otherwise.
     */
    fun rename(sourceName: String, targetName: String, psiFile: PsiFile?): Boolean

    /**
     * Deletes the given PsiElement in a safe manner, ensuring that no syntax errors or unexpected behavior occur as a result.
     * The method performs checks before deletion to confirm that it is safe to remove the element from the code structure.
     *
     * @param element The PsiElement to be deleted. This should be a valid element within the PSI tree structure.
     * @return true if the element was successfully deleted without any issues, false otherwise. This indicates whether
     * the deletion was performed and considered safe.
     */
    fun safeDelete(element: PsiElement): Boolean {
        val delete = SafeDeleteFix(element)
        try {
            delete.invoke(element.project, element.containingFile, element, element)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    /**
     * In Java the canonicalName is the fully qualified name of the target package.
     * In Kotlin the canonicalName is the fully qualified name of the target package or class.
     */
    fun move(element: PsiElement, canonicalName: String): Boolean {
        val file = element.containingFile
        val fix = MoveToPackageFix(file, canonicalName)

        try {
            fix.invoke(file.project, file, element, element)
        } catch (e: Exception) {
            return false
        }

        return true
    }

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
