package cc.unitmesh.devti.context.filebuilder

import cc.unitmesh.devti.context.FileContext
import com.intellij.psi.PsiFile

interface FileContextBuilder {
    fun getFileContext(paramPsiFile: PsiFile): FileContext?
}
