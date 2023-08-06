package cc.unitmesh.devti.intentions.error

import cc.unitmesh.devti.PsiUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

data class ErrorPlace(
    val hyperlinkText: String,
    val lineNumber: Int,
    val isProjectFile: Boolean,
    val virtualFile: VirtualFile,
    val project: Project
) {
    val psiFile: PsiFile?
        get() = PsiManager.getInstance(project).findFile(virtualFile)

    val programText: String
        get() = VfsUtilCore.loadText(virtualFile)

    fun findContainingElement(): PsiElement? {
        val psiFile = psiFile ?: return null
        val lineStartOffset = PsiUtils.getLineStartOffset(psiFile, lineNumber) ?: return null
        val errorPlaceOffset: Int = lineStartOffset
        return this.psiFile?.findElementAt(errorPlaceOffset)
    }

    fun getMarkDownLanguageSlug(): String? {
        val psiFile = psiFile ?: return null
        val language = psiFile.language
        val displayNameLowercased = language.displayName.lowercase()

        return when (displayNameLowercased) {
            "c#" -> "csharp"
            "c++" -> "cpp"
            else -> displayNameLowercased
        }
    }

}