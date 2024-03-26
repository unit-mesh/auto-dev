package cc.unitmesh.devti.provider

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

interface RunService {
    private val logger: Logger get() = logger<RunService>()

    /**
     * Retrieves the run configuration class for the given project.
     *
     * @param project The project for which to retrieve the run configuration class.
     * @return The run configuration class for the project.
     */
    fun runConfigurationClass(project: Project): Class<out RunProfile>?

    fun psiFileClass(project: Project): Class<out PsiElement>

    fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? = null

    /**
     * Creates a new run configuration settings for the given project and virtual file.
     *
     * If a configuration with the same name already exists, it will be returned.
     * Otherwise, a new configuration is created and added to the run manager.
     *
     * @param project The project for which the configuration should be created.
     * @param virtualFile The virtual file for which the configuration should be created.
     * @return The created or found run configuration settings, or `null` if no suitable configuration could be
     */
    fun createRunSettings(project: Project, virtualFile: VirtualFile): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)
        var testConfig = runManager.allConfigurationsList.firstOrNull {
            val runConfigureClass = runConfigurationClass(project)
            it.name == virtualFile.nameWithoutExtension && (it.javaClass == runConfigureClass)
        }

        var isTemporary = false

        // try to create config if not founds
        if (testConfig == null) {
            isTemporary = true
            testConfig = createConfiguration(project, virtualFile)
        }

        if (testConfig == null) {
            logger.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return null
        }

        val settings = runManager.findConfigurationByTypeAndName(testConfig.type, testConfig.name)
        if (settings == null) {
            logger.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return null
        }

        if (isTemporary) {
            settings.isTemporary = true
        }

        runManager.selectedConfiguration = settings

        return settings
    }

    /**
     * This function is responsible for running a file within a specified project and virtual file.
     * It creates a run configuration using the provided parameters and then attempts to execute it using the `ExecutionManager`. The function returns `null` if an error occurs during the configuration creation or execution process.
     *
     * @param project The project within which the file is to be run.
     * @param virtualFile The virtual file that represents the file to be run.
     * @return The result of the run operation, or `null` if an error occurred.
     */
    fun runFile(project: Project, virtualFile: VirtualFile, testElement: PsiElement?): String? {
        val runTask = RunServiceTask(project, virtualFile, testElement, this)
        ProgressManager.getInstance().run(runTask)

        return null
    }
}

