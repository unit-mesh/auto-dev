package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.provider.RunService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.io.File
import java.io.IOException


class AutoDevRunAction : DumbAwareAction(AutoDevBundle.message("autodev.run.action")) {
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
        var originPsiFile = PsiManager.getInstance(project).findFile(file)
            ?: return

        var scratchFile: VirtualFile? = ScratchRootType.getInstance()
            .createScratchFile(project, file.name, originPsiFile.language, document.text)

        if (scratchFile?.extension == "Dockerfile") {
            scratchFile = createDockerFile(project, document.text) ?: scratchFile
        }

        if (scratchFile == null) {
            AutoDevNotifications.warn(project, "Cannot create scratch file")
            return
        }

        if (scratchFile.extension == "sh") {
            File(scratchFile.path).setExecutable(true)
        }

        var psiFile = PsiManager.getInstance(project).findFile(scratchFile)
            ?: return

        try {
            RunService.provider(project, file)
                ?.runFile(project, scratchFile, psiFile, isFromToolAction = true)
                ?: RunService.runInCli(project, psiFile)
                ?: AutoDevNotifications.notify(project, "No run service found for ${file.name}")
        } catch (e: Exception) {
            AutoDevNotifications.notify(project, "Run Failed: ${e.message}")
        }
    }

    private fun createDockerFile(project: Project, text: @NlsSafe String): VirtualFile? {
        val projectDir = project.guessProjectDir()
        if (projectDir == null) {
            return null
        }

        return runWriteAction {
            try {
                // 在项目根目录创建名为 dockerfile 的文件
                var dockerfile = projectDir.findChild("Dockerfile")
                if (dockerfile == null) {
                    dockerfile = projectDir.createChildData(null, "Dockerfile")
                }

                dockerfile.setBinaryContent(text.toByteArray())
                return@runWriteAction dockerfile
            } catch (e: IOException) {
                return@runWriteAction null
            }
        }
    }
}
