package cc.unitmesh.python.provider

import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.run.PythonRunConfiguration
import com.jetbrains.python.run.PythonRunConfigurationProducer

class PythonRunService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean = file.extension == "py"

    override fun runConfigurationClass(project: Project): Class<out RunProfile> = PythonRunConfiguration::class.java

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val psiFile: PyFile = runReadAction {
            PsiManager.getInstance(project).findFile(virtualFile)
        } as? PyFile ?: return null

        val runManager = RunManager.getInstance(project)

        val context = runReadAction {
            ConfigurationContext(psiFile)
        }

        val configProducer = RunConfigurationProducer.getInstance(
            PythonRunConfigurationProducer::class.java
        )
        var settings = configProducer.findExistingConfiguration(context)

        if (settings == null) {
            val fromContext = configProducer.createConfigurationFromContext(context)
                ?: throw IllegalStateException("Failed to create configuration from context")

            settings = fromContext.configurationSettings
            runManager.setTemporaryConfiguration(settings)
        }

        return settings.configuration as PythonRunConfiguration
    }
}
