package cc.unitmesh.agent.render

/**
 * Core renderer interface for CodingAgent
 * Defines the contract for all renderer implementations
 */
interface CodingAgentRenderer {
    // Lifecycle methods
    fun renderIterationHeader(current: Int, max: Int)
    fun renderLLMResponseStart()
    fun renderLLMResponseChunk(chunk: String)
    fun renderLLMResponseEnd()

    // Tool execution methods
    fun renderToolCall(toolName: String, paramsStr: String)
    fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?, metadata: Map<String, String> = emptyMap())

    // Status and completion methods
    fun renderTaskComplete()
    fun renderFinalResult(success: Boolean, message: String, iterations: Int)
    fun renderError(message: String)
    fun renderRepeatWarning(toolName: String, count: Int)

    // Error recovery methods
    fun renderRecoveryAdvice(recoveryAdvice: String)

    // Policy and permission methods
    fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>)
    
    /**
     * Add live terminal session (optional, for PTY-enabled platforms)
     * Default implementation does nothing - override in renderers that support live terminals
     */
    fun addLiveTerminal(
        sessionId: String,
        command: String,
        workingDirectory: String?,
        ptyHandle: Any?
    ) {
        // Default: no-op for renderers that don't support live terminals
    }
}

/**
 * Renderer type enumeration for different UI implementations
 */
enum class RendererType {
    CONSOLE,    // Default console output
    CLI,        // Enhanced CLI with colors and formatting
    TUI,        // Terminal UI with interactive elements
    WEB         // Web-based UI
}