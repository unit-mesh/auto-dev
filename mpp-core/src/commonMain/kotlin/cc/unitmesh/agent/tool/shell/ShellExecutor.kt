package cc.unitmesh.agent.tool.shell

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import kotlinx.serialization.Serializable

/**
 * Result of shell command execution
 */
@Serializable
data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val command: String,
    val workingDirectory: String? = null,
    val executionTimeMs: Long = 0
) {
    fun isSuccess(): Boolean = exitCode == 0
    
    fun getOutput(): String = if (isSuccess()) stdout else stderr
    
    fun getCombinedOutput(): String = buildString {
        if (stdout.isNotEmpty()) {
            appendLine("STDOUT:")
            appendLine(stdout)
        }
        if (stderr.isNotEmpty()) {
            if (stdout.isNotEmpty()) appendLine()
            appendLine("STDERR:")
            appendLine(stderr)
        }
    }.trim()
}

/**
 * Configuration for shell command execution
 */
data class ShellExecutionConfig(
    val workingDirectory: String? = null,
    val environment: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 30000L, // 30 seconds default
    val inheritIO: Boolean = false,
    val shell: String? = null // e.g., "/bin/bash", "cmd.exe"
)

/**
 * Cross-platform shell command executor interface
 */
interface ShellExecutor {
    /**
     * Execute a shell command synchronously
     */
    suspend fun execute(
        command: String,
        config: ShellExecutionConfig = ShellExecutionConfig()
    ): ShellResult
    
    /**
     * Check if the executor is available on the current platform
     */
    fun isAvailable(): Boolean
    
    /**
     * Get the default shell for the current platform
     */
    fun getDefaultShell(): String?
    
    /**
     * Validate if a command is safe to execute (basic security check)
     */
    fun validateCommand(command: String): Boolean {
        // Basic validation - can be overridden by implementations
        val dangerousCommands = setOf(
            "rm -rf /", "del /f /s /q C:\\", "format", "fdisk",
            "dd if=/dev/zero", ":(){ :|:& };:", "sudo rm -rf"
        )
        
        return dangerousCommands.none { dangerous ->
            command.contains(dangerous, ignoreCase = true)
        }
    }
}

/**
 * Empty shell executor for platforms that don't support shell execution
 */
class EmptyShellExecutor : ShellExecutor {
    override suspend fun execute(command: String, config: ShellExecutionConfig): ShellResult {
        throw ToolException(
            "Shell execution is not supported on this platform",
            ToolErrorType.NOT_SUPPORTED
        )
    }
    
    override fun isAvailable(): Boolean = false
    
    override fun getDefaultShell(): String? = null
}

/**
 * Expect/actual declaration for platform-specific shell executor
 */
expect class DefaultShellExecutor() : ShellExecutor {
    override suspend fun execute(command: String, config: ShellExecutionConfig): ShellResult
    override fun isAvailable(): Boolean
    override fun getDefaultShell(): String?
}

/**
 * Utility functions for shell operations
 */
object ShellUtils {
    /**
     * Escape shell arguments to prevent injection
     */
    fun escapeShellArg(arg: String): String {
        return when {
            arg.isEmpty() -> "''"
            arg.all { it.isLetterOrDigit() || it in "._-" } -> arg
            else -> "'${arg.replace("'", "'\\''")}'"
        }
    }
    
    /**
     * Split command line into command and arguments
     */
    fun parseCommand(commandLine: String): Pair<String, List<String>> {
        val parts = commandLine.trim().split(Regex("\\s+"))
        return if (parts.isEmpty()) {
            "" to emptyList()
        } else {
            parts.first() to parts.drop(1)
        }
    }
    
    /**
     * Check if a command exists in PATH
     */
    fun commandExists(command: String): Boolean {
        // This would need platform-specific implementation
        // For now, just basic validation
        return command.isNotBlank() && !command.contains("..")
    }
    
    /**
     * Get common shell commands for different platforms
     */
    fun getCommonCommands(): Map<String, String> {
        return mapOf(
            "list_files" to "ls -la",
            "current_directory" to "pwd",
            "disk_usage" to "df -h",
            "memory_usage" to "free -h",
            "process_list" to "ps aux",
            "network_info" to "ifconfig",
            "system_info" to "uname -a"
        )
    }
    
    /**
     * Format shell result for display
     */
    fun formatShellResult(result: ShellResult): String {
        return buildString {
            appendLine("Command: ${result.command}")
            if (result.workingDirectory != null) {
                appendLine("Working Directory: ${result.workingDirectory}")
            }
            appendLine("Exit Code: ${result.exitCode}")
            appendLine("Execution Time: ${result.executionTimeMs}ms")
            appendLine()
            
            if (result.stdout.isNotEmpty()) {
                appendLine("STDOUT:")
                appendLine(result.stdout)
            }
            
            if (result.stderr.isNotEmpty()) {
                if (result.stdout.isNotEmpty()) appendLine()
                appendLine("STDERR:")
                appendLine(result.stderr)
            }
        }.trim()
    }
}
