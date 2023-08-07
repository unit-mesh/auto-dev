package cc.unitmesh.devti.gui.error

import com.intellij.temporary.gui.block.AutoDevSnippetFile
import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement

class CodeBlockHighlightErrorFilter : HighlightErrorFilter() {
    override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
        val containingFile = element.containingFile
        val highlightedFile = containingFile?.virtualFile ?: return true
        return !AutoDevSnippetFile.isSnippet(highlightedFile)
    }
}
