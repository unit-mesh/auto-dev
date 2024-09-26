package cc.unitmesh.devti.util

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager

object InsertUtil {
    fun insertStringAndSaveChange(
        project: Project,
        content: String,
        document: Document,
        startOffset: Int,
        withReformat: Boolean,
    ) {
        document.insertString(startOffset, content)
        PsiDocumentManager.getInstance(project).commitDocument(document)

        if (!withReformat) return

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        psiFile?.let { file ->
            val reformatRange = TextRange(startOffset, startOffset + content.length)
            CodeStyleManager.getInstance(project).reformatText(file, listOf(reformatRange))
        }
    }

    fun insertStreamingToDoc(project: Project, char: String, editor: Editor, currentOffset: Int) {
        WriteCommandAction.runWriteCommandAction(
            project,
            AutoDevBundle.message("intentions.chat.code.complete.name"),
            "intentions.write.action",
            {
                insertStringAndSaveChange(project, char, editor.document, currentOffset, false)
            })

        editor.caretModel.moveToOffset(currentOffset + char.length)
        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }

    fun replaceText(project: Project, editor: Editor, element: PsiElement?, output: String) {
        val primaryCaret = editor.caretModel.primaryCaret;
        val start = runReadAction { primaryCaret.selectionStart }
        val end = runReadAction { primaryCaret.selectionEnd }
        val document = runReadAction { editor.document }

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, output)
        }
    }
}