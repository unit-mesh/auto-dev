package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.boolean
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.agent.tool.shell.ShellSessionManager
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

// ============================================================================
// ReadProcess Tool - Read output from a running process
// ============================================================================

@Serializable
data class ReadProcessParams(
    val sessionId: String,
    val wait: Boolean = false,
    val maxWaitSeconds: Int = 60
)

object ReadProcessSchema : DeclarativeToolSchema(
    description = "Read output from a running or completed process session",
    properties = mapOf(
        "sessionId" to string(
            description = "The session ID returned by shell command with wait=false or timeout",
            required = true
        ),
        "wait" to boolean(
            description = "If true, wait for process to complete before returning output",
            required = false,
            default = false
        ),
        "maxWaitSeconds" to integer(
            description = "Maximum seconds to wait if wait=true",
            required = false,
            default = 60,
            minimum = 1,
            maximum = 600
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName sessionId=\"abc-123\" wait=false"
    }
}

class ReadProcessInvocation(
    params: ReadProcessParams,
    tool: ReadProcessTool
) : BaseToolInvocation<ReadProcessParams, ToolResult>(params, tool) {

    override fun getDescription(): String = "Read output from session: ${params.sessionId}"
    override fun getToolLocations(): List<ToolLocation> = emptyList()

    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        val session = ShellSessionManager.getSession(params.sessionId)
            ?: return ToolResult.Error(
                message = "Session not found: ${params.sessionId}",
                errorType = ToolErrorType.INVALID_PARAMETERS.code
            )

        if (params.wait) {
            // Wait for process to complete
            val timeoutMs = params.maxWaitSeconds * 1000L
            val startTime = Platform.getCurrentTimestamp()

            while (session.isRunning() && (Platform.getCurrentTimestamp() - startTime) < timeoutMs) {
                delay(100)
            }
        }

        val output = session.getOutput()
        val isRunning = session.isRunning()

        val metadata = mapOf(
            "session_id" to params.sessionId,
            "command" to session.command,
            "is_running" to isRunning.toString(),
            "exit_code" to (session.exitCode?.toString() ?: ""),
            "execution_time_ms" to session.getExecutionTimeMs().toString()
        )

        return if (isRunning) {
            ToolResult.Pending(
                sessionId = params.sessionId,
                toolName = "read-process",
                command = session.command,
                message = "Process still running.\n\nCurrent output:\n$output",
                metadata = metadata
            )
        } else {
            val exitCode = session.exitCode ?: -1
            if (exitCode == 0) {
                ToolResult.Success(output.ifEmpty { "(no output)" }, metadata)
            } else {
                ToolResult.Error(
                    message = "Process exited with code $exitCode:\n$output",
                    errorType = ToolErrorType.COMMAND_FAILED.code,
                    metadata = metadata
                )
            }
        }
    }
}

class ReadProcessTool : BaseExecutableTool<ReadProcessParams, ToolResult>() {
    override val name: String = "read-process"
    override val description: String = "Read output from a running or completed process session"
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Read Process",
        tuiEmoji = "📖",
        composeIcon = "terminal",
        category = ToolCategory.Execution,
        schema = ReadProcessSchema
    )

    override fun getParameterClass(): String = ReadProcessParams::class.simpleName ?: "ReadProcessParams"

    override fun createToolInvocation(params: ReadProcessParams): ToolInvocation<ReadProcessParams, ToolResult> {
        if (params.sessionId.isBlank()) {
            throw ToolException("sessionId is required", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }
        return ReadProcessInvocation(params, this)
    }
}

// ============================================================================
// WaitProcess Tool - Wait for a process to complete
// ============================================================================

@Serializable
data class WaitProcessParams(
    val sessionId: String,
    val timeoutMs: Long = 60000L
)

