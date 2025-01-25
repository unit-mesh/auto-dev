package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.provider.RunService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiManager
import java.io.File

class AutoDevRunAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document)

        if (file != null) {
            e.presentation.isEnabled = RunService.provider(project, file) != null
            return
        }

        e.presentation.isEnabled = false
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val project = e.project ?: return

        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val psiFile = PsiManager.getInstance(project).findFile(file)
            ?: return

        val scratchFile = ScratchRootType.getInstance()
            .createScratchFile(project, file.name, psiFile.language, document.text)

        if (scratchFile == null) {
            AutoDevNotifications.warn(project, "Cannot create scratch file")
            return
        }

        if (scratchFile.extension == "sh") {
            File(scratchFile.path).setExecutable(true)
        }

        try {
            RunService.provider(project, file)
                ?.runFile(project, scratchFile, psiFile, isFromToolAction = true)
                ?: RunService.runInCli(project, psiFile)
                ?: AutoDevNotifications.notify(project, "No run service found for ${file.name}")
        } catch (e: Exception) {
            AutoDevNotifications.notify(project, "Run Failed: ${e.message}")
        }
    }
}
