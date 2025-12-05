package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.gui.snippet.container.AutoDevContainer
import cc.unitmesh.devti.provider.RunService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.json.JsonLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
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
import com.intellij.testFramework.LightVirtualFile
import java.io.File
import java.io.IOException


class AutoDevRunAction : AnAction(AutoDevBundle.message("autodev.run.action")) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document)

        if (file == null) {
            e.presentation.isEnabled = false
            return
        }

        val lightFile = file as? LightVirtualFile
        if (lightFile?.language == JsonLanguage.INSTANCE) {
            val virtualFile = AutoDevContainer.updateForDevContainer(project, file, document.text)
                ?: lightFile
            e.presentation.isEnabled = RunService.provider(project, virtualFile) != null
            return
        }

        e.presentation.isEnabled = RunService.provider(project, file) != null
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

        val extension = scratchFile?.extension
        when {
            extension == "Dockerfile" -> {
                scratchFile = createDockerFile(project, document.text) ?: scratchFile
            }

            extension?.lowercase() == "json" -> {
                scratchFile = AutoDevContainer.updateForDevContainer(project, file as LightVirtualFile, document.text)
                    ?: scratchFile
            }

            scratchFile?.extension == "sh" -> {
                File(scratchFile.path).setExecutable(true)
            }
        }

        if (scratchFile == null) {
            AutoDevNotifications.warn(project, "Cannot create scratch file")
            return
        }

        var psiFile = PsiManager.getInstance(project).findFile(scratchFile) ?: return

        try {
            RunService.provider(project, scratchFile)
                ?.runFile(project, scratchFile, psiFile, isFromToolAction = true)
                ?: RunService.runInCli(project, psiFile)
                ?: AutoDevNotifications.notify(project, "No run service found for ${file.name}")
        } catch (e: Exception) {
            AutoDevNotifications.notify(project, "Run Failed: ${e.message}")
        }
    }

    private fun createDockerFile(project: Project, text: String): VirtualFile? = runWriteAction {
        val projectDir = project.guessProjectDir()!!
        try {
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
