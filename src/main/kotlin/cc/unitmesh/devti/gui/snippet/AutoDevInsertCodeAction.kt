package cc.unitmesh.devti.gui.snippet

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager


class AutoDevInsertCodeAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val selectionModel = if (editor.selectionModel.hasSelection()) editor.selectionModel else null
        val newText = selectionModel?.selectedText ?: editor.document.text.trimEnd()

        val textEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val currentSelection = textEditor.selectionModel

        WriteCommandAction.writeCommandAction(project).compute<Any, RuntimeException> {
            val offset: Int
            val document = textEditor.document

            if (currentSelection.hasSelection()) {
                offset = currentSelection.selectionStart
                document.replaceString(currentSelection.selectionStart, currentSelection.selectionEnd, newText)
            } else {
                offset = textEditor.caretModel.offset
                document.insertString(offset, newText)
            }

            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return@compute
            PsiDocumentManager.getInstance(project).commitDocument(document)
            CodeStyleManager.getInstance(project).reformatText(psiFile, offset, offset + newText.length)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor
        if (selectedTextEditor == null || !selectedTextEditor.document.isWritable) {
            e.presentation.isEnabled = false
        } else {
            e.presentation.isEnabledAndVisible = true
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
