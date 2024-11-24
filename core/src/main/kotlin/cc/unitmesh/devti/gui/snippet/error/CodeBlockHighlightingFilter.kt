// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.gui.snippet.error

import com.intellij.temporary.gui.block.AutoDevSnippetFile
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

class CodeBlockHighlightingFilter : HighlightInfoFilter {
    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        val hasError = highlightInfo.severity >= HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING;
        if (file == null || !hasError) return true
        val virtualFile = file.virtualFile ?: return true

        return !(AutoDevSnippetFile.isSnippet(virtualFile))
    }
}
