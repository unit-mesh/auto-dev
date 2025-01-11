package cc.unitmesh.devti.runner

import com.intellij.execution.*
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

interface ConfigurationRunner {
    fun runnerId() = DefaultRunExecutor.EXECUTOR_ID

    fun executeRunConfigurations(
        project: Project,
        settings: RunnerAndConfigurationSettings,
        testEventsListener: SMTRunnerEventsAdapter? = null,
        indicator: ProgressIndicator? = null,
    ) {
        val runContext = createRunContext()
        executeRunConfigures(project, settings, runContext, testEventsListener, indicator)
    }

    fun executeRunConfigures(
        project: Project,
        settings: RunnerAndConfigurationSettings,
        runContext: RunContext,
        testEventsListener: SMTRunnerEventsAdapter?,
        indicator: ProgressIndicator?,
    ) {
        val connection: MessageBusConnection? = project.messageBus.connect()
        try {
            return executeRunConfigurations(connection, settings, runContext, testEventsListener, indicator)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * This function is responsible for executing run configurations with the given parameters.
     *
     * @param connection The message bus connection to use.
     * @param configurations The runner and configuration settings to execute.
     * @param runContext The run context for the execution.
     * @param testEventsListener The listener for test events.
     * @param indicator The progress indicator for the execution.
     */
    fun executeRunConfigurations(
        connection: MessageBusConnection?,
        configurations: RunnerAndConfigurationSettings,
        runContext: RunContext,
        testEventsListener: SMTRunnerEventsListener?,
        indicator: ProgressIndicator?,
    ) {
        testEventsListener?.let {
            connection?.subscribe(SMTRunnerEventsListener.TEST_STATUS, it)
        }
        connection?.let {
            Disposer.register(runContext, connection)
        }

        runInEdt {
            try {
                configurations.startRunConfigurationExecution(runContext)
                val handler = CheckExecutionListener(runnerId(), runContext)
                connection?.subscribe(ExecutionManager.EXECUTION_TOPIC, handler)
            } catch (e: ExecutionException) {
                logger<ConfigurationRunner>().warn("Failed to start run configuration: ${configurations.name}")
                runContext.latch.countDown()
            }
        }

        // todo: find a better way
        if (indicator != null) {
            while (!indicator.isCanceled) {
                val result = runContext.latch.await(100, TimeUnit.MILLISECONDS)
                if (result) break
            }

            if (indicator.isCanceled) {
                Disposer.dispose(runContext)
            }
        }
    }

    fun createRunContext(): RunContext {
        val stderr = StringBuilder()
        val processListener = object : OutputListener() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (ProcessOutputType.isStderr(outputType)) {
                    stderr.append(event.text)
                }
            }
        }

        val runContext = RunContext(processListener, null, CountDownLatch(1))
        return runContext
    }


    /**
     * This function defines a process run completion action to be executed once a process run by the program runner completes.
     * It is designed to handle the aftermath of a process execution, including stopping the process and notifying the run context.
     *
     * @param runContext The context in which the run operation is being executed. It provides the necessary information
     *                   and handles to manage the run process, including a latch to synchronize the completion of the run.
     *                   The run context is also responsible for disposing of resources once the run completes.
     *
     * Note: This function uses the 'return@Callback' syntax to exit the lambda expression early in case of a null descriptor.
     */
    fun processRunCompletionAction(runContext: RunContext) = ProgramRunner.Callback { descriptor ->
        // Descriptor can be null in some cases.
        // For example, IntelliJ Rust's test runner provides null here if compilation fails
        if (descriptor == null) {
            runContext.latch.countDown()
            return@Callback
        }

        Disposer.register(runContext) {
            ExecutionManagerImpl.stopProcess(descriptor)
        }
        val processHandler = descriptor.processHandler
        if (processHandler != null) {
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    runContext.latch.countDown()
                }
            })
            runContext.processListener?.let {
                processHandler.addProcessListener(it)
            }
        }
    }

    @Throws(ExecutionException::class)
    fun RunnerAndConfigurationSettings.startRunConfigurationExecution(runContext: RunContext): Boolean {
        val runner = ProgramRunner.getRunner(runnerId(), configuration)
        val env =
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), this)
                .activeTarget()
                .build(processRunCompletionAction(runContext))

        if (runner == null || env.state == null) {
            runContext.latch.countDown()
            return false
        }

        runContext.environments.add(env)
        try {
            runner.execute(env)
        } catch (e: ExecutionException) {
            runContext.latch.countDown()
            throw e
        }
        return true
    }

    fun executeRunConfigurations(project: Project, configuration: RunConfiguration) {
        val runManager = RunManager.getInstance(project)
        val settings = runManager.findConfigurationByTypeAndName(configuration.type, configuration.name)

        if (settings == null) {
            logger<ConfigurationRunner>().warn("Failed to find test configuration for: ${configuration.name}")
            return
        }

        executeRunConfigurations(project, settings)
    }
}

