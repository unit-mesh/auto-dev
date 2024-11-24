// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.gui.snippet.error

import com.intellij.temporary.gui.block.AutoDevSnippetFile
import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement

class CodeBlockHighlightErrorFilter : HighlightErrorFilter() {
    override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
        val highlightedFile = element.containingFile?.virtualFile ?: return true
        return !AutoDevSnippetFile.isSnippet(highlightedFile)
    }
}
