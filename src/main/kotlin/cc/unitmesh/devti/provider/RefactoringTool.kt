package cc.unitmesh.devti.provider

import com.intellij.codeInsight.daemon.impl.quickfix.SafeDeleteFix
import com.intellij.codeInspection.MoveToPackageFix
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.ThrowableRunnable

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
     * Performs a refactoring rename operation on a given PSI element within the specified project.
     * The method iterates through available renamer factories to find the appropriate one for the
     * element to be renamed. It then collects usages of the element, checks read-only status,
     * and performs the rename operation on all found usages, including those in comments if applicable.
     *
     * @param myProject The project in which the refactoring operation is taking place.
     * @param elementToRename The PSI element (which must implement PsiNamedElement) that is to be renamed.
     * @param newName The new name to assign to the element and its usages.
     *
     * Note: The method uses ProgressManager to search for usages and may prompt the user for read-only
     * file access. It also uses ApplicationManager to schedule the rename operation on the EDT.
     */
    fun performRefactoringRename(myProject: Project, elementToRename: PsiNamedElement, newName: String) {
        for (renamerFactory in AutomaticRenamerFactory.EP_NAME.extensionList) {
            if (!renamerFactory.isApplicable(elementToRename)) continue
            val usages: List<UsageInfo> = ArrayList()
            val renamer = renamerFactory.createRenamer(elementToRename, newName, ArrayList())
            if (!renamer.hasAnythingToRename()) continue

            val runnable = Runnable {
                ApplicationManager.getApplication().runReadAction {
                    renamer.findUsages(usages, false, false)
                }
            }

            if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    runnable, RefactoringBundle.message("searching.for.variables"), true, myProject
                )
            ) {
                return
            }

            if (!CommonRefactoringUtil.checkReadOnlyStatus(
                    myProject,
                    *PsiUtilCore.toPsiElementArray(renamer.elements)
                )
            ) return

            val performAutomaticRename = ThrowableRunnable<RuntimeException> {
                CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject)
                val classified = RenameProcessor.classifyUsages(renamer.elements, usages)
                for (element in renamer.elements) {
                    val newElementName = renamer.getNewName(element)
                    if (newElementName != null) {
                        val infos = classified[element]
                        RenameUtil.doRename(
                            element,
                            newElementName,
                            infos.toTypedArray(),
                            myProject,
                            RefactoringElementListener.DEAF
                        )
                    }
                }
            }

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.writeCommandAction(myProject)
                    .withName("Rename").run(performAutomaticRename)
            }
        }
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
