package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.JsExport
import kotlin.js.Promise

/**
 * JS exports for SubAgents
 * 
 * Provides JavaScript-friendly exports for error recovery and log summary agents
 * Now returns ToolResult.AgentResult directly for better interoperability
 */

/**
 * JS-friendly ErrorContext
 */
@JsExport
data class JsErrorContext(
    val command: String,
    val errorMessage: String,
    val exitCode: Int? = null,
    val stdout: String? = null,
    val stderr: String? = null
) {
    fun toCommon(): ErrorContext = ErrorContext(
        command = command,
        errorMessage = errorMessage,
        exitCode = exitCode,
        stdout = stdout,
        stderr = stderr
    )
}

/**
 * JS-friendly LogSummaryContext
 */
@JsExport
data class JsLogSummaryContext(
    val command: String,
    val output: String,
    val exitCode: Int = 0,
    val executionTime: Int = 0
) {
    fun toCommon(): LogSummaryContext = LogSummaryContext(
        command = command,
        output = output,
        exitCode = exitCode,
        executionTime = executionTime
    )
}

/**
 * JS-friendly ErrorRecoveryAgent
 * Returns ToolResult directly
 */
@JsExport
class JsErrorRecoveryAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService
) {
    private val agent = ErrorRecoveryAgent(projectPath, llmService)
    
    /**
     * Analyze and recover from error
     * Returns a ToolResult with analysis and metadata
     */
    fun execute(errorContext: JsErrorContext): Promise<ToolResult.AgentResult> {
        return GlobalScope.promise {
            val commonContext = errorContext.toCommon()
            agent.execute(commonContext) { /* ignore progress */ }
        }
    }
}

/**
 * JS-friendly LogSummaryAgent
 * Returns ToolResult directly
 */
@JsExport
class JsLogSummaryAgent(
    private val llmService: KoogLLMService,
    private val threshold: Int = 2000
) {
    private val agent = LogSummaryAgent(llmService, threshold)
    
    /**
     * Summarize long output
     * Returns a ToolResult with summary and metadata
     */
    fun execute(context: JsLogSummaryContext): Promise<ToolResult.AgentResult> {
        return GlobalScope.promise {
            val commonContext = context.toCommon()
            agent.execute(commonContext) { /* ignore progress */ }
        }
    }
    
    /**
     * Check if output needs summarization
     */
    fun needsSummarization(output: String): Boolean {
        return agent.needsSummarization(output)
    }
}
