package cc.unitmesh.agent.tool.shell

import cc.unitmesh.agent.logging.logger
import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * JVM implementation of shell executor using JediTerm for better terminal output handling.
 * Provides pseudo-terminal (PTY) capabilities for more accurate terminal emulation.
 *
 * Note: This still uses pty4j for process creation but integrates with JediTerm for terminal handling.
 */
class PtyShellExecutor : ShellExecutor {

    override suspend fun execute(
        command: String,
        config: ShellExecutionConfig
    ): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (!validateCommand(command)) {
            throw ToolException("Command not allowed: $command", ToolErrorType.PERMISSION_DENIED)
        }

        val processCommand = prepareCommand(command, config.shell)

        val environment = HashMap<String, String>(System.getenv())
        environment.putAll(config.environment)
        // Ensure TERM is set for proper terminal behavior
        if (!environment.containsKey("TERM")) {
            environment["TERM"] = "xterm-256color"
        }

        val ptyProcessBuilder = PtyProcessBuilder()
            .setCommand(processCommand.toTypedArray())
            .setEnvironment(environment)
            .setConsole(false)
            .setCygwin(false)

        config.workingDirectory?.let { workDir ->
            ptyProcessBuilder.setDirectory(workDir)
        }

        val ptyProcess = ptyProcessBuilder.start()

        val result = withTimeoutOrNull(config.timeoutMs) {
            executeWithPty(ptyProcess, config)
        }

        if (result == null) {
            // Timeout occurred - terminate process
            ptyProcess.destroyForcibly()
            ptyProcess.waitFor(3000000, TimeUnit.MILLISECONDS)
            throw ToolException("Command timed out after ${config.timeoutMs}ms", ToolErrorType.TIMEOUT)
        }

        val executionTime = System.currentTimeMillis() - startTime

        result.copy(
            command = command,
            workingDirectory = config.workingDirectory,
            executionTimeMs = executionTime
        )
    }

    private suspend fun executeWithPty(
        ptyProcess: Process,
        config: ShellExecutionConfig
    ): ShellResult = withContext(Dispatchers.IO) {
        val outputBuilder = StringBuilder()

        // Use coroutine to read output
        val outputJob = if (!config.inheritIO) {
            launch {
                try {
                    ptyProcess.inputStream.bufferedReader().use { reader ->
                        var line = reader.readLine()
                        while (line != null && isActive) {
                            outputBuilder.appendLine(line)
                            line = reader.readLine()
                        }
                    }
                } catch (e: Exception) {
                    logger().error(e) { "Failed to read output from PTY process: ${e.message}" }
                }
            }
        } else null

        val completed = withTimeoutOrNull(config.timeoutMs) {
            while (ptyProcess.isAlive && isActive) {
                delay(100)
            }
            true
        }

        if (completed == null) {
            outputJob?.cancel()
            ptyProcess.destroyForcibly()
            ptyProcess.waitFor(3000000, TimeUnit.MILLISECONDS)
            throw ToolException("Process timed out", ToolErrorType.TIMEOUT)
        }

        // Wait for output reader to finish
        outputJob?.join()

        val exitCode = ptyProcess.exitValue()

        ShellResult(
            exitCode = exitCode,
            stdout = outputBuilder.toString().trim(),
            stderr = "", // PTY combines stdout and stderr
            command = "",
            workingDirectory = null,
            executionTimeMs = 0
        )
    }

    private fun prepareCommand(command: String, shell: String?): List<String> {
        val effectiveShell = shell ?: getDefaultShell()

        return if (effectiveShell != null) {
            // Use shell to execute command
            when {
                effectiveShell.endsWith("cmd.exe") || effectiveShell.endsWith("cmd") -> {
                    listOf(effectiveShell, "/c", command)
                }

                effectiveShell.endsWith("powershell.exe") || effectiveShell.endsWith("powershell") -> {
                    listOf(effectiveShell, "-Command", command)
                }

                else -> {
                    // Unix-like shell
                    listOf(effectiveShell, "-c", command)
                }
            }
        } else {
            // Try to execute command directly
            ShellUtils.parseCommand(command).let { (cmd, args) ->
                listOf(cmd) + args
            }
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            // Check if PTY is available on this platform
            val testEnv = HashMap<String, String>(System.getenv())
            testEnv["TERM"] = "xterm"

            val testProcess = PtyProcessBuilder()
                .setCommand(arrayOf("echo", "test"))
                .setEnvironment(testEnv)
                .setConsole(false)
                .start()

            testProcess.waitFor(1000, TimeUnit.MILLISECONDS)
            testProcess.destroyForcibly()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getDefaultShell(): String? {
        val os = System.getProperty("os.name").lowercase()

        return when {
            os.contains("windows") -> {
                // Try PowerShell first, then cmd
                listOf("powershell.exe", "cmd.exe").firstOrNull { shellExists(it) }
            }

            os.contains("mac") || os.contains("darwin") -> {
                // Try zsh first (default on macOS), then bash
                listOf("/bin/zsh", "/bin/bash", "/bin/sh").firstOrNull { shellExists(it) }
            }

            else -> {
                // Linux and other Unix-like systems
                listOf("/bin/bash", "/bin/sh", "/bin/zsh").firstOrNull { shellExists(it) }
            }
        }
    }

    private fun shellExists(shellPath: String): Boolean {
        return try {
            val file = File(shellPath)
            file.exists() && file.canExecute()
        } catch (e: Exception) {
            false
        }
    }

    override fun validateCommand(command: String): Boolean {
        if (!ShellExecutor.validateCommand(command)) {
            return false
        }

        // Additional dangerous commands
        val jvmDangerousCommands = setOf(
            "shutdown", "reboot", "halt", "poweroff",
            "mkfs", "fdisk", "parted", "gparted",
            "iptables", "ufw", "firewall-cmd"
        )

        val commandLower = command.lowercase()
        return jvmDangerousCommands.none { dangerous ->
            commandLower.contains(dangerous)
        }
    }
}

