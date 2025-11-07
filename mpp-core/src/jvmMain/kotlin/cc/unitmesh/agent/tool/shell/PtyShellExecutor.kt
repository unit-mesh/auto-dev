package cc.unitmesh.agent.tool.shell

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * JVM implementation of shell executor using Pty4J for better terminal output handling.
 * Provides pseudo-terminal (PTY) capabilities for more accurate terminal emulation.
 */
class PtyShellExecutor : ShellExecutor {

    override suspend fun execute(
        command: String,
        config: ShellExecutionConfig
    ): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            // Validate command
            if (!validateCommand(command)) {
                throw ToolException("Command not allowed: $command", ToolErrorType.PERMISSION_DENIED)
            }

            // Prepare command for execution
            val processCommand = prepareCommand(command, config.shell)

            // Set up environment
            val environment = HashMap<String, String>(System.getenv())
            environment.putAll(config.environment)
            // Ensure TERM is set for proper terminal behavior
            if (!environment.containsKey("TERM")) {
                environment["TERM"] = "xterm-256color"
            }

            // Create PTY process
            val ptyProcessBuilder = PtyProcessBuilder()
                .setCommand(processCommand.toTypedArray())
                .setEnvironment(environment)
                .setConsole(false)
                .setCygwin(false)

            // Set working directory
            config.workingDirectory?.let { workDir ->
                ptyProcessBuilder.setDirectory(workDir)
            }

            val ptyProcess = ptyProcessBuilder.start()

            try {
                val result = withTimeoutOrNull(config.timeoutMs) {
                    executeWithPty(ptyProcess, config)
                }

                if (result == null) {
                    // Timeout occurred - terminate process
                    ptyProcess.destroyForcibly()
                    ptyProcess.waitFor(1000, TimeUnit.MILLISECONDS)
                    throw ToolException("Command timed out after ${config.timeoutMs}ms", ToolErrorType.TIMEOUT)
                }

                val executionTime = System.currentTimeMillis() - startTime

                result.copy(
                    command = command,
                    workingDirectory = config.workingDirectory,
                    executionTimeMs = executionTime
                )

            } finally {
                // Ensure process is cleaned up
                if (ptyProcess.isAlive) {
                    ptyProcess.destroyForcibly()
                    ptyProcess.waitFor(500, TimeUnit.MILLISECONDS)
                }
            }

        } catch (e: ToolException) {
            throw e
        } catch (e: IOException) {
            throw ToolException("Failed to execute command: ${e.message}", ToolErrorType.COMMAND_FAILED, e)
        } catch (e: Exception) {
            throw ToolException("Unexpected error: ${e.message}", ToolErrorType.INTERNAL_ERROR, e)
        }
    }

    private suspend fun executeWithPty(
        ptyProcess: Process,
        config: ShellExecutionConfig
    ): ShellResult = withContext(Dispatchers.IO) {
        // Read output in a separate thread to avoid blocking
        val outputBuilder = StringBuilder()
        var timedOut = false

        val outputReader = if (!config.inheritIO) {
            Thread {
                try {
                    ptyProcess.inputStream.bufferedReader().use { reader ->
                        reader.forEachLine { line ->
                            outputBuilder.appendLine(line)
                        }
                    }
                } catch (e: Exception) {
                    // Stream closed, ignore
                }
            }.apply { start() }
        } else null

        // Wait for process to complete with timeout
        val completed = ptyProcess.waitFor(config.timeoutMs, TimeUnit.MILLISECONDS)

        if (!completed) {
            timedOut = true
            // Kill the process
            ptyProcess.destroyForcibly()
            // Give it a moment to clean up
            ptyProcess.waitFor(1000, TimeUnit.MILLISECONDS)
        }

        // Wait for reader to finish (with timeout)
        outputReader?.join(1000)

        val exitCode = if (completed) ptyProcess.exitValue() else -1

        if (timedOut) {
            throw ToolException("Process timed out", ToolErrorType.TIMEOUT)
        }

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
        // Enhanced validation
        if (!ShellExecutor::class.java.getMethod("validateCommand", String::class.java).let { method ->
            method.invoke(this, command) as Boolean
        }) {
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

