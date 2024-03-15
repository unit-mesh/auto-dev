package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.compiler.DevInCompiler
import cc.unitmesh.devti.language.psi.DevInFile
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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import java.io.OutputStream

open class DevInRunConfigurationProfileState(
    private val myProject: Project,
    private val configuration: AutoDevConfiguration,
) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler: ProcessHandler = createProcessHandler(configuration.name)
        ProcessTerminatedListener.attach(processHandler)

        val console: ConsoleView = ConsoleViewWrapperBase(ConsoleViewImpl(myProject, true))
        console.attachToProcess(processHandler)

        val file: DevInFile? = VirtualFileManager.getInstance()
            .findFileByUrl("file://${configuration.getScriptPath()}")
            ?.let {
                PsiManager.getInstance(myProject).findFile(it)
            } as? DevInFile

        if (file == null) {
            console.print("File not found: ${configuration.getScriptPath()}", ConsoleViewContentType.ERROR_OUTPUT)
            processHandler.destroyProcess()
            return DefaultExecutionResult(console, processHandler)
        }

        val compiler = DevInCompiler(myProject, file)
        val output = compiler.compile()

        console.print(output, ConsoleViewContentType.NORMAL_OUTPUT)

        // done!
        processHandler.detachProcess()

        val result = DefaultExecutionResult(console, processHandler)

        return result
    }

    @Throws(ExecutionException::class)
    private fun createProcessHandler(myExecutionName: String): ProcessHandler {
        return object : BuildProcessHandler() {
            override fun detachIsDefault(): Boolean = false

            override fun destroyProcessImpl() {
            }

            override fun detachProcessImpl() {
                notifyProcessTerminated(0);
            }

            override fun getProcessInput(): OutputStream? = null
            override fun getExecutionName(): String = myExecutionName

            protected fun closeInput() {

            }
        }
    }
}
