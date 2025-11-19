package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.objectType
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
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

object ShellSchema : DeclarativeToolSchema(
    description = "Execute shell commands with various options",
    properties = mapOf(
        "command" to string(
            description = "The shell command to execute",
            required = true
        ),
        "workingDirectory" to string(
            description = "Working directory for command execution (optional)",
            required = false
        ),
        "environment" to objectType(
            description = "Environment variables to set (optional)",
            properties = mapOf(
                "additionalProperties" to string(
                    description = "Environment variable value"
                )
            ),
            required = false,
            additionalProperties = true
        ),
        "timeoutMs" to integer(
            description = "Timeout in milliseconds",
            required = false,
            default = 30000,
            minimum = 1000,
            maximum = 300000
        ),
        "description" to string(
            description = "Description of what the command does (for logging/confirmation)",
            required = false
        ),
        "shell" to string(
            description = "Specific shell to use (optional, uses system default if not specified)",
            required = false,
            enum = listOf("bash", "zsh", "sh", "cmd", "powershell")
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName command=\"ls -la\" workingDirectory=\"/tmp\" timeoutMs=10000"
    }
}


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
            if (!shellExecutor.isAvailable()) {
                throw ToolException(
                    "Shell execution is not available on this platform",
                    ToolErrorType.NOT_SUPPORTED
                )
            }

            if (!shellExecutor.validateCommand(params.command)) {
                throw ToolException(
                    "Command not allowed for security reasons: ${params.command}",
                    ToolErrorType.PERMISSION_DENIED
                )
            }

            val config = ShellExecutionConfig(
                workingDirectory = params.workingDirectory ?: context.workingDirectory,
                environment = params.environment + context.environment,
                timeoutMs = params.timeoutMs.coerceAtMost(context.timeout),
                shell = params.shell
            )

            val result = shellExecutor.execute(params.command, config)
            val output = ShellUtils.formatShellResult(result)

            val metadata = mapOf(
                "command" to params.command,
                "exit_code" to result.exitCode.toString(),
                "execution_time_ms" to result.executionTimeMs.toString(),
                "working_directory" to (result.workingDirectory ?: ""),
                "shell" to (shellExecutor.getDefaultShell() ?: "unknown"),
                "stdout_length" to result.stdout.length.toString(),
                "stderr_length" to result.stderr.length.toString(),
                "success" to result.isSuccess().toString(),
                "stdout" to result.stdout,
                "stderr" to result.stderr
            )

            if (result.isSuccess()) {
                ToolResult.Success(output, metadata)
            } else {
                ToolResult.Error(
                    message = "Command failed with exit code ${result.exitCode}: ${result.stderr.ifEmpty { result.stdout }}",
                    errorType = ToolErrorType.COMMAND_FAILED.code,
                    metadata = metadata
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

    override val name: String = "shell"
    override val description: String = """
  The following information is returned:

  Command: Executed command.
  Directory: Directory where command was executed, or \`(root)\`.
  Stdout: Output on stdout stream. Can be \`(empty)\` or partial on error and for any unwaited background processes.
  Stderr: Output on stderr stream. Can be \`(empty)\` or partial on error and for any unwaited background processes.
  Error: Error or \`(none)\` if no error was reported for the subprocess.
  Exit Code: Exit code or \`(none)\` if terminated by signal.
  Signal: Signal number or \`(none)\` if no signal was received.
  Background PIDs: List of background processes started or \`(none)\`.
  Process Group PGID: Process group started or \`(none)\``
    """.trimIndent()

    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Shell Command",
        tuiEmoji = "💻",
        composeIcon = "terminal",
        category = ToolCategory.Execution,
        schema = ShellSchema
    )

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

        params.workingDirectory?.let { workDir ->
            if (workDir.contains("..")) {
                throw ToolException("Path traversal not allowed in working directory", ToolErrorType.PATH_INVALID)
            }
        }

        params.environment.keys.forEach { key ->
            if (key.isBlank() || key.contains('=')) {
                throw ToolException("Invalid environment variable name: $key", ToolErrorType.INVALID_PARAMETERS)
            }
        }

        if (!shellExecutor.validateCommand(params.command)) {
            throw ToolException("Command not allowed: ${params.command}", ToolErrorType.PERMISSION_DENIED)
        }
    }

    fun isAvailable(): Boolean = shellExecutor.isAvailable()

    fun getDefaultShell(): String? = shellExecutor.getDefaultShell()
    
    /**
     * Get the underlying shell executor
     * Used by ToolOrchestrator to check for PTY support
     */
    fun getExecutor(): ShellExecutor = shellExecutor
}
