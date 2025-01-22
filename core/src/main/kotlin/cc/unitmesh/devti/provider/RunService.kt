package cc.unitmesh.devti.provider

import cc.unitmesh.devti.runner.RunServiceTask
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.util.concurrent.CompletableFuture

interface RunService {
    private val logger: Logger get() = logger<RunService>()

    fun isApplicable(project: Project, file: VirtualFile): Boolean

    /**
     * Retrieves the run configuration class for the given project.
     *
     * @param project The project for which to retrieve the run configuration class.
     * @return The run configuration class for the project.
     */
    fun runConfigurationClass(project: Project): Class<out RunProfile>?

    /**
     * Creates a new run configuration for the given project and virtual file.
     *
     * 1. Looks up the PSI file from the virtual file using `PsiManager.getInstance(project).findFile(virtualFile)`.
     * 2. Creates a RunConfigurationSettings instance with the name "name" and the specified RunConfigurationType using `RunManager.getInstance(project).createConfiguration("name", RunConfigurationType)`.
     *
     * @param project The project for which to create the run configuration.
     * @param virtualFile The virtual file to associate with the run configuration.
     * @return The newly created RunConfiguration, or `null` if creation failed.
     */
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
    fun createRunSettings(
        project: Project,
        virtualFile: VirtualFile,
        testElement: PsiElement?,
    ): RunnerAndConfigurationSettings? {
        if (testElement != null) {
            val settings = createDefaultConfigurations(project, testElement)
            if (settings != null) {
                return settings
            }
        }

        val runManager = RunManager.getInstance(project)
        var testConfig = runManager.allConfigurationsList.firstOrNull {
            val runConfigureClass = runConfigurationClass(project)
            it.name == virtualFile.nameWithoutExtension && (it.javaClass == runConfigureClass)
        }

        if (testConfig == null) {
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

        settings.isTemporary = true
        runManager.selectedConfiguration = settings

        return settings
    }

    fun createDefaultConfigurations(
        project: Project,
        element: PsiElement,
    ): RunnerAndConfigurationSettings? {
        return runReadAction {
            ConfigurationContext(element).configurationsFromContext?.firstOrNull()?.configurationSettings
        }
    }

    fun PsiFile.collectPsiError(): MutableList<String> {
        val errors = mutableListOf<String>()
        val visitor = object : PsiSyntaxCheckingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PsiErrorElement) {
                    errors.add("Syntax error at position ${element.textRange.startOffset}: ${element.errorDescription}")
                }
                super.visitElement(element)
            }
        }

        this.accept(visitor)
        return errors
    }

    abstract class PsiSyntaxCheckingVisitor : com.intellij.psi.PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            runReadAction {
                element.children.forEach { it.accept(this) }
            }
        }
    }

    /**
     * This function is responsible for running a file within a specified project and virtual file. It is a synchronous operation.
     * [runFileAsync] should be used for asynchronous operations.
     *
     * It creates a run configuration using the provided parameters and then attempts to execute it using
     * the `ExecutionManager`. The function returns `null` if an error occurs during the configuration creation or execution process.
     *
     * @param project The project within which the file is to be run.
     * @param virtualFile The virtual file that represents the file to be run.
     * @return The result of the run operation, or `null` if an error occurred.
     */
    fun runFile(project: Project, virtualFile: VirtualFile, psiElement: PsiElement?, isFromToolAction: Boolean): String? {
        try {
            val runTask = RunServiceTask(project, virtualFile, psiElement, this)
            ProgressManager.getInstance().run(runTask)
        } catch (e: Exception) {
            logger.error("Failed to run file: ${virtualFile.name}", e)
            return e.message
        }

        return null
    }

    /**
     * This function is responsible for running a file within a specified project and virtual file asynchronously.
     *
     * @param project The project within which the file is to be run.
     * @param virtualFile The virtual file that represents the file to be run.
     * @return The result of the run operation, or `null` if an error occurred.
     */
    fun runFileAsync(project: Project, virtualFile: VirtualFile, psiElement: PsiElement?): String? {
        val future: CompletableFuture<String> = CompletableFuture<String>()

        try {
            val runTask = RunServiceTask(project, virtualFile, psiElement, this, future = future)
            ProgressManager.getInstance().run(runTask)
        } catch (e: Exception) {
            logger.error("Failed to run file: ${virtualFile.name}", e)
            future.completeExceptionally(e)
            return e.message
        }

        return future.get()
    }

    companion object {
        val EP_NAME: ExtensionPointName<RunService> = ExtensionPointName("cc.unitmesh.runService")

        fun provider(project: Project, file: VirtualFile): RunService? {
            return EP_NAME.extensionList.firstOrNull {
                runReadAction {
                    it.isApplicable(project, file)
                }
            }
        }

        fun runInCli(project: Project, psiFile: PsiFile, args: List<String>? = null): String? {
            logger<RunService>().info("Running file in CLI model: ${psiFile.virtualFile.name}")

            val commandLine = when (psiFile.language.displayName.lowercase()) {
                "python" -> GeneralCommandLine("python3", psiFile.virtualFile.path)
                "javascript" -> GeneralCommandLine("node", psiFile.virtualFile.path)
                "ecmascript 6" -> GeneralCommandLine("node", psiFile.virtualFile.path)
                "ruby" -> GeneralCommandLine("ruby", psiFile.virtualFile.path)
                "shell script" -> GeneralCommandLine("sh", psiFile.virtualFile.path)
                // kotlin script, `kotlinc -script hello.kts`
                "kotlin" -> GeneralCommandLine("kotlinc", "-script", psiFile.virtualFile.path)
                else -> {
                    logger<RunService>().warn("Unsupported language: ${psiFile.language.displayName}")
                    return null
                }
            }

            if (args != null) {
                commandLine.addParameters(args)
            }

            commandLine.setWorkDirectory(project.basePath)
            return try {
                val output = ExecUtil.execAndGetOutput(commandLine)
                output.stdout
            } catch (e: Exception) {
                e.printStackTrace()
                e.message
            }
        }

        fun runInCli(project: Project, virtualFile: VirtualFile, args: List<String>? = null): String? {
            val psiFile = runReadAction { PsiManager.getInstance(project).findFile(virtualFile) } ?: return null
            return runInCli(project, psiFile, args)
        }

        /**
         * We will handle Shire UnSupported FileType here
         */
        fun retryRun(project: Project, virtualFile: VirtualFile, args: List<String>? = null): String? {
            val defaultRunService = object : RunService {
                override fun isApplicable(project: Project, file: VirtualFile): Boolean = true
                override fun runConfigurationClass(project: Project): Class<out RunProfile>? = null
            }

            val file = runReadAction { PsiManager.getInstance(project).findFile(virtualFile) }

            defaultRunService.createRunSettings(project, virtualFile, file) ?: return null

            return defaultRunService.runFile(project, virtualFile, null, false)
        }
    }
}

