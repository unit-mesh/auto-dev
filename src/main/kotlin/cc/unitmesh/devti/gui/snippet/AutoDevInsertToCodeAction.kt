package cc.unitmesh.devti.gui.snippet

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction

class AutoDevInsertFileAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val selectionModel = if (editor.selectionModel.hasSelection()) editor.selectionModel else null
        val textToPaste = selectionModel?.selectedText ?: editor.document.text.trimEnd()

        val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor
        WriteCommandAction.writeCommandAction(project).compute<Any, RuntimeException> {
            val offset = selectionModel?.selectionStart ?: selectedTextEditor?.document?.textLength ?: return@compute null
            selectedTextEditor?.document?.insertString(offset, textToPaste)
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
