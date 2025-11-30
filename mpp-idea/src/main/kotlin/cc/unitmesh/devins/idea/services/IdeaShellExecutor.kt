package cc.unitmesh.devins.idea.services

import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.text.Strings
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.*
import java.io.*

/**
 * Result from shell command execution.
 */
data class ShellExecutorResult(
    val exitCode: Int,
    val stdOutput: String,
    val errOutput: String
)

/**
 * Execution state for terminal commands.
 * Aligned with ext-terminal's TerminalExecutionState.
 */
enum class IdeaTerminalExecutionState {
    READY,
    EXECUTING,
    SUCCESS,
    FAILED,
    TERMINATED
}

/**
 * Shell command executor service for mpp-idea.
 * Based on ProcessExecutor from core module but adapted for standalone use.
 */
@Service(Service.Level.PROJECT)
class IdeaShellExecutor(private val project: Project) {

    /**
     * Execute shell command with streaming output support.
     * 
     * @param shellScript The shell command to execute
     * @param stdWriter Writer for stdout
     * @param errWriter Writer for stderr
     * @param dispatcher Coroutine dispatcher for execution
     * @return Exit code of the process
     */
    suspend fun exec(
        shellScript: String,
        stdWriter: Writer,
        errWriter: Writer,
        dispatcher: CoroutineDispatcher
    ): Int = withContext(dispatcher) {
        val process = createProcess(shellScript)
        
        val exitCode = async { process.awaitExit() }
        val errOutput = async { consumeProcessOutput(process.errorStream, errWriter, process, dispatcher) }
        val stdOutput = async { consumeProcessOutput(process.inputStream, stdWriter, process, dispatcher) }
        
        stdOutput.await()
        errOutput.await()
        exitCode.await()
    }

    /**
     * Execute shell command and return result.
     */
    suspend fun executeCommand(command: String, dispatcher: CoroutineDispatcher): ShellExecutorResult {
        val stdWriter = StringWriter()
        val errWriter = StringWriter()
        
        val exitCode = exec(command, stdWriter, errWriter, dispatcher)
        return ShellExecutorResult(
            exitCode = exitCode,
            stdOutput = stdWriter.toString(),
            errOutput = errWriter.toString()
        )
    }

    private fun createProcess(shellScript: String): Process {
        val basedir = project.basePath
        val commandLine = PtyCommandLine()
        commandLine.withConsoleMode(false)
        commandLine.withUnixOpenTtyToPreserveOutputAfterTermination(true)
        commandLine.withInitialColumns(240)
        commandLine.withInitialRows(80)
        commandLine.withEnvironment("TERM", "dumb")
        commandLine.withEnvironment("BASH_SILENCE_DEPRECATION_WARNING", "1")
        commandLine.withEnvironment("GIT_PAGER", "cat")
        
        // Set JAVA_HOME if available
        try {
            getJdkVersion()?.let { javaHomePath ->
                commandLine.withEnvironment("JAVA_HOME", javaHomePath)
            }
        } catch (e: Exception) {
            // Ignore JAVA_HOME errors
        }

        val shell = detectShell()
        val commands: List<String> = listOf(shell, "--noprofile", "--norc", "-c", formatCommand(shellScript))

        if (basedir != null) {
            commandLine.withWorkDirectory(basedir)
        }

        return commandLine.startProcessWithPty(commands)
    }

    private fun formatCommand(command: String): String {
        return "{ $command; } 2>&1"
    }

    private fun detectShell(): String {
        val shells = listOf("/bin/zsh", "/bin/bash", "/bin/sh")
        return shells.find { File(it).exists() } ?: "bash"
    }

    private fun getJdkVersion(): String? {
        return try {
            val sdk = ProjectRootManager.getInstance(project).projectSdk
            sdk?.homePath
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun consumeProcessOutput(
        source: InputStream?,
        outputWriter: Writer,
        process: Process,
        dispatcher: CoroutineDispatcher
    ) = withContext(dispatcher) {
        if (source == null) return@withContext

        var isFirstLine = true
        BufferedReader(InputStreamReader(source, Charsets.UTF_8.name())).use { reader ->
            do {
                val line = reader.readLine()
                if (Strings.isNotEmpty(line)) {
                    if (!isFirstLine) outputWriter.append(System.lineSeparator())
                    isFirstLine = false
                    outputWriter.append(line)
                } else {
                    yield()
                }
                ensureActive()
            } while (process.isAlive || line != null)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): IdeaShellExecutor = project.service()
    }
}

