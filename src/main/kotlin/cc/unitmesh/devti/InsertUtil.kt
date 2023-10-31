package cc.unitmesh.devti

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
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
}