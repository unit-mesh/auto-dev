package cc.unitmesh.devti.language.run

import com.intellij.build.process.BuildProcessHandler
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.terminal.TerminalExecutionConsole
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.Pipe

class DevInRunConfigurationProfileState(
    private val myProject: Project,
    private val configuration: AutoDevConfiguration,
) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler: ProcessHandler = createProcessHandler(configuration.name)
        ProcessTerminatedListener.attach(processHandler)

        val console: ConsoleView = TerminalExecutionConsole(myProject, processHandler)
        console.attachToProcess(processHandler)

        return DefaultExecutionResult(console, processHandler)
    }

    @Throws(ExecutionException::class)
    private fun createProcessHandler(myExecutionName: String): ProcessHandler {
        return object : BuildProcessHandler() {
            var myProcessInputWriter: OutputStream? = null
            var myProcessInputReader: InputStream? = null

            init {
                val pipe = Pipe.open()

                myProcessInputWriter = BufferedOutputStream(Channels.newOutputStream(pipe.sink()))
                myProcessInputReader = BufferedInputStream(Channels.newInputStream(pipe.source()))
            }

            override fun detachIsDefault(): Boolean = false

            override fun destroyProcessImpl() {
                try {
                    // cancel myTask
                    // myTask?.cancel()
                } finally {
                    closeInput()
                }
            }

            override fun detachProcessImpl() {
                try {
                    notifyProcessDetached()
                } finally {
                    closeInput()
                }
            }

            override fun getProcessInput(): OutputStream? {
                return myProcessInputWriter
            }

            override fun getExecutionName(): String {
                return myExecutionName
            }

            protected fun closeInput() {
                val processInputWriter = myProcessInputWriter
                val processInputReader = myProcessInputReader
                myProcessInputWriter = null
                myProcessInputReader = null

                StreamUtil.closeStream(processInputWriter)
                StreamUtil.closeStream(processInputReader)
            }
        }
    }
}
