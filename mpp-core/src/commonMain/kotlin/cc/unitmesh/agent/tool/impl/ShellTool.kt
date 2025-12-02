package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.boolean
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.objectType
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.tool.shell.LiveShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellSessionManager
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
     * Timeout in milliseconds (default: 60 seconds)
     * If wait=true and command doesn't complete within timeout, returns "still running" message
     */
    val timeoutMs: Long = 60000L,

    /**
     * Whether to wait for the command to complete (default: true)
     * - wait=true: Wait for completion up to timeoutMs, then return result or "still running"
     * - wait=false: Start command in background and return sessionId immediately
     */
    val wait: Boolean = true,

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
    description = """Execute shell commands with live output streaming.

If wait=true (default): Waits for command to complete up to timeoutMs.
  - If completed: Returns stdout, stderr, exit code
  - If timeout: Returns "still running" message with sessionId for later interaction

If wait=false: Starts command in background and returns sessionId immediately.
  Use read-process, wait-process, or kill-process to interact with the session.""",
    properties = mapOf(
        "command" to string(
            description = "The shell command to execute",
            required = true
        ),
        "workingDirectory" to string(
            description = "Working directory for command execution (optional)",
            required = false
        ),
        "wait" to boolean(
            description = "Whether to wait for command completion. true=wait up to timeout, false=run in background",
            required = false,
            default = true
        ),
        "timeoutMs" to integer(
            description = "Timeout in milliseconds when wait=true. After timeout, returns 'still running' with sessionId",
            required = false,
            default = 60000,
            minimum = 1000,
            maximum = 600000
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
        return """Examples:
  /$toolName command="ls -la" (wait for completion)
  /$toolName command="npm run dev" wait=false (run in background, returns sessionId)
  /$toolName command="./gradlew build" timeoutMs=120000 (wait up to 2 minutes)"""
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

            // Check if we should use live execution (async mode)
            val liveExecutor = shellExecutor as? LiveShellExecutor

            if (!params.wait && liveExecutor != null && liveExecutor.supportsLiveExecution()) {
                // Background mode: start and return immediately with sessionId
                return@safeExecute executeBackground(liveExecutor, config)
            }

            if (liveExecutor != null && liveExecutor.supportsLiveExecution()) {
                // Wait mode with live execution: start, wait with timeout
                return@safeExecute executeWithTimeout(liveExecutor, config)
            }

            // Fallback: synchronous execution
            executeSynchronous(config)
        }
    }

    /**
     * Execute in background mode - start and return sessionId immediately
     */
    private suspend fun executeBackground(
        liveExecutor: LiveShellExecutor,
        config: ShellExecutionConfig
    ): ToolResult {
        val session = liveExecutor.startLiveExecution(params.command, config)

        // Register session for later interaction
        ShellSessionManager.registerSession(
            sessionId = session.sessionId,
            command = params.command,
            workingDirectory = config.workingDirectory,
            processHandle = session.ptyHandle
        )

        val metadata = mapOf(
            "command" to params.command,
            "session_id" to session.sessionId,
            "working_directory" to (config.workingDirectory ?: ""),
            "mode" to "background"
        )

        return ToolResult.Pending(
            sessionId = session.sessionId,
            toolName = "shell",
            command = params.command,
            message = "Process started in background. Use read-process, wait-process, or kill-process with sessionId: ${session.sessionId}",
            metadata = metadata
        )
    }

    /**
     * Execute with timeout - wait for completion or return "still running"
     */
    private suspend fun executeWithTimeout(
        liveExecutor: LiveShellExecutor,
        config: ShellExecutionConfig
    ): ToolResult {
        val session = liveExecutor.startLiveExecution(params.command, config)

        // Register session
        val managedSession = ShellSessionManager.registerSession(
            sessionId = session.sessionId,
            command = params.command,
            workingDirectory = config.workingDirectory,
            processHandle = session.ptyHandle
        )

        return try {
            val exitCode = liveExecutor.waitForSession(session, config.timeoutMs)

            // Process completed - get output and clean up
            val output = managedSession.getOutput()
            managedSession.markCompleted(exitCode)
            ShellSessionManager.removeSession(session.sessionId)

            val metadata = mapOf(
                "command" to params.command,
                "exit_code" to exitCode.toString(),
                "working_directory" to (config.workingDirectory ?: ""),
                "session_id" to session.sessionId,
                "mode" to "completed"
            )

            if (exitCode == 0) {
                ToolResult.Success(output.ifEmpty { "(no output)" }, metadata)
            } else {
                ToolResult.Error(
                    message = "Command failed with exit code $exitCode:\n$output",
                    errorType = ToolErrorType.COMMAND_FAILED.code,
                    metadata = metadata
                )
            }
        } catch (e: ToolException) {
            if (e.errorType == ToolErrorType.TIMEOUT) {
                // Timeout - process still running
                val output = managedSession.getOutput()
                val metadata = mapOf(
                    "command" to params.command,
                    "session_id" to session.sessionId,
                    "working_directory" to (config.workingDirectory ?: ""),
                    "mode" to "timeout",
                    "partial_output" to output.take(1000)
                )

                ToolResult.Pending(
                    sessionId = session.sessionId,
                    toolName = "shell",
                    command = params.command,
                    message = """Process still running after ${config.timeoutMs}ms timeout.
SessionId: ${session.sessionId}

Use these tools to interact:
- read-process sessionId="${session.sessionId}" - Read current output
- wait-process sessionId="${session.sessionId}" timeoutMs=60000 - Wait for completion
- kill-process sessionId="${session.sessionId}" - Terminate the process

Partial output:
${output.take(500)}${if (output.length > 500) "\n...(truncated)" else ""}""",
                    metadata = metadata
                )
            } else {
                throw e
            }
        }
    }

    /**
     * Synchronous execution (fallback)
     */
    private suspend fun executeSynchronous(config: ShellExecutionConfig): ToolResult {
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

        return if (result.isSuccess()) {
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
