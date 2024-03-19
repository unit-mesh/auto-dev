package cc.unitmesh.devti.provider

import com.intellij.build.process.BuildProcessHandler
import com.intellij.execution.*
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.*
import java.nio.channels.Channels
import java.nio.channels.Pipe

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

    fun runFile(project: Project, virtualFile: VirtualFile): DefaultExecutionResult? {
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
        val executionManager = ExecutionManager.getInstance(project)
        val dataHolderBase = UserDataHolderBase()
        val processHandler = RunServiceHandler(settings.name, dataHolderBase)

        executionManager
            .restartRunProfile(
                project,
                executor,
                DefaultExecutionTarget.INSTANCE,
                settings,
                processHandler
            )

        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        ProcessTerminatedListener.attach(processHandler)
        consoleView.attachToProcess(processHandler)

        val defaultExecutionResult = DefaultExecutionResult(consoleView, processHandler)
        defaultExecutionResult.processHandler.processInput
        return defaultExecutionResult
    }
}

class RunServiceHandler(val myExecutionName: String, private val myDataHolder: UserDataHolderBase) :
    BuildProcessHandler() {
    private var myProcessInputWriter: OutputStream? = null
    private var myProcessInputReader: InputStream? = null

    init {
        try {
            val pipe = Pipe.open()
            myProcessInputReader = BufferedInputStream(Channels.newInputStream(pipe.source()))
            myProcessInputWriter = BufferedOutputStream(Channels.newOutputStream(pipe.sink()))
        } catch (e: IOException) {
            logger<RunService>().warn("Unable to setup process input", e)
        }

        myDataHolder.putUserData(RUN_INPUT_KEY, myProcessInputReader)
    }

    override fun notifyTextAvailable(text: String, outputType: Key<*>) {
        super.notifyTextAvailable(text, outputType)
    }

    override fun detachIsDefault(): Boolean = false
    override fun destroyProcessImpl() {
        notifyProcessDetached()
        closeInput()
    }

    override fun detachProcessImpl() {
        closeInput()
        notifyProcessTerminated(0)
    }

    override fun getProcessInput(): OutputStream {
        return myProcessInputWriter!!
    }

    protected fun closeInput() {
        val processInputWriter = myProcessInputWriter!!
        val processInputReader = myProcessInputReader!!
        myProcessInputWriter = null
        myProcessInputReader = null

        myDataHolder.putUserData(RUN_INPUT_KEY, null)

        StreamUtil.closeStream(processInputWriter)
        StreamUtil.closeStream(processInputReader)
    }

    override fun getExecutionName(): String = myExecutionName

    companion object {
        val RUN_INPUT_KEY: Key<InputStream> = Key.create("RUN_INPUT_KEY")
    }
}
