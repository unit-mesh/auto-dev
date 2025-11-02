package cc.unitmesh.agent.subagent

import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.JsExport
import kotlin.js.Promise

/**
 * JS exports for SubAgents
 * 
 * Provides JavaScript-friendly exports for error recovery and log summary agents
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
 * JS-friendly RecoveryResult
 */
@JsExport
data class JsRecoveryResult(
    val success: Boolean,
    val analysis: String,
    val suggestedActions: Array<String>,
    val recoveryCommands: Array<String>?,
    val shouldRetry: Boolean,
    val shouldAbort: Boolean
) {
    companion object {
        fun fromCommon(result: RecoveryResult): JsRecoveryResult = JsRecoveryResult(
            success = result.success,
            analysis = result.analysis,
            suggestedActions = result.suggestedActions.toTypedArray(),
            recoveryCommands = result.recoveryCommands?.toTypedArray(),
            shouldRetry = result.shouldRetry,
            shouldAbort = result.shouldAbort
        )
    }
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
 * JS-friendly LogSummaryResult
 */
@JsExport
data class JsLogSummaryResult(
    val success: Boolean,
    val summary: String,
    val keyPoints: Array<String>,
    val errors: Array<String>,
    val warnings: Array<String>,
    val nextSteps: Array<String>?
) {
    companion object {
        fun fromCommon(result: LogSummaryResult): JsLogSummaryResult = JsLogSummaryResult(
            success = result.success,
            summary = result.summary,
            keyPoints = result.keyPoints.toTypedArray(),
            errors = result.errors.toTypedArray(),
            warnings = result.warnings.toTypedArray(),
            nextSteps = result.nextSteps?.toTypedArray()
        )
    }
}

/**
 * JS-friendly ErrorRecoveryAgent
 */
@JsExport
class JsErrorRecoveryAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService
) {
    private val agent = ErrorRecoveryAgent(projectPath, llmService)
    
    /**
     * Analyze and recover from error
     */
    fun execute(errorContext: JsErrorContext): Promise<JsRecoveryResult> {
        return GlobalScope.promise {
            val commonContext = errorContext.toCommon()
            val result = agent.execute(commonContext) { /* ignore progress */ }
            JsRecoveryResult.fromCommon(result)
        }
    }
}

/**
 * JS-friendly LogSummaryAgent
 */
@JsExport
class JsLogSummaryAgent(
    private val llmService: KoogLLMService,
    private val threshold: Int = 2000
) {
    private val agent = LogSummaryAgent(llmService, threshold)
    
    /**
     * Summarize long output
     */
    fun execute(context: JsLogSummaryContext): Promise<JsLogSummaryResult> {
        return GlobalScope.promise {
            val commonContext = context.toCommon()
            val result = agent.execute(commonContext) { /* ignore progress */ }
            JsLogSummaryResult.fromCommon(result)
        }
    }
    
    /**
     * Check if output needs summarization
     */
    fun needsSummarization(output: String): Boolean {
        return agent.needsSummarization(output)
    }
}
