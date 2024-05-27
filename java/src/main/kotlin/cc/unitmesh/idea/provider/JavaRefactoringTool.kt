package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.codeInsight.daemon.impl.quickfix.SafeDeleteFix
import com.intellij.codeInspection.MoveToPackageFix
import com.intellij.jvm.analysis.quickFix.RenameQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner

class JavaRefactoringTool : RefactoringTool {
    override fun rename(project: Project, psiFile: PsiFile, element: PsiNameIdentifierOwner, newName: String): Boolean {
        val renameQuickFix = RenameQuickFix(element, newName)
        val startElement = element
        val endElement = element

        try {
            renameQuickFix.invoke(project, psiFile, startElement, endElement)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    override fun safeDelete(element: PsiElement): Boolean {
        val delete = SafeDeleteFix(element)
        try {
            delete.invoke(element.project, element.containingFile, element, element)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    /**
     * This method is used to move a given element to a specified target.
     *
     * @param element The PsiNameIdentifierOwner object that is to be moved. This object represents the element in
     * the PSI (Program Structure Interface) tree.
     * @param target The target location where the element is to be moved. This is a string representing the canonical
     * name of the target.
     *
     * @return A boolean value indicating the success of the operation. Returns true if the element is
     * successfully moved to the target location, false otherwise.
     */
    override fun move(element: PsiElement, canonicalName: String): Boolean {
        val file = element.containingFile
        val fix = MoveToPackageFix(file, canonicalName)

        try {
            fix.invoke(file.project, file, element, element)
        } catch (e: Exception) {
            return false
        }

        return true
    }
}
