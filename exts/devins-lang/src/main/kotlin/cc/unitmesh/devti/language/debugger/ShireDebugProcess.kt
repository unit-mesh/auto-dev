package cc.unitmesh.devti.language.debugger

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.DevInsConfiguration
import cc.unitmesh.devti.language.run.DevInsRunConfigurationProfileState
import cc.unitmesh.devti.language.run.ShireProcessAdapter
import cc.unitmesh.devti.language.run.runner.ShireConsoleView
import cc.unitmesh.devti.language.run.runner.ShireRunner
import cc.unitmesh.devti.language.run.runner.ShireRunnerContext
import cc.unitmesh.devti.language.status.DevInsRunListener
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.content.Content
import com.intellij.util.Alarm
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import com.intellij.xdebugger.ui.XDebugTabLayouter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ShireDebugProcess(private val session: XDebugSession, private val environment: ExecutionEnvironment) :
    XDebugProcess(session), Disposable {
    private val debuggableConfiguration: DevInsConfiguration get() = session.runProfile as DevInsConfiguration
    private val runProfileState =
        debuggableConfiguration.getState(environment.executor, environment) as DevInsRunConfigurationProfileState

    private val connection = ApplicationManager.getApplication().messageBus.connect(this)
    private val myRequestsScheduler: Alarm

    init {
        myRequestsScheduler = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    }

    private val breakpointHandlers = arrayOf<XBreakpointHandler<*>>(
        ShireBreakpointHandler(
            this
        )
    )

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> = breakpointHandlers
    override fun createTabLayouter(): XDebugTabLayouter = ShireDebugTabLayouter(runProfileState.console)
    override fun createConsole(): ExecutionConsole = runProfileState.console

    var shireRunnerContext: ShireRunnerContext? = null

    fun start() {
        AutoDevCoroutineScope.scope(session.project).launch {
            runBlocking {
                val psiFile: DevInFile = DevInFile.lookup(session.project, debuggableConfiguration.getScriptPath())
                    ?: return@runBlocking

                shireRunnerContext = ShireRunner.compileOnly(session.project, psiFile, mapOf(), null)
                session.positionReached(ShireSuspendContext(this@ShireDebugProcess, session.project))
            }
        }

        val processAdapter = ShireProcessAdapter(debuggableConfiguration, runProfileState.console)
        processHandler.addProcessListener(processAdapter)
        runProfileState.console.print("Waiting for resume...", ConsoleViewContentType.NORMAL_OUTPUT)
    }

    override fun resume(context: XSuspendContext?) {
        connection.subscribe(DevInsRunListener.TOPIC, object : DevInsRunListener {
            override fun runFinish(
                allOutput: String,
                llmOutput: String,
                event: ProcessEvent,
                scriptPath: String,
                consoleView: ShireConsoleView?,
            ) {
                this@ShireDebugProcess.stop()
            }
        })

        runProfileState.execute(environment.executor, environment.runner)
    }

    override fun runToPosition(position: XSourcePosition, context: XSuspendContext?) {
        runProfileState.execute(environment.executor, environment.runner)
    }

    override fun startStepOut(context: XSuspendContext?) {
        runProfileState.execute(environment.executor, environment.runner)
    }

    override fun startStepInto(context: XSuspendContext?) {
        runProfileState.execute(environment.executor, environment.runner)
    }

    override fun startStepOver(context: XSuspendContext?) {
        runProfileState.execute(environment.executor, environment.runner)
    }

    override fun startForceStepInto(context: XSuspendContext?) {
        runProfileState.execute(environment.executor, environment.runner)
    }

    override fun startPausing() {

    }

    override fun stop() {
        connection.disconnect()
        processHandler.destroyProcess()
        session.stop()
    }

    override fun dispose() {
        connection.disconnect()
    }

    fun addBreakpoint(breakpoint: XLineBreakpoint<ShireBpProperties>) {

    }

    fun removeBreakpoint(breakpoint: XLineBreakpoint<ShireBpProperties>) {

    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider {
        return ShireDebuggerEditorsProvider()
    }
}

class ShireDebugTabLayouter(val console: ShireConsoleView) : XDebugTabLayouter() {
    override fun registerConsoleContent(ui: RunnerLayoutUi, console: ExecutionConsole): Content {
        val content = ui
            .createContent(
                "DebuggedConsoleContent", console.component, "Shire Debugged Console",
                AllIcons.Debugger.Console, console.preferredFocusableComponent
            )

        content.isCloseable = false
        ui.addContent(content, 1, PlaceInGrid.bottom, false)
        return content
    }
}


val RUNNER_ID: String = "ShireProgramRunner"