package cc.unitmesh.agent.render

import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.llm.compression.TokenInfo

interface CodingAgentRenderer {
    fun renderIterationHeader(current: Int, max: Int)
    fun renderLLMResponseStart()
    fun renderLLMResponseChunk(chunk: String)
    fun renderLLMResponseEnd()

    fun renderToolCall(toolName: String, paramsStr: String)
    fun renderToolResult(
        toolName: String,
        success: Boolean,
        output: String?,
        fullOutput: String?,
        metadata: Map<String, String> = emptyMap()
    )

    fun renderTaskComplete()
    fun renderFinalResult(success: Boolean, message: String, iterations: Int)
    fun renderError(message: String)
    fun renderRepeatWarning(toolName: String, count: Int)

    fun renderRecoveryAdvice(recoveryAdvice: String)

    fun updateTokenInfo(tokenInfo: TokenInfo) {}

    fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>)

    /**
     * Add a live terminal session to the timeline.
     * Called when a Shell tool starts execution with PTY support.
     */
    fun addLiveTerminal(
        sessionId: String,
        command: String,
        workingDirectory: String?,
        ptyHandle: Any?
    ) {
        // Default: no-op for renderers that don't support live terminals
    }

    /**
     * Update the status of a live terminal session.
     * Called when the shell command completes (either success or failure).
     *
     * @param sessionId The session ID of the live terminal
     * @param exitCode The exit code of the command (0 = success)
     * @param executionTimeMs The total execution time in milliseconds
     * @param output The captured output (optional, may be null if output is streamed via PTY)
     * @param cancelledByUser Whether the command was cancelled by the user (exit code 137)
     */
    fun updateLiveTerminalStatus(
        sessionId: String,
        exitCode: Int,
        executionTimeMs: Long,
        output: String? = null,
        cancelledByUser: Boolean = false
    ) {
        // Default: no-op for renderers that don't support live terminals
    }

    /**
     * Await the result of an async session.
     * Used when the Agent needs to wait for a shell command to complete before proceeding.
     *
     * @param sessionId The session ID to wait for
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return The final ToolResult (Success or Error)
     */
    suspend fun awaitSessionResult(sessionId: String, timeoutMs: Long): ToolResult {
        // Default: return error for renderers that don't support async sessions
        return ToolResult.Error("Async session not supported by this renderer")
    }
}
