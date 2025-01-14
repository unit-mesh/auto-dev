package cc.unitmesh.devti.language.compiler.service

import cc.unitmesh.devti.language.run.DevInsConfiguration
import cc.unitmesh.devti.language.run.DevInsConfigurationType
import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

class DevInRunService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return file.extension == "devin"
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile> = DevInsConfiguration::class.java

    override fun createRunSettings(
        project: Project,
        file: VirtualFile,
        testElement: PsiElement?
    ): RunnerAndConfigurationSettings? {
        val settings = RunManager.getInstance(project)
            .createConfiguration(file.name, DevInsConfigurationType::class.java)

        val runConfiguration = settings
            .configuration as DevInsConfiguration

        runConfiguration.setScriptPath(file.path)
        runConfiguration.name = file.nameWithoutExtension

        settings.isTemporary = true

        return settings
    }

    override fun runFile(project: Project, virtualFile: VirtualFile, psiElement: PsiElement?): String? {
        val settings = createRunSettings(project, virtualFile, psiElement) ?: return null
        val runConfiguration = settings.configuration as DevInsConfiguration

        val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
        val builder = ExecutionEnvironmentBuilder.createOrNull(executorInstance, runConfiguration)
            ?: return null

        ExecutionManager.getInstance(project).restartRunProfile(builder.build())
        return "Running DevIn file: ${virtualFile.name}"
    }
}