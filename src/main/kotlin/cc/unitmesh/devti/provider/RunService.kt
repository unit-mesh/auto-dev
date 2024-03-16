package cc.unitmesh.devti.provider

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistryImpl
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunProfile
import com.intellij.ide.actions.runAnything.RunAnythingPopupUI
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface RunService {
    private val logger: Logger get() = logger<RunService>()

    /**
     * Retrieves the run configuration class for the given project.
     *
     * @param project The project for which to retrieve the run configuration class.
     * @return The run configuration class for the project.
     */
    fun runConfigurationClass(project: Project): Class<out RunProfile>?

    fun runTest(project: Project, virtualFile: VirtualFile) {
        val runManager = RunManager.getInstance(project)
        val testConfig = runManager.allConfigurationsList.firstOrNull {
            val runConfigureClass = runConfigurationClass(project)
            it.name == virtualFile.nameWithoutExtension && (it.javaClass == runConfigureClass)
        }

        if (testConfig == null) {
            AutoTestService.log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        val configurationSettings =
            runManager.findConfigurationByTypeAndName(testConfig.type, testConfig.name)

        if (configurationSettings == null) {
            AutoTestService.log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        AutoTestService.log.info("configurationSettings: $configurationSettings")
        runManager.selectedConfiguration = configurationSettings

        val executor: Executor = RunAnythingPopupUI.getExecutor()
        ExecutorRegistryImpl.RunnerHelper.run(
            project,
            testConfig,
            configurationSettings,
            DataContext.EMPTY_CONTEXT,
            executor
        )
    }
}
