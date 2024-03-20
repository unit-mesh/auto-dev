package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.sh.run.ShConfigurationType
import com.intellij.sh.run.ShRunner
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.util.io.BaseDataReader
import com.intellij.util.io.BaseOutputReader

class ShellInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override fun execute(): String? {
        val virtualFile = myProject.lookupFile(prop.trim()) ?: return "<DevInsError>: File not found: $prop"

        val workingDirectory = virtualFile.parent.path
        val shRunner = ApplicationManager.getApplication().getService(
            ShRunner::class.java
        )
        if (shRunner != null && shRunner.isAvailable(myProject)) {
            shRunner.run(myProject, virtualFile.path, workingDirectory, "RunDevInsShell", true)
        }

//        runInTerminal(virtualFile.path, workingDirectory, myProject)

        return ""
    }

    @Throws(ExecutionException::class)
    private fun runInTerminal(command: String, workingDirectory: String, project: Project): DefaultExecutionResult {
        val commandLine = createCommandLineForScript(project, workingDirectory, command)
        val processHandler = createProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        val console: ConsoleView = TerminalExecutionConsole(project, processHandler)
        console.attachToProcess(processHandler)
        return DefaultExecutionResult(console, processHandler)
    }

    private fun createCommandLineForScript(
        project: Project,
        workingDirectory: String,
        command: String
    ): GeneralCommandLine {
        val commandLine = PtyCommandLine()
            .withConsoleMode(false)
            .withInitialColumns(120)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withExePath(ShConfigurationType.getDefaultShell(project))
            .withParameters("-c")
            .withParameters(command)

        commandLine.setWorkDirectory(workingDirectory)
        return commandLine
    }

    @Throws(ExecutionException::class)
    private fun createProcessHandler(commandLine: GeneralCommandLine) =
        object : KillableProcessHandler(commandLine) {
            override fun readerOptions() = object : BaseOutputReader.Options() {
                override fun policy(): BaseDataReader.SleepingPolicy {
                    return BaseDataReader.SleepingPolicy.BLOCKING
                }

                override fun splitToLines(): Boolean {
                    return false
                }
            }
        }
}
