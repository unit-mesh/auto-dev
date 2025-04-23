package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.agent.custom.CustomAgentExecutor
import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.language.ast.config.DevInsActionLocation
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.streaming.OnStreamingService
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.flow.DevInsConversationService
import cc.unitmesh.devti.language.run.runner.ShireConsoleView
import cc.unitmesh.devti.language.run.runner.ShireExecutionConsole
import cc.unitmesh.devti.language.run.runner.ShireRunner
import cc.unitmesh.devti.language.service.ConsoleService
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.*
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.panels.NonOpaquePanel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JComponent

open class DevInsRunConfigurationProfileState(
    private val myProject: Project,
    private val configuration: DevInsConfiguration,
) : RunProfileState, Disposable {
    private val llm: LLMProvider = LlmFactory.create(myProject)

    private var executionConsole: ShireExecutionConsole =
        ShireExecutionConsole(myProject, true, configuration = configuration)
    var console: ShireConsoleView = ShireConsoleView(executionConsole)

    var isShowRunContent = true

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler = ShireProcessHandler(configuration.name)
        ProcessTerminatedListener.attach(processHandler)

        val processAdapter = ShireProcessAdapter(configuration, console)
        processHandler.addProcessListener(processAdapter)

        console.attachToProcess(processHandler)
        
        ConsoleService.getInstance(myProject).setActiveConsole(executionConsole)

        val shireFile: DevInFile? = DevInFile.lookup(myProject, configuration.getScriptPath())
        if (shireFile == null) {
            console.print("File not found: ${configuration.getScriptPath()}", ConsoleViewContentType.ERROR_OUTPUT)
            processHandler.exitWithError()
            return DefaultExecutionResult(console, processHandler)
        }

        val shireRunner = ShireRunner(
            myProject, console, configuration, configuration.getVariables(), processHandler
        ).also {
            console.bindShireRunner(it)
            processHandler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    it.cancel()
                }
            })
        }

        console.print("Prepare for running ${configuration.name}...\n", ConsoleViewContentType.NORMAL_OUTPUT)
        AutoDevCoroutineScope.scope(myProject).launch {
            val parsedResult = ShireRunner.preAnalysisAndLocalExecute(shireFile, myProject)

            val location = parsedResult.config?.actionLocation
            if (location == DevInsActionLocation.TERMINAL_MENU || location == DevInsActionLocation.COMMIT_MENU) {
                isShowRunContent = false
            }

            val interaction = parsedResult.config?.interaction
            if (interaction == InteractionType.RightPanel) {
                isShowRunContent = false
            }

            try {
                val llmOutput = shireRunner.execute(parsedResult)
                processAdapter.setLlmOutput(llmOutput)

                processAdapter.processTerminated(ProcessEvent(processHandler, 0))
                myProject.getService(OnStreamingService::class.java)?.onDone(myProject)
            } catch (e: Exception) {
                console.print(
                    "Failed to run ${configuration.name}: ${e.message}\n",
                    ConsoleViewContentType.LOG_ERROR_OUTPUT
                )
                console.print(e.stackTraceToString(), ConsoleViewContentType.ERROR_OUTPUT)
            }

            if (!configuration.showConsole) {
                cleanup(configuration)
            }
        }

        return DefaultExecutionResult(console, processHandler)
    }

    override fun dispose() {
        console.dispose()
        executionConsole.dispose()
    }

    fun executeOld(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler = DevInsProcessHandler(configuration.name)
        ProcessTerminatedListener.attach(processHandler)

        val sb = StringBuilder()

//        processHandler.addProcessListener(object : ProcessAdapter() {
//            var result = ""
//            override fun processTerminated(event: ProcessEvent) {
//                super.processTerminated(event)
//
//                ApplicationManager.getApplication().messageBus
//                    .syncPublisher(DevInsRunListener.TOPIC)
//                    .runFinish(result, event, configuration.getScriptPath())
//            }
//
//            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
//                super.onTextAvailable(event, outputType)
//                result = sb.toString()
//            }
//        })

        val executionConsole = ConsoleViewImpl(myProject, true)
        val console = object : ConsoleViewWrapperBase(executionConsole) {
            override fun getComponent(): JComponent = myPanel
            private var myPanel: NonOpaquePanel = NonOpaquePanel(BorderLayout())

            init {
                val baseComponent = delegate.component
                myPanel.add(baseComponent, BorderLayout.CENTER)

                val actionGroup = DefaultActionGroup(*executionConsole.createConsoleActions())
                val toolbar = ActionManager.getInstance().createActionToolbar("BuildConsole", actionGroup, false)
                toolbar.targetComponent = baseComponent
                myPanel.add(toolbar.component, BorderLayout.EAST)
            }
        }

        // start message log in here
        console.addMessageFilter { line, _ ->
            sb.append(line)
            null
        }

        console.attachToProcess(processHandler)

        val file: DevInFile? = DevInFile.lookup(myProject, configuration.getScriptPath())
        if (file == null) {
            console.print("File not found: ${configuration.getScriptPath()}", ConsoleViewContentType.ERROR_OUTPUT)
            processHandler.destroyProcess()
            return DefaultExecutionResult(console, processHandler)
        }

        AutoDevCoroutineScope.scope(myProject).launch {
            val compiler = DevInsCompiler(myProject, file)
            val compileResult = compiler.compile()

            myProject.service<DevInsConversationService>()
                .createConversation(configuration.getScriptPath(), compileResult)

            val output = compileResult.output
            val agent = compileResult.executeAgent

            output.split("\n").forEach {
                if (it.contains(DEVINS_ERROR)) {
                    console.print(it, ConsoleViewContentType.LOG_ERROR_OUTPUT)
                } else {
                    console.print(it, ConsoleViewContentType.USER_INPUT)
                }
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
            }

            console.print("\n--------------------\n", ConsoleViewContentType.NORMAL_OUTPUT)

            if (output.contains(DEVINS_ERROR)) {
                processHandler.exitWithError()
            } else {
                if (agent != null) {
                    agentRun(output, console, processHandler, agent)
                } else {
                    defaultRun(output, console, processHandler, compileResult.isLocalCommand)
                }
            }
        }

        return DefaultExecutionResult(console, processHandler)
    }

    private fun agentRun(
        output: String,
        console: ConsoleViewWrapperBase,
        processHandler: ProcessHandler,
        agent: CustomAgentConfig
    ) {
        ApplicationManager.getApplication().invokeLater {
            val stringFlow: Flow<String>? = CustomAgentExecutor(project = myProject)
                .execute(output, agent, StringBuilder())
            if (stringFlow != null) {
                AutoDevCoroutineScope.scope(myProject).launch {
                    val llmResult = StringBuilder()

                    stringFlow.collect {
                        llmResult.append(it)
                        console.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                    }

                    console.print("\nDone!", ConsoleViewContentType.SYSTEM_OUTPUT)
                    myProject.service<DevInsConversationService>()
                        .updateLlmResponse(configuration.getScriptPath(), llmResult.toString())
                    processHandler.detachProcess()
                }
            }
        }
    }

    private fun defaultRun(
        output: String,
        console: ConsoleViewWrapperBase,
        processHandler: ProcessHandler,
        isLocalMode: Boolean,
    ) {
        ApplicationManager.getApplication().invokeLater {
            if (isLocalMode) {
                console.print("Local command detected, running in local mode", ConsoleViewContentType.SYSTEM_OUTPUT)
                processHandler.detachProcess()

                if (!configuration.showConsole) {
                    cleanup(configuration)
                }

                return@invokeLater
            }

            AutoDevCoroutineScope.scope(myProject).launch {
                val llmResult = StringBuilder()
                llm.stream(output, "", true).collect {
                    llmResult.append(it)
                    console.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                }

                console.print("\nDone!", ConsoleViewContentType.SYSTEM_OUTPUT)
                myProject.service<DevInsConversationService>()
                    .updateLlmResponse(configuration.getScriptPath(), llmResult.toString())
                processHandler.detachProcess()

                if (!configuration.showConsole) {
                    cleanup(configuration)
                }
            }
        }
    }

    private fun cleanup(configuration: DevInsConfiguration) {
        val virtualFile =
            VirtualFileManager.getInstance().findFileByUrl("file://${configuration.getScriptPath()}")
        val service: ScratchFileService = ScratchFileService.getInstance()
        if (virtualFile != null) {
            val scratchRootType = ScratchRootType.getInstance()
            val foundFile = runReadAction {
                service.findFile(scratchRootType, virtualFile.name, ScratchFileService.Option.existing_only)
            }

            if (foundFile != null) {
                AutoDevCoroutineScope.scope(myProject).launch {
                    runInEdt {
                        runWriteAction {
                            foundFile.delete(this)
                        }
                    }
                }
            }
        }
    }
}
