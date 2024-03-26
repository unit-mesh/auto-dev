// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.runner

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.*
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
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
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        val runContext = RunContext(processListener, null, CountDownLatch(1))
        executeRunConfigures(project, settings, runContext, testEventsListener, indicator)

        @Suppress("UnstableApiUsage")
        invokeAndWaitIfNeeded {  }

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
        runContext: RunContext,
        testEventsListener: SMTRunnerEventsAdapter,
        indicator: ProgressIndicator
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
        indicator: ProgressIndicator
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

        while (!indicator.isCanceled) {
            val result = runContext.latch.await(100, TimeUnit.MILLISECONDS)
            if (result) break
        }

        if (indicator.isCanceled) {
            Disposer.dispose(runContext)
        }
    }

    @Throws(ExecutionException::class)
    private fun RunnerAndConfigurationSettings.startRunConfigurationExecution(runContext: RunContext): Boolean {
        val runner = ProgramRunner.getRunner(runnerId(), configuration)
        val env =
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), this)
                .activeTarget()
                .build(callback(runContext))

        if (runner == null || env.state == null) {
            runContext.latch.countDown()
            return false
        }

        runContext.environments.add(env)
        runner.execute(env)
        return true
    }

    fun callback(runContext: RunContext) = ProgramRunner.Callback { descriptor ->
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
            runContext.processListener?.let { processHandler.addProcessListener(it) }
        }
    }


    fun createDefaultTestConfigurations(project: Project, element: PsiElement): RunnerAndConfigurationSettings? {
        return ConfigurationContext(element).configurationsFromContext?.firstOrNull()?.configurationSettings
    }
}