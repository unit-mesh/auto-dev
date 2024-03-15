package cc.unitmesh.devti.language.run

import com.intellij.build.process.BuildProcessHandler
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.StreamUtil
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

        val console: ConsoleView = ConsoleViewWrapperBase(ConsoleViewImpl(myProject, true))

        console.attachToProcess(processHandler)
        processHandler.startNotify()

        console.print("Hello, World!", ConsoleViewContentType.NORMAL_OUTPUT)
        processHandler.destroyProcess()

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

                // execute default process destroy
                super.destroyProcess()
            }

            override fun detachProcessImpl() {
                try {
                    notifyProcessDetached()
                } finally {
                    closeInput()
                }

                super.detachProcess()
            }

            override fun getProcessInput(): OutputStream? = myProcessInputWriter
            override fun getExecutionName(): String = myExecutionName

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
