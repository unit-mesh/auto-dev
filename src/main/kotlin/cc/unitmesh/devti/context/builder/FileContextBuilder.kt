package cc.unitmesh.devti.context.builder

import cc.unitmesh.devti.context.FileContext
import com.intellij.psi.PsiFile

interface FileContextBuilder {
    fun getFileContext(paramPsiFile: PsiFile): FileContext?
}
