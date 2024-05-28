package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class JavaScriptRefactoringTool : RefactoringTool {
    val project = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun lookupFile(path: String): PsiFile? {
        return null
    }

    override fun rename(sourceName: String, targetName: String, psiFile: PsiFile?): Boolean {
        return false
    }
}
