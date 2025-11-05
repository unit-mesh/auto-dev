package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.JsExport
import kotlin.js.Promise

/**
 * JS exports for Agents
 *
 * Provides JavaScript-friendly exports for error recovery and analysis agents
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
 * JS-friendly AnalysisContext
 */
@JsExport
data class JsAnalysisContext(
    val content: String,
    val contentType: String = "text",
    val source: String = "unknown",
    val metadata: Map<String, String> = emptyMap()
) {
    fun toCommon(): ContentHandlerContext = ContentHandlerContext(
        content = content,
        contentType = contentType,
        source = source,
        metadata = metadata
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
 * JS-friendly AnalysisAgent
 * Returns ToolResult directly
 */
@JsExport
class JsAnalysisAgent(
    private val llmService: KoogLLMService,
    private val contentThreshold: Int = 5000
) {
    private val agent = AnalysisAgent(llmService, contentThreshold)

    /**
     * Analyze content intelligently
     * Returns a ToolResult with analysis and metadata
     */
    fun execute(context: JsAnalysisContext): Promise<ToolResult.AgentResult> {
        return GlobalScope.promise {
            val commonContext = context.toCommon()
            agent.execute(commonContext) { /* ignore progress */ }
        }
    }

    /**
     * Check if content needs analysis
     */
    fun needsHandling(content: String): Boolean {
        return agent.needsHandling(content)
    }
}
