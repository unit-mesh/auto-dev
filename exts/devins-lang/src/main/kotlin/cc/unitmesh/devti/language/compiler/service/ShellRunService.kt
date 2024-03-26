package cc.unitmesh.devti.language.compiler.service

import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.sh.psi.ShFile
import com.intellij.sh.run.ShConfigurationType
import com.intellij.sh.run.ShRunConfiguration

class ShellRunService : RunService {
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = ShRunConfiguration::class.java
    override fun psiFileClass(project: Project): Class<out PsiElement> = ShFile::class.java

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val configurationSetting = RunManager.getInstance(project)
            .createConfiguration(virtualFile.name, ShConfigurationType.getInstance())

        val configuration = configurationSetting.configuration as ShRunConfiguration
        configuration.scriptPath = virtualFile.path
        return configurationSetting.configuration
    }

    override fun createRunSettings(project: Project, virtualFile: VirtualFile): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)
        this.createConfiguration(project, virtualFile)?.let {
            return runManager.findSettings(it)
        }

        return null
    }
}