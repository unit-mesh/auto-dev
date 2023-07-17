package cc.unitmesh.devti

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager

object IdeHelper {
    fun formatCode(project: Project, document: Document, textRange: TextRange, suggestion: String) {
        PsiDocumentManager.getInstance(project).commitDocument(document)
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return
        val reformatRange = TextRange(textRange.startOffset, textRange.startOffset + suggestion.length)
        CodeStyleManager.getInstance(project).reformatText(psiFile, listOf(reformatRange))
    }


}