package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.codeInsight.daemon.impl.quickfix.SafeDeleteFix
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

    override fun safeDelete(element: PsiNameIdentifierOwner, deleteReferences: Boolean): Boolean {
        return false
    }

    override fun move(element: PsiNameIdentifierOwner, target: PsiNameIdentifierOwner): Boolean {
        return false
    }
}
