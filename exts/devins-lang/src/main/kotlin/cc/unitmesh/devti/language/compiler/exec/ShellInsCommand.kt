package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.sketch.run.ShellUtil
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionMode
import com.intellij.execution.ExecutionModes.SameThreadMode
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.*
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sh.ShLanguage
import com.intellij.util.concurrency.Semaphore
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

/**
 * A class that implements the `InsCommand` interface to execute a shell command within the IntelliJ IDEA environment.
 *
 * This class is designed to run a shell command specified by a given `prop` string, which is assumed to be the path to a file within the project.
 * The command is executed in a shell runner service provided by IntelliJ IDEA, using the specified file's path and its parent directory as the working directory.
 *
 * @param myProject The current project context.
 * @param shellFile The path to the file within the project whose content should be executed as a shell command.
 */
class ShellInsCommand(val myProject: Project, private val shellFile: String?, val shellContent: String?) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.SHELL

    override suspend fun execute(): String? {
        val virtualFile = if (shellContent != null) {
            ScratchRootType.getInstance()
                .createScratchFile(myProject, "devin-shell-ins.sh", ShLanguage.INSTANCE, shellContent)
                ?: return "$DEVINS_ERROR: Failed to create scratch file for ShellInsCommand"
        } else if (shellFile != null) {
            myProject.lookupFile(shellFile.trim()) ?: return "$DEVINS_ERROR: File not found: $shellFile"
        } else {
            null
        } ?: return "$DEVINS_ERROR: File not found"

        return ApplicationManager.getApplication().executeOnPooledThread<String?> {
            var output = ""
            output = doExecute(virtualFile)
            output
        }.get()
    }

    /**
     * Executes a shell script specified by the given virtual file and returns the output as a string.
     * This method ensures that the script file is executable before running it. It creates a command line
     * for the script, attaches a process handler to manage the execution, and captures the output of the
     * script. The execution is time-limited to prevent indefinite hanging.
     *
     * @param virtualFile The virtual file representing the shell script to be executed. The file must exist
     *                    and be accessible.
     * @return The output of the executed shell script as a string. This includes any text produced by the
     *         script during its execution.
     *
     * @throws ExecutionException If an error occurs during the execution of the shell script, such as
     *                            permission issues or process creation failures.
     */
    private fun doExecute(virtualFile: VirtualFile): String {
        // 设置文件可执行
        File(virtualFile.path).setExecutable(true)

        val command = virtualFile.path
        val commandLine = createCommandLineForScript(myProject, command)

        val processBuilder = commandLine.toProcessBuilder()
        processBuilder.directory(commandLine.workDirectory)
        processBuilder.environment().putAll(commandLine.environment)

        // 重定向输出到标准输出
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)
        processBuilder.redirectError(ProcessBuilder.Redirect.PIPE)

        val process = processBuilder.start()

        val output = StringBuilder()
        val errorOutput = StringBuilder()

        val outputReader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))

        val outputThread = Thread {
            outputReader.lines().forEach { line ->
                output.append(line).append("\n")
            }
        }

        val errorThread = Thread {
            errorReader.lines().forEach { line ->
                errorOutput.append(line).append("\n")
            }
        }

        outputThread.start()
        errorThread.start()

        val mode = SameThreadMode(true, "ShellInsCommand", 30)
        val finished = process.waitFor(mode.timeout.toLong(), TimeUnit.SECONDS)

        if (!finished) {
            process.destroy()
            throw ExecutionException("Process timed out")
        }

        outputThread.join()
        errorThread.join()

        return output.toString()
    }

    private fun createTimeLimitedExecutionProcess(
        processHandler: ProcessHandler,
        mode: ExecutionMode,
        presentableCmdline: String
    ): Runnable {
        val outputCollected = ProcessOutput()
        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val eventText = event.text
                if (StringUtil.isNotEmpty(eventText)) {
                    if (ProcessOutputType.isStdout(outputType)) {
                        outputCollected.appendStdout(eventText)
                    } else if (ProcessOutputType.isStderr(outputType)) {
                        outputCollected.appendStderr(eventText)
                    }
                }
            }
        })

        val invocatorStack = Throwable()
        return object : Runnable {
            private val mySemaphore = Semaphore()

            private val myProcessRunnable = Runnable {
                try {
                    val finished = processHandler.waitFor(1000L * mode.timeout)
                    if (!finished) {
                        mode.onTimeout(processHandler, presentableCmdline, outputCollected, invocatorStack)
                        processHandler.destroyProcess()
                    }
                } finally {
                    mySemaphore.up()
                }
            }

            override fun run() {
                mySemaphore.down()
                ApplicationManager.getApplication().executeOnPooledThread(myProcessRunnable)
                OSProcessHandler.checkEdtAndReadAction(processHandler)
                mySemaphore.waitFor()
            }
        }
    }

    fun createCommandLineForScript(project: Project, scriptText: String): GeneralCommandLine {
        val workingDirectory = project.basePath
        val commandLine = PtyCommandLine()
        commandLine.withConsoleMode(false)
        commandLine.withInitialColumns(120)
        commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        commandLine.setWorkDirectory(workingDirectory!!)
        commandLine.withExePath(ShellUtil.detectShells().first())
        commandLine.withParameters("-c")
        commandLine.withParameters(scriptText)
        return commandLine
    }
}