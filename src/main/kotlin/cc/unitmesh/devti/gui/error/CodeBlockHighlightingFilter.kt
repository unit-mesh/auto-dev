package cc.unitmesh.devti.gui.error

import cc.unitmesh.devti.gui.block.AutoDevSnippetFile.isSnippet
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiFile

class CodeBlockHighlightingFilter : HighlightInfoFilter {
    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        val virtualFile = file?.virtualFile ?: return true

        return highlightInfo.severity >= HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING && isSnippet(
            virtualFile
        )
    }
}
