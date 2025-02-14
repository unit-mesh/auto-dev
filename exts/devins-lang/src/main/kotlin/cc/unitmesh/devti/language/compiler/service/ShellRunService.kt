package cc.unitmesh.devti.language.compiler.service

import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.sh.psi.ShFile
import com.intellij.sh.run.ShConfigurationType
import com.intellij.sh.run.ShRunConfiguration
import com.intellij.sh.run.ShRunner
import com.intellij.testFramework.LightVirtualFile

class ShellRunService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return file.extension == "sh" || file.extension == "bash"
    }

    override fun runFile(project: Project, virtualFile: VirtualFile, psiElement: PsiElement?, isFromToolAction: Boolean): String? {
        val workingDirectory = if (virtualFile is LightVirtualFile) {
            project.basePath!!
        } else {
            virtualFile.parent.path
        }

        val shRunner = ApplicationManager.getApplication().getService(ShRunner::class.java)
            ?: return "Shell runner not found"

        if (shRunner.isAvailable(project)) {
            shRunner.run(project, virtualFile.path, workingDirectory, "Shell Run Service", true)
        }

        return "Running shell command: ${virtualFile.path}"
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile> = ShRunConfiguration::class.java
    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val psiFile = runReadAction {
            PsiManager.getInstance(project).findFile(virtualFile) as? ShFile
        } ?: return null

        val configurationSetting = RunManager.getInstance(project)
            .createConfiguration(psiFile.name, ShConfigurationType.getInstance())

        val configuration = configurationSetting.configuration as ShRunConfiguration
        configuration.scriptPath = virtualFile.path
        return configurationSetting.configuration
    }
}