package cc.unitmesh.devti.provider

import com.intellij.execution.*
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.messages.MessageBusConnection
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch

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
        var settings: RunnerAndConfigurationSettings? = createRunSettings(project, virtualFile)
        if (settings == null) {
            settings = createDefaultTestConfigurations(project, testElement ?: return null) ?: return null
        }

        settings.isActivateToolWindowBeforeRun = false

        val stderr = StringBuilder()
        val processListener = object : OutputListener() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val text = event.text
                if (text != null && ProcessOutputType.isStderr(outputType)) {
                    stderr.append(text)
                }
            }
        }

        val testRoots = mutableListOf<SMTestProxy.SMRootTestProxy>()
        val testEventsListener = object : SMTRunnerEventsAdapter() {
            override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
                testRoots += testsRoot
            }
        }

        executeRunConfigures(project, settings, processListener, testEventsListener)

        @Suppress("UnstableApiUsage")
        invokeAndWaitIfNeeded {}
//        val testResults = testRoots.map { it.toCheckResult() }

        val output = processListener.output
        val errorOutput = output.stderr

        if (output.exitCode != 0) {
            return errorOutput
        }

        val outputString = output.stdout
        return outputString
    }

    fun executeRunConfigures(
        project: Project,
        settings: RunnerAndConfigurationSettings,
        processListener: OutputListener,
        testEventsListener: SMTRunnerEventsAdapter
    ) {
        val connection = project.messageBus.connect()
        try {
            return executeRunConfigurations(connection, settings, processListener, testEventsListener)
        } finally {
//            connection.disconnect()
        }
    }

    private fun executeRunConfigurations(
        connection: MessageBusConnection,
        configurations: RunnerAndConfigurationSettings,
        processListener: ProcessListener?,
        testEventsListener: SMTRunnerEventsListener?
    ) {
        testEventsListener?.let {
            connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, it)
        }
        val context = Context(processListener, null, CountDownLatch(1))
        Disposer.register(connection, context)

        runInEdt {
            connection.subscribe(
                ExecutionManager.EXECUTION_TOPIC,
                CheckExecutionListener(DefaultRunExecutor.EXECUTOR_ID, context)
            )

            configurations.startRunConfigurationExecution(context)
        }

        //        if run in Task, Disposer.dispose(context)
    }


    /**
     * Returns `true` if configuration execution is started successfully, `false` otherwise
     */
    @Throws(ExecutionException::class)
    private fun RunnerAndConfigurationSettings.startRunConfigurationExecution(context: Context): Boolean {
        val runner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, configuration)
        val env =
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), this)
                .activeTarget()
                .build(callback(context))

        if (runner == null || env.state == null) {
            context.latch.countDown()
            return false
        }

        context.environments.add(env)
        runner.execute(env)
        return true
    }

    fun callback(context: Context) = ProgramRunner.Callback { descriptor ->
        // Descriptor can be null in some cases.
        // For example, IntelliJ Rust's test runner provides null here if compilation fails
        if (descriptor == null) {
            context.latch.countDown()
            return@Callback
        }

        Disposer.register(context, Disposable {
            ExecutionManagerImpl.stopProcess(descriptor)
        })
        val processHandler = descriptor.processHandler
        if (processHandler != null) {
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    context.latch.countDown()
                }
            })
            context.processListener?.let { processHandler.addProcessListener(it) }
        }
    }


    fun createDefaultTestConfigurations(project: Project, element: PsiElement): RunnerAndConfigurationSettings? {
        return ConfigurationContext(element).configurationsFromContext?.firstOrNull()?.configurationSettings
    }
}

private class CheckExecutionListener(
    private val executorId: String,
    private val context: Context,
) : ExecutionListener {
    override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processStartScheduled(executorId, env)
        }
    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            context.latch.countDown()
            context.executionListener?.processNotStarted(executorId, env)
        }
    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processStarting(executorId, env)
        }
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processStarted(executorId, env, handler)
        }
    }

    override fun processTerminating(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processTerminating(executorId, env, handler)
        }
    }

    override fun processTerminated(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int
    ) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processTerminated(executorId, env, handler, exitCode)
        }
    }

    private fun checkAndExecute(executorId: String, env: ExecutionEnvironment, action: () -> Unit) {
        if (this.executorId == executorId && env in context.environments) {
            action()
        }
    }
}

class Context(
    val processListener: ProcessListener?,
    val executionListener: ExecutionListener?,
    val latch: CountDownLatch
) : Disposable {
    val environments: MutableList<ExecutionEnvironment> = mutableListOf()
    override fun dispose() {}
}
