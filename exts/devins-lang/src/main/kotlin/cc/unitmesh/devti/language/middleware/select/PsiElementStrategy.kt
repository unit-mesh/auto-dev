package cc.unitmesh.devti.language.middleware.select

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface PsiElementStrategy {
    fun getElementToAction(project: Project?, editor: Editor?): PsiElement?
    fun getElementToAction(project: Project?, psiFile: PsiFile, range: TextRange): PsiElement?
}
