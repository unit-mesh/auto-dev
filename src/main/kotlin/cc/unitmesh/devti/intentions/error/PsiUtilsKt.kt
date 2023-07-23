package cc.unitmesh.devti.intentions.error

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil


object PsiUtilsKt {
    fun getStartOffset(element: PsiElement): Int = element.textRange.startOffset
    fun getEndOffset(element: PsiElement): Int = element.textRange.endOffset

    fun getLineStartOffset(psiFile: PsiFile, line: Int): Int? {
        var document = psiFile.viewProvider.document
        if (document == null) {
            document = PsiDocumentManager.getInstance(psiFile.project)
                .getDocument(psiFile)
        }

        if (document != null && line >= 0 && line < document.lineCount) {
            val startOffset = document.getLineStartOffset(line)
            val element = psiFile.findElementAt(startOffset) ?: return startOffset
            if (element is PsiWhiteSpace || element is PsiComment) {
                val skipSiblingsForward = PsiTreeUtil.skipSiblingsForward(
                    element, PsiWhiteSpace::class.java, PsiComment::class.java
                )

                return if (skipSiblingsForward != null) getStartOffset(skipSiblingsForward) else startOffset
            }
            return startOffset
        }

        return null
    }

    fun getLineNumber(element: PsiElement, start: Boolean): Int {
        var document = element.containingFile.viewProvider.document
        if (document == null) {
            document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
        }

        val index = if (start) getStartOffset(element) else getEndOffset(element)
        return if (index > (document?.textLength ?: 0) || document == null) {
            0
        } else {
            document.getLineNumber(index)
        }
    }
}
