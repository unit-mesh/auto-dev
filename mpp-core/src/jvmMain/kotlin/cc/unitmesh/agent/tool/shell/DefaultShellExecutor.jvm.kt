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
 * JVM implementation of shell executor using ProcessBuilder or Pty4J
 */
actual class DefaultShellExecutor : ShellExecutor {
    
    // Lazy initialization of Pty4J executor
    private val ptyExecutor: PtyShellExecutor? by lazy {
        try {
            val executor = PtyShellExecutor()
            if (executor.isAvailable() && !isHeadless()) {
                executor
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    actual override suspend fun execute(
        command: String,
        config: ShellExecutionConfig
    ): ShellResult {
        // Use Pty4J for better terminal output if available on desktop
        val usePty = ptyExecutor != null && !config.inheritIO
        
        return if (usePty) {
            ptyExecutor!!.execute(command, config)
        } else {
            executeWithProcessBuilder(command, config)
        }
    }
    
    private suspend fun executeWithProcessBuilder(
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
                
                // Augment PATH to include common tool installation directories
                // This is critical for production builds where the app doesn't inherit
                // the user's full shell environment (e.g., Homebrew paths)
                augmentEnvironmentPath(environment(), config.environment)
                
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
    
    /**
     * Check if running in headless mode (e.g., server, CI/CD)
     */
    private fun isHeadless(): Boolean {
        return try {
            System.getProperty("java.awt.headless")?.lowercase() == "true"
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun executeProcess(
        processBuilder: ProcessBuilder,
        config: ShellExecutionConfig
    ): ShellResult = withContext(Dispatchers.IO) {
        val process = processBuilder.start()
        var timedOut = false
        
        try {
            // Read output streams in separate threads to avoid blocking
            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()
            
            val stdoutReader = if (!config.inheritIO) {
                Thread {
                    try {
                        process.inputStream.bufferedReader().use { reader ->
                            reader.forEachLine { line ->
                                stdoutBuilder.appendLine(line)
                            }
                        }
                    } catch (e: Exception) {
                        // Stream closed, ignore
                    }
                }.apply { start() }
            } else null
            
            val stderrReader = if (!config.inheritIO) {
                Thread {
                    try {
                        process.errorStream.bufferedReader().use { reader ->
                            reader.forEachLine { line ->
                                stderrBuilder.appendLine(line)
                            }
                        }
                    } catch (e: Exception) {
                        // Stream closed, ignore
                    }
                }.apply { start() }
            } else null
            
            // Wait for process to complete with timeout
            val completed = process.waitFor(config.timeoutMs, TimeUnit.MILLISECONDS)
            
            if (!completed) {
                timedOut = true
                // Kill the process
                process.destroyForcibly()
                // Give it a moment to clean up
                process.waitFor(1000, TimeUnit.MILLISECONDS)
            }
            
            // Wait for readers to finish (with timeout)
            stdoutReader?.join(1000)
            stderrReader?.join(1000)
            
            val exitCode = if (completed) process.exitValue() else -1
            
            if (timedOut) {
                throw ToolException("Process timed out", ToolErrorType.TIMEOUT)
            }
            
            ShellResult(
                exitCode = exitCode,
                stdout = stdoutBuilder.toString().trim(),
                stderr = stderrBuilder.toString().trim(),
                command = "",
                workingDirectory = null,
                executionTimeMs = 0
            )
            
        } finally {
            // Ensure process is cleaned up
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(500, TimeUnit.MILLISECONDS)
            }
        }
    }
    
    /**
     * Augment the PATH environment variable to include common tool installation directories.
     * 
     * This is essential for production builds where the app runs as a standalone bundle
     * and doesn't inherit the user's full shell environment. Common package managers like
     * Homebrew install tools in non-standard paths that need to be explicitly added.
     * 
     * @param processEnv The process environment map (will be modified)
     * @param configEnv The user-provided environment from config (should not override if PATH is set)
     */
    private fun augmentEnvironmentPath(
        processEnv: MutableMap<String, String>,
        configEnv: Map<String, String>
    ) {
        // Don't augment if user explicitly set PATH in config
        if (configEnv.containsKey("PATH")) {
            return
        }
        
        val os = System.getProperty("os.name").lowercase()
        val currentPath = processEnv["PATH"] ?: System.getenv("PATH") ?: ""
        
        // Determine additional paths based on OS
        val additionalPaths = when {
            os.contains("mac") || os.contains("darwin") -> listOf(
                "/opt/homebrew/bin",        // Homebrew (Apple Silicon)
                "/opt/homebrew/sbin",
                "/usr/local/bin",           // Homebrew (Intel)
                "/usr/local/sbin"
            )
            os.contains("linux") -> listOf(
                "/usr/local/bin",
                "/home/linuxbrew/.linuxbrew/bin",  // Linuxbrew
                "/home/linuxbrew/.linuxbrew/sbin"
            )
            else -> emptyList()  // Windows - use default PATH
        }
        
        if (additionalPaths.isEmpty()) {
            return
        }
        
        // Filter to only include paths that actually exist
        val existingAdditionalPaths = additionalPaths.filter { path ->
            File(path).exists()
        }
        
        if (existingAdditionalPaths.isEmpty()) {
            return
        }
        
        // Build the augmented PATH by prepending additional paths to current PATH
        // This ensures tools in these directories are found first
        val pathSeparator = if (os.contains("windows")) ";" else ":"
        val augmentedPath = (existingAdditionalPaths + currentPath.split(pathSeparator))
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(pathSeparator)
        
        processEnv["PATH"] = augmentedPath
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
