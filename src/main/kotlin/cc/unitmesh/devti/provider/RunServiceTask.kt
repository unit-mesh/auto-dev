// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.provider

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionManager
import com.intellij.execution.OutputListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.CountDownLatch

class RunServiceTask(
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
        doRun(indicator)
    }

    private fun doRun(indicator: ProgressIndicator): String? {
        var settings: RunnerAndConfigurationSettings? = runService.createRunSettings(project, virtualFile)
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
                CheckExecutionListener(runnerId(), context)
            )

            configurations.startRunConfigurationExecution(context)
        }

        // if run in Task, Disposer.dispose(context)
    }

    @Throws(ExecutionException::class)
    private fun RunnerAndConfigurationSettings.startRunConfigurationExecution(context: Context): Boolean {
        val runner = ProgramRunner.getRunner(runnerId(), configuration)
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

        Disposer.register(context) {
            ExecutionManagerImpl.stopProcess(descriptor)
        }
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