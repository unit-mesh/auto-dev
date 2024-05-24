// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.runner

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.Filter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.text.nullize
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class RunServiceTask(
    private val project: Project,
    private val virtualFile: VirtualFile,
    private val testElement: PsiElement?,
    private val runService: RunService,
    private val runner: ProgramRunner<*>? = null
) : com.intellij.openapi.progress.Task.Backgroundable(
    project,
    AutoDevBundle.message("progress.run.task"),
    true
) {
    private fun runnerId() = runner?.runnerId ?: DefaultRunExecutor.EXECUTOR_ID

    override fun run(indicator: ProgressIndicator) {
        runAndCollectTestResults(indicator)
    }

    /**
     * This function is responsible for executing a run configuration and returning the corresponding check result.
     * It is used within the test framework to run tests and report the results back to the user.
     *
     * @param indicator A progress indicator that is used to track the progress of the execution.
     * @return The check result of the executed run configuration, or `null` if no run configuration could be created.
     */
    private fun runAndCollectTestResults(indicator: ProgressIndicator?): RunnerResult? {
        val settings: RunnerAndConfigurationSettings? = runService.createRunSettings(project, virtualFile, testElement)
        if (settings == null) {
            logger<RunServiceTask>().warn("No run configuration found for file: ${virtualFile.path}")
            return null
        }

        settings.isActivateToolWindowBeforeRun = false

        val stderr = StringBuilder()
        val processListener = object : OutputListener() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (ProcessOutputType.isStderr(outputType)) {
                    stderr.append(event.text)
                }
            }
        }

        val testRoots = mutableListOf<SMTestProxy.SMRootTestProxy>()
        val testEventsListener = object : SMTRunnerEventsAdapter() {
            override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
                testRoots += testsRoot
            }
        }
        val runContext = RunContext(processListener, null, CountDownLatch(1))
        executeRunConfigures(project, settings, runContext, testEventsListener, indicator)

        @Suppress("UnstableApiUsage")
        invokeAndWaitIfNeeded { }

        val testResults = testRoots.map { it.toCheckResult() }
        if (testResults.isEmpty()) return RunnerResult.noTestsRun

        val firstFailure = testResults.firstOrNull { it.status != RunnerStatus.Solved }
        val result = firstFailure ?: testResults.first()
        return result
    }

    protected fun SMTestProxy.SMRootTestProxy.toCheckResult(): RunnerResult {
        if (finishedSuccessfully()) return RunnerResult(RunnerStatus.Solved, "CONGRATULATIONS")

        val failedChildren = collectChildren(object : Filter<SMTestProxy>() {
            override fun shouldAccept(test: SMTestProxy): Boolean = test.isLeaf && !test.finishedSuccessfully()
        })

        val firstFailedTest = failedChildren.firstOrNull() ?: error("Testing failed although no failed tests found")
        val diff = firstFailedTest.diffViewerProvider?.let {
            CheckResultDiff(it.left, it.right, it.diffTitle)
        }
        val message = if (diff != null) getComparisonErrorMessage(firstFailedTest) else getErrorMessage(firstFailedTest)
        val details = firstFailedTest.stacktrace
        return RunnerResult(
            RunnerStatus.Failed,
            removeAttributes(fillWithIncorrect(message)),
            diff = diff,
            details = details
        )
    }

    private fun SMTestProxy.finishedSuccessfully(): Boolean {
        return !hasErrors() && (isPassed || isIgnored)
    }

    /**
     * Some testing frameworks add attributes to be shown in console (ex. Jest - ANSI color codes)
     * which are not supported in Task Description, so they need to be removed
     */
    private fun removeAttributes(text: String): String {
        val buffer = StringBuilder()
        AnsiEscapeDecoder().escapeText(text, ProcessOutputTypes.STDOUT) { chunk, _ ->
            buffer.append(chunk)
        }
        return buffer.toString()
    }

    /**
     * Returns message for test error that will be shown to a user in Check Result panel
     */
    @Suppress("UnstableApiUsage")
    @NlsSafe
    private fun getErrorMessage(node: SMTestProxy): String = node.errorMessage ?: "Execution failed"

    /**
     * Returns message for comparison error that will be shown to a user in Check Result panel
     */
    private fun getComparisonErrorMessage(node: SMTestProxy): String = getErrorMessage(node)

    private fun fillWithIncorrect(message: String): String =
        message.nullize(nullizeSpaces = true) ?: "Incorrect"

    fun executeRunConfigures(
        project: Project,
        settings: RunnerAndConfigurationSettings,
        runContext: RunContext,
        testEventsListener: SMTRunnerEventsAdapter,
        indicator: ProgressIndicator?
    ) {
        val connection = project.messageBus.connect()
        try {
            return executeRunConfigurations(connection, settings, runContext, testEventsListener, indicator)
        } finally {
            connection.disconnect()
        }
    }

    private fun executeRunConfigurations(
        connection: MessageBusConnection,
        configurations: RunnerAndConfigurationSettings,
        runContext: RunContext,
        testEventsListener: SMTRunnerEventsListener?,
        indicator: ProgressIndicator?
    ) {
        testEventsListener?.let {
            connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, it)
        }
        Disposer.register(connection, runContext)

        runInEdt {
            connection.subscribe(
                ExecutionManager.EXECUTION_TOPIC,
                CheckExecutionListener(runnerId(), runContext)
            )

            try {
                configurations.startRunConfigurationExecution(runContext)
            } catch (e: ExecutionException) {
                runContext.latch.countDown()
            }
        }

        while (indicator?.isCanceled != true) {
            val result = runContext.latch.await(100, TimeUnit.MILLISECONDS)
            if (result) break
        }

        if (indicator?.isCanceled == true) {
            Disposer.dispose(runContext)
        }
    }

    @Throws(ExecutionException::class)
    private fun RunnerAndConfigurationSettings.startRunConfigurationExecution(runContext: RunContext): Boolean {
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
        runner.execute(env)
        return true
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
}