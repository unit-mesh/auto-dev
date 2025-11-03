package cc.unitmesh.agent.orchestrator

import kotlinx.datetime.Clock

/**
 * Enhanced execution context for tool orchestration
 * Extends the basic ToolExecutionContext with orchestration-specific data
 */
data class ToolExecutionContext(
    val workingDirectory: String? = null,
    val environment: Map<String, String> = emptyMap(),
    val timeout: Long = 30000L, // 30 seconds default
    val metadata: Map<String, Any> = emptyMap(),

    // Orchestration-specific fields
    val executionId: String = generateExecutionId(),
    val parentExecutionId: String? = null,
    val maxRetries: Int = 0,
    val currentRetry: Int = 0,
    val userConfirmationRequired: Boolean = false,
    val dryRun: Boolean = false
) {
    companion object {
        private var counter = 0

        private fun generateExecutionId(): String {
            return "exec_${Clock.System.now().toEpochMilliseconds()}_${++counter}"
        }
    }
    
    /**
     * Create a child context for nested tool execution
     */
    fun createChildContext(
        executionId: String = generateExecutionId(),
        additionalMetadata: Map<String, Any> = emptyMap()
    ): ToolExecutionContext {
        return copy(
            executionId = executionId,
            parentExecutionId = this.executionId,
            metadata = this.metadata + additionalMetadata
        )
    }
    
    /**
     * Create a retry context
     */
    fun createRetryContext(): ToolExecutionContext {
        return copy(
            currentRetry = currentRetry + 1,
            executionId = generateExecutionId()
        )
    }
    
    /**
     * Check if more retries are available
     */
    fun canRetry(): Boolean {
        return currentRetry < maxRetries
    }
    
    /**
     * Convert to basic ToolExecutionContext for tool execution
     */
    fun toBasicContext(): cc.unitmesh.agent.tool.ToolExecutionContext {
        return cc.unitmesh.agent.tool.ToolExecutionContext(
            workingDirectory = workingDirectory,
            environment = environment,
            timeout = timeout,
            metadata = metadata
        )
    }
}
