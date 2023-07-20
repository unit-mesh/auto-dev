package cc.unitmesh.idea.kotlin

import cc.unitmesh.devti.context.FileContext
import cc.unitmesh.devti.context.builder.FileContextBuilder
import com.intellij.psi.PsiFile

class KotlinFileContextBuilder : FileContextBuilder {
    override fun getFileContext(psiFile: PsiFile): FileContext? {
        return null
    }
}