object WaitProcessSchema : DeclarativeToolSchema(
    description = "Wait for a background process to complete",
    properties = mapOf(
        "sessionId" to string(
            description = "The session ID of the process to wait for",
            required = true
        ),
        "timeoutMs" to integer(
            description = "Maximum milliseconds to wait for completion",
            required = false,
            default = 60000,
            minimum = 1000,
            maximum = 600000
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName sessionId=\"abc-123\" timeoutMs=120000"
    }
}

class WaitProcessInvocation(
    params: WaitProcessParams,
    tool: WaitProcessTool
) : BaseToolInvocation<WaitProcessParams, ToolResult>(params, tool) {

    override fun getDescription(): String = "Wait for session: ${params.sessionId}"
    override fun getToolLocations(): List<ToolLocation> = emptyList()

    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        val session = ShellSessionManager.getSession(params.sessionId)
            ?: return ToolResult.Error(
                message = "Session not found: ${params.sessionId}",
                errorType = ToolErrorType.INVALID_PARAMETERS.code
            )

        val startTime = Platform.getCurrentTimestamp()

        while (session.isRunning() && (Platform.getCurrentTimestamp() - startTime) < params.timeoutMs) {
            delay(100)
        }

        val output = session.getOutput()
        val isRunning = session.isRunning()

        val metadata = mapOf(
            "session_id" to params.sessionId,
            "command" to session.command,
            "is_running" to isRunning.toString(),
            "exit_code" to (session.exitCode?.toString() ?: ""),
            "execution_time_ms" to session.getExecutionTimeMs().toString()
        )

        return if (isRunning) {
            ToolResult.Pending(
                sessionId = params.sessionId,
                toolName = "wait-process",
                command = session.command,
                message = "Process still running after ${params.timeoutMs}ms timeout.\n\nPartial output:\n${output.take(1000)}",
                metadata = metadata
            )
        } else {
            // Clean up completed session
            ShellSessionManager.removeSession(params.sessionId)

            val exitCode = session.exitCode ?: -1
            if (exitCode == 0) {
                ToolResult.Success(output.ifEmpty { "(no output)" }, metadata)
            } else {
                ToolResult.Error(
                    message = "Process exited with code $exitCode:\n$output",
                    errorType = ToolErrorType.COMMAND_FAILED.code,
                    metadata = metadata
                )
            }
        }
    }
}

class WaitProcessTool : BaseExecutableTool<WaitProcessParams, ToolResult>() {
    override val name: String = "wait-process"
    override val description: String = "Wait for a background process to complete and return its output"
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Wait Process",
        tuiEmoji = "⏳",
        composeIcon = "terminal",
        category = ToolCategory.Execution,
        schema = WaitProcessSchema
    )

    override fun getParameterClass(): String = WaitProcessParams::class.simpleName ?: "WaitProcessParams"

    override fun createToolInvocation(params: WaitProcessParams): ToolInvocation<WaitProcessParams, ToolResult> {
        if (params.sessionId.isBlank()) {
            throw ToolException("sessionId is required", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }
        return WaitProcessInvocation(params, this)
    }
}

// ============================================================================
// KillProcess Tool - Terminate a running process
// ============================================================================

@Serializable
data class KillProcessParams(
    val sessionId: String
)

object KillProcessSchema : DeclarativeToolSchema(
    description = "Terminate a running process by session ID",
    properties = mapOf(
        "sessionId" to string(
            description = "The session ID of the process to terminate",
            required = true
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName sessionId=\"abc-123\""
    }
}

class KillProcessInvocation(
    params: KillProcessParams,
    tool: KillProcessTool
) : BaseToolInvocation<KillProcessParams, ToolResult>(params, tool) {

    override fun getDescription(): String = "Kill session: ${params.sessionId}"
    override fun getToolLocations(): List<ToolLocation> = emptyList()

    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        val session = ShellSessionManager.getSession(params.sessionId)
            ?: return ToolResult.Error(
                message = "Session not found: ${params.sessionId}",
                errorType = ToolErrorType.INVALID_PARAMETERS.code
            )

        val wasRunning = session.isRunning()
        val output = session.getOutput()

        val killed = session.kill()
        ShellSessionManager.removeSession(params.sessionId)

        val metadata = mapOf(
            "session_id" to params.sessionId,
            "command" to session.command,
            "was_running" to wasRunning.toString(),
            "killed" to killed.toString(),
            "execution_time_ms" to session.getExecutionTimeMs().toString()
        )

        return if (killed || !wasRunning) {
            ToolResult.Success(
                content = if (wasRunning) {
                    "Process terminated successfully.\n\nFinal output:\n$output"
                } else {
                    "Process was already completed.\n\nOutput:\n$output"
                },
                metadata = metadata
            )
        } else {
            ToolResult.Error(
                message = "Failed to terminate process",
                errorType = ToolErrorType.COMMAND_FAILED.code,
                metadata = metadata
            )
        }
    }
}

class KillProcessTool : BaseExecutableTool<KillProcessParams, ToolResult>() {
    override val name: String = "kill-process"
    override val description: String = "Terminate a running process by session ID"
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Kill Process",
        tuiEmoji = "🛑",
        composeIcon = "stop",
        category = ToolCategory.Execution,
        schema = KillProcessSchema
    )

    override fun getParameterClass(): String = KillProcessParams::class.simpleName ?: "KillProcessParams"

    override fun createToolInvocation(params: KillProcessParams): ToolInvocation<KillProcessParams, ToolResult> {
        if (params.sessionId.isBlank()) {
            throw ToolException("sessionId is required", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }
        return KillProcessInvocation(params, this)
    }
}
