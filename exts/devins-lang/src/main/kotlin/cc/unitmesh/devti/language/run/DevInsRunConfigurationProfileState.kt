package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.agent.CustomAgentExecutor
import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.LLMCoroutineScope
import com.intellij.build.process.BuildProcessHandler
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.filters.Filter
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.ui.components.panels.NonOpaquePanel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.io.OutputStream
import javax.swing.JComponent

open class DevInsRunConfigurationProfileState(
    private val myProject: Project,
    private val configuration: DevInsConfiguration,
) : RunProfileState {
    private val llm: LLMProvider = LlmFactory.instance.create(myProject)

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler: ProcessHandler = createProcessHandler(configuration.name)
        ProcessTerminatedListener.attach(processHandler)
        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                // Done
                println(event.text)
            }
        })

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
        console.addMessageFilter(object : com.intellij.execution.filters.Filter {
            override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
//                println("Filtering: $line")
                return null
            }
        })

        console.attachToProcess(processHandler)

        val file: DevInFile? = lookupDevInsFile(myProject, configuration.getScriptPath())
        if (file == null) {
            console.print("File not found: ${configuration.getScriptPath()}", ConsoleViewContentType.ERROR_OUTPUT)
            processHandler.destroyProcess()
            return DefaultExecutionResult(console, processHandler)
        }

        val compiler = DevInsCompiler(myProject, file)
        val compileResult = compiler.compile()

        val output = compileResult.output

        val agent = compileResult.workingAgent

        if (agent != null) {
            agentRun(output, console, processHandler, agent)
        } else {
            defaultRun(output, console, processHandler, compileResult.isLocalCommand, executor, runner)
        }

        return DefaultExecutionResult(console, processHandler)
    }

    private fun agentRun(
        output: String,
        console: ConsoleViewWrapperBase,
        processHandler: ProcessHandler,
        agent: CustomAgentConfig
    ) {
        output.split("\n").forEach {
            if (it.contains("<DevInsError>")) {
                console.print(it, ConsoleViewContentType.LOG_ERROR_OUTPUT)
            } else {
                console.print(it, ConsoleViewContentType.USER_INPUT)
            }
            console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
        }

        console.print("\n--------------------\n", ConsoleViewContentType.NORMAL_OUTPUT)

        ApplicationManager.getApplication().invokeLater {
            val stringFlow: Flow<String>? = CustomAgentExecutor(project = myProject).execute(output, agent)
            if (stringFlow != null) {
                LLMCoroutineScope.scope(myProject).launch {
                    runBlocking {
                        stringFlow.collect {
                            console.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                        }
                    }

                    console.print("\nDone!", ConsoleViewContentType.SYSTEM_OUTPUT)
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
        executor: Executor?,
        runner: ProgramRunner<*>
    ) {
        // contains <DevInsError> means error
        output.split("\n").forEach {
            if (it.contains("<DevInsError>")) {
                console.print(it, ConsoleViewContentType.LOG_ERROR_OUTPUT)
            } else {
                console.print(it, ConsoleViewContentType.USER_INPUT)
            }
            console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
        }

        console.print("\n--------------------\n", ConsoleViewContentType.NORMAL_OUTPUT)

        ApplicationManager.getApplication().invokeLater {
            if (isLocalMode) {
                console.print("Local command detected, running in local mode", ConsoleViewContentType.SYSTEM_OUTPUT)
                processHandler.detachProcess()
                return@invokeLater
            }

            LLMCoroutineScope.scope(myProject).launch {
                runBlocking {
                    llm.stream(output, "").collect {
                        console.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                }

                console.print("\nDone!", ConsoleViewContentType.SYSTEM_OUTPUT)
                processHandler.detachProcess()
            }
        }
    }

    @Throws(ExecutionException::class)
    private fun createProcessHandler(myExecutionName: String): ProcessHandler = object : BuildProcessHandler() {
        override fun detachIsDefault(): Boolean = false
        override fun destroyProcessImpl() = Unit
        override fun detachProcessImpl() = notifyProcessTerminated(0)
        override fun getProcessInput(): OutputStream? = null
        override fun getExecutionName(): String = myExecutionName
    }
}

fun lookupDevInsFile(project: Project, path: String) = VirtualFileManager.getInstance()
    .findFileByUrl("file://$path")
    ?.let {
        PsiManager.getInstance(project).findFile(it)
    } as? DevInFile
