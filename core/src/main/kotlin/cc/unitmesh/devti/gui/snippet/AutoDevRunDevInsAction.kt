package cc.unitmesh.devti.gui.snippet

import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.readText
import com.intellij.psi.PsiManager
import cc.unitmesh.devti.provider.RunService

class AutoDevRunDevInsAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document)

        if (file != null) {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile != null) {
                e.presentation.isEnabled = RunService.provider(project, file) != null
                return
            }
        }

        e.presentation.isEnabled = false
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR) ?: return
        val project = e.project ?: return

        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val psiFile = PsiManager.getInstance(project).findFile(file)
            ?: return

        val scratchFile = ScratchRootType.getInstance()
            .createScratchFile(project, file.name, psiFile.language, file.readText())
            ?: return

        try {
            RunService.provider(project, file)?.runFile(
                project,
                scratchFile,
                psiFile,
            )
        } finally {
            runWriteAction {
                scratchFile.delete(this)
            }
        }
    }
}
