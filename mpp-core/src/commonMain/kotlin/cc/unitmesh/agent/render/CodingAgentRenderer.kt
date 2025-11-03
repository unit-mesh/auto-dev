package cc.unitmesh.agent.render

/**
 * Output renderer interface for CodingAgent
 * Allows customization of output formatting (e.g., CLI vs TUI)
 */
interface CodingAgentRenderer {
    fun renderIterationHeader(current: Int, max: Int)
    fun renderLLMResponseStart()
    fun renderLLMResponseChunk(chunk: String)
    fun renderLLMResponseEnd()
    fun renderToolCall(toolName: String, paramsStr: String)
    fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?)
    fun renderTaskComplete()
    fun renderFinalResult(success: Boolean, message: String, iterations: Int)
    fun renderError(message: String)
    fun renderRepeatWarning(toolName: String, count: Int)
}