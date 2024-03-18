package cc.unitmesh.devti.provider

import com.intellij.build.process.BuildProcessHandler
import com.intellij.execution.*
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedOutputStream
import java.io.OutputStream
import javax.swing.JPanel

val RW_MAIN_CONFIGURATION_ID: Key<String> = Key<String>("RunWidgetMainRunConfigurationId")

interface RunService {
    private val logger: Logger get() = logger<RunService>()

    /**
     * Retrieves the run configuration class for the given project.
     *
     * @param project The project for which to retrieve the run configuration class.
     * @return The run configuration class for the project.
     */
    fun runConfigurationClass(project: Project): Class<out RunProfile>?

    fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? = null

    fun createConfiguration(project: Project, path: String): RunConfiguration? = null

    fun runFile(project: Project, virtualFile: VirtualFile): @NlsSafe String? {
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

        logger.info("configurationSettings: $settings")
        runManager.selectedConfiguration = settings

        val executor: Executor = DefaultRunExecutor()
        val dataContext = DataContext.EMPTY_CONTEXT
        val contentToReuse = RunContentDescriptor(
            ConsoleViewImpl(project, false),
            createProcessHandler(settings.name),
            JPanel(),
            settings.name
        )

        var builder = ExecutionEnvironmentBuilder.createOrNull(executor, settings) ?: return null
        builder = builder.activeTarget()
        builder = builder.contentToReuse(contentToReuse)

        val environment = builder.dataContext(dataContext).build()
        environment.contentToReuse = contentToReuse

        val executionManager = ExecutionManager.getInstance(project)
        executionManager
            .restartRunProfile(
                project,
                executor,
                DefaultExecutionTarget.INSTANCE,
                settings,
                createProcessHandler(settings.name)
            ) { executionEnvironment ->
                executionEnvironment.runProfile
            }

        invokeLater {
            val selectedContent = RunContentManager.getInstance(project).selectedContent
            val executionConsole = selectedContent?.executionConsole as? ConsoleViewImpl ?: return@invokeLater

            val document = executionConsole.editor.document
            val text = document.text
        }

        return ""
    }
}

@Throws(ExecutionException::class)
private fun createProcessHandler(myExecutionName: String): ProcessHandler = object : BuildProcessHandler() {
    override fun notifyTextAvailable(text: String, outputType: Key<*>) {
        logger<RunService>().warn("notifyTextAvailable: $text")
    }

    override fun addProcessListener(listener: ProcessListener) {
        super.addProcessListener(listener)
    }

    override fun detachIsDefault(): Boolean = false
    override fun destroyProcessImpl() = Unit
    override fun detachProcessImpl() = notifyProcessTerminated(0)
    override fun getProcessInput(): OutputStream? {
        return BufferedOutputStream(object : OutputStream() {
            override fun write(b: Int) {
                logger<RunService>().warn("write: $b")
            }
        })
    }

    override fun getExecutionName(): String = myExecutionName
}
