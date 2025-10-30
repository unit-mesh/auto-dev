package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellUtils
import kotlinx.serialization.Serializable

/**
 * Parameters for the Shell tool
 */
@Serializable
data class ShellParams(
    /**
     * The shell command to execute
     */
    val command: String,
    
    /**
     * Working directory for command execution (optional)
     */
    val workingDirectory: String? = null,
    
    /**
     * Environment variables to set (optional)
     */
    val environment: Map<String, String> = emptyMap(),
    
    /**
     * Timeout in milliseconds (default: 30 seconds)
     */
    val timeoutMs: Long = 30000L,
    
    /**
     * Description of what the command does (for logging/confirmation)
     */
    val description: String? = null,
    
    /**
     * Specific shell to use (optional, uses system default if not specified)
     */
    val shell: String? = null
)

/**
 * Tool invocation for shell command execution
 */
class ShellInvocation(
    params: ShellParams,
    tool: ShellTool,
    private val shellExecutor: ShellExecutor
) : BaseToolInvocation<ShellParams, ToolResult>(params, tool) {
    
    override fun getDescription(): String {
        val desc = params.description?.let { " ($it)" } ?: ""
        val workDir = params.workingDirectory?.let { " in $it" } ?: ""
        return "Execute shell command: ${params.command}$desc$workDir"
    }
    
    override fun getToolLocations(): List<ToolLocation> {
        val locations = mutableListOf<ToolLocation>()
        
        // Add working directory if specified
        params.workingDirectory?.let { workDir ->
            locations.add(ToolLocation(workDir, LocationType.DIRECTORY))
        }
        
        return locations
    }
    
    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return ToolErrorUtils.safeExecute(ToolErrorType.COMMAND_FAILED) {
            // Check if shell executor is available
            if (!shellExecutor.isAvailable()) {
                throw ToolException(
                    "Shell execution is not available on this platform",
                    ToolErrorType.NOT_SUPPORTED
                )
            }

            // Validate command
            if (!shellExecutor.validateCommand(params.command)) {
                throw ToolException(
                    "Command not allowed for security reasons: ${params.command}",
                    ToolErrorType.PERMISSION_DENIED
                )
            }
            
            // Prepare execution config
            val config = ShellExecutionConfig(
                workingDirectory = params.workingDirectory ?: context.workingDirectory,
                environment = params.environment + context.environment,
                timeoutMs = params.timeoutMs.coerceAtMost(context.timeout),
                shell = params.shell
            )
            
            // Execute command
            val result = shellExecutor.execute(params.command, config)
            
            // Format result
            val output = ShellUtils.formatShellResult(result)
            
            val metadata = mapOf(
                "command" to params.command,
                "exit_code" to result.exitCode.toString(),
                "execution_time_ms" to result.executionTimeMs.toString(),
                "working_directory" to (result.workingDirectory ?: ""),
                "shell" to (shellExecutor.getDefaultShell() ?: "unknown"),
                "stdout_length" to result.stdout.length.toString(),
                "stderr_length" to result.stderr.length.toString(),
                "success" to result.isSuccess().toString()
            )
            
            if (result.isSuccess()) {
                ToolResult.Success(output, metadata)
            } else {
                ToolResult.Error(
                    message = "Command failed with exit code ${result.exitCode}: ${result.stderr.ifEmpty { result.stdout }}",
                    errorType = ToolErrorType.COMMAND_FAILED.code
                )
            }
        }
    }
}

/**
 * Tool for executing shell commands
 */
class ShellTool(
    private val shellExecutor: ShellExecutor = DefaultShellExecutor()
) : BaseExecutableTool<ShellParams, ToolResult>() {
    
    override val name: String = ToolNames.SHELL
    override val description: String = """
        Execute shell commands in the project environment with security controls.
        Use for system operations, build scripts, environment setup, or external tool execution.
        Commands run in specified working directory context with custom environment variables.
        Returns stdout, stderr, exit codes, and execution metadata. Handle with security considerations.
    """.trimIndent()
    
    override fun getParameterClass(): String = ShellParams::class.simpleName ?: "ShellParams"
    
    override fun createToolInvocation(params: ShellParams): ToolInvocation<ShellParams, ToolResult> {
        validateParameters(params)
        return ShellInvocation(params, this, shellExecutor)
    }
    
    private fun validateParameters(params: ShellParams) {
        if (params.command.isBlank()) {
            throw ToolException("Shell command cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }

        if (params.timeoutMs <= 0) {
            throw ToolException("Timeout must be positive", ToolErrorType.PARAMETER_OUT_OF_RANGE)
        }

        // Check for path traversal in working directory
        params.workingDirectory?.let { workDir ->
            if (workDir.contains("..")) {
                throw ToolException("Path traversal not allowed in working directory", ToolErrorType.PATH_INVALID)
            }
        }

        // Validate environment variable names
        params.environment.keys.forEach { key ->
            if (key.isBlank() || key.contains('=')) {
                throw ToolException("Invalid environment variable name: $key", ToolErrorType.INVALID_PARAMETERS)
            }
        }

        // Additional command validation
        if (!shellExecutor.validateCommand(params.command)) {
            throw ToolException("Command not allowed: ${params.command}", ToolErrorType.PERMISSION_DENIED)
        }
    }
    
    /**
     * Get common shell commands for the current platform
     */
    fun getCommonCommands(): Map<String, String> {
        return ShellUtils.getCommonCommands()
    }
    
    /**
     * Check if shell execution is available
     */
    fun isAvailable(): Boolean {
        return shellExecutor.isAvailable()
    }
    
    /**
     * Get the default shell for the current platform
     */
    fun getDefaultShell(): String? {
        return shellExecutor.getDefaultShell()
    }
}
