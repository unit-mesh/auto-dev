package cc.unitmesh.agent.render

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

    fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>)

    fun addLiveTerminal(
        sessionId: String,
        command: String,
        workingDirectory: String?,
        ptyHandle: Any?
    ) {
        // Default: no-op for renderers that don't support live terminals
    }
}
