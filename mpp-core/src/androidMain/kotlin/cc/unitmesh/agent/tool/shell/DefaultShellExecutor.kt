package cc.unitmesh.agent.tool.shell

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * JVM implementation of shell executor using ProcessBuilder
 */
actual class DefaultShellExecutor : ShellExecutor {

    actual override suspend fun execute(
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
            
            // Create process builder
            val processBuilder = ProcessBuilder(processCommand).apply {
                // Set working directory
                config.workingDirectory?.let { workDir ->
                    directory(File(workDir))
                }
                
                // Set environment variables
                if (config.environment.isNotEmpty()) {
                    environment().putAll(config.environment)
                }
                
                // Redirect error stream if not inheriting IO
                if (!config.inheritIO) {
                    redirectErrorStream(false)
                }
            }
            
            // Execute with timeout
            val result = withTimeoutOrNull(config.timeoutMs) {
                executeProcess(processBuilder, config)
            }
            
            if (result == null) {
                throw ToolException("Command timed out after ${config.timeoutMs}ms", ToolErrorType.TIMEOUT)
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            result.copy(
                command = command,
                workingDirectory = config.workingDirectory,
                executionTimeMs = executionTime
            )
            
        } catch (e: ToolException) {
            throw e
        } catch (e: IOException) {
            throw ToolException("Failed to execute command: ${e.message}", ToolErrorType.COMMAND_FAILED, e)
        } catch (e: Exception) {
            throw ToolException("Unexpected error: ${e.message}", ToolErrorType.INTERNAL_ERROR, e)
        }
    }
    
    private suspend fun executeProcess(
        processBuilder: ProcessBuilder,
        config: ShellExecutionConfig
    ): ShellResult = withContext(Dispatchers.IO) {
        val process = processBuilder.start()
        
        try {
            // Read output streams
            val stdout = if (config.inheritIO) {
                ""
            } else {
                process.inputStream.bufferedReader().use { it.readText() }
            }
            
            val stderr = if (config.inheritIO) {
                ""
            } else {
                process.errorStream.bufferedReader().use { it.readText() }
            }
            
            // Wait for process to complete
            val exitCode = if (process.waitFor(config.timeoutMs, TimeUnit.MILLISECONDS)) {
                process.exitValue()
            } else {
                process.destroyForcibly()
                throw ToolException("Process timed out", ToolErrorType.TIMEOUT)
            }
            
            ShellResult(
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                command = "",
                workingDirectory = null,
                executionTimeMs = 0
            )
            
        } finally {
            // Ensure process is cleaned up
            if (process.isAlive) {
                process.destroyForcibly()
            }
        }
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
    
    actual override fun isAvailable(): Boolean {
        return try {
            // Test if we can create a simple process
            val testProcess = ProcessBuilder("echo", "test").start()
            testProcess.waitFor(1000, TimeUnit.MILLISECONDS)
            testProcess.destroyForcibly()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    actual override fun getDefaultShell(): String? {
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
        // Enhanced validation for JVM platform
        if (!super.validateCommand(command)) {
            return false
        }
        
        // Additional JVM-specific dangerous commands
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
