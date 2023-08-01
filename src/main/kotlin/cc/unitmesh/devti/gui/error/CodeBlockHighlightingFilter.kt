package cc.unitmesh.devti.gui.error

import cc.unitmesh.devti.gui.block.AutoDevSnippetFile
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

class CodeBlockHighlightingFilter : HighlightInfoFilter {
    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        val hasError = highlightInfo.getSeverity().compareTo(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING) >= 0;

        if (file == null || !hasError) {
            return true;
        }

        val virtualFile = file.getVirtualFile();
        if (virtualFile != null && AutoDevSnippetFile.isSnippet(virtualFile)) {
            return false;
        }

        return true;
    }
}
