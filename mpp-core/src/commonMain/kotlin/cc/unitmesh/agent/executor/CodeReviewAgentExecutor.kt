package cc.unitmesh.agent.executor

import cc.unitmesh.agent.CodeReviewContext
import cc.unitmesh.agent.CodeReviewResult
import cc.unitmesh.agent.ReviewFinding
import cc.unitmesh.agent.ReviewTask
import cc.unitmesh.agent.Severity
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.llm.KoogLLMService

/**
 * Executor for CodeReviewAgent
 * Handles the execution flow for code review tasks
 */
class CodeReviewAgentExecutor(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    private val toolRegistry: ToolRegistry,
    private val renderer: CodingAgentRenderer,
    private val maxIterations: Int = 50,
    private val enableLLMStreaming: Boolean = true
) {
    private val logger = getLogger("CodeReviewAgentExecutor")

    suspend fun execute(
        task: ReviewTask,
        systemPrompt: String,
        onProgress: (String) -> Unit = {}
    ): CodeReviewResult {
        logger.info { "Starting code review: ${task.reviewType} for ${task.filePaths.size} files" }
        
        onProgress("🔍 Starting code review...")
        // Note: renderTaskStart is not available in base renderer

        try {
            // Build user message
            val userMessage = buildUserMessage(task)
            
            onProgress("📖 Reading files for review...")
            
            // Call LLM for analysis
            renderer.renderLLMResponseStart()
            
            val response = if (enableLLMStreaming) {
                callLLMStreaming(systemPrompt, userMessage, onProgress)
            } else {
                callLLM(systemPrompt, userMessage, onProgress)
            }
            
            renderer.renderLLMResponseEnd()
            
            onProgress("✅ Review complete")
            
            // Parse findings from response
            val findings = parseFindings(response)
            
            renderer.renderTaskComplete()
            
            return CodeReviewResult(
                success = true,
                message = response,
                findings = findings
            )
        } catch (e: Exception) {
            logger.error(e) { "Code review failed: ${e.message}" }
            renderer.renderError("Review failed: ${e.message}")
            
            return CodeReviewResult(
                success = false,
                message = "Review failed: ${e.message}",
                findings = emptyList()
            )
        }
    }

    private fun buildUserMessage(task: ReviewTask): String {
        return buildString {
            appendLine("Please review the following code:")
            appendLine()
            
            if (task.filePaths.isNotEmpty()) {
                appendLine("Files to review:")
                task.filePaths.forEach { file ->
                    appendLine("- $file")
                }
                appendLine()
            }
            
            appendLine("Review type: ${task.reviewType}")
            
            if (task.additionalContext.isNotBlank()) {
                appendLine()
                appendLine("Additional context:")
                appendLine(task.additionalContext)
            }
            
            appendLine()
            appendLine("Please provide a thorough code review following the guidelines in the system prompt.")
        }
    }

    private suspend fun callLLM(
        systemPrompt: String,
        userMessage: String,
        onProgress: (String) -> Unit
    ): String {
        onProgress("🤖 Analyzing code...")
        
        val fullPrompt = buildString {
            appendLine(systemPrompt)
            appendLine()
            appendLine(userMessage)
        }
        
        return llmService.sendPrompt(fullPrompt)
    }

    private suspend fun callLLMStreaming(
        systemPrompt: String,
        userMessage: String,
        onProgress: (String) -> Unit
    ): String {
        onProgress("🤖 Analyzing code...")
        
        val fullPrompt = buildString {
            appendLine(systemPrompt)
            appendLine()
            appendLine(userMessage)
        }
        
        val fullResponse = StringBuilder()
        
        llmService.streamPrompt(fullPrompt).collect { chunk ->
            fullResponse.append(chunk)
            renderer.renderLLMResponseChunk(chunk)
        }
        
        return fullResponse.toString()
    }

    private fun parseFindings(response: String): List<ReviewFinding> {
        // Simple parsing logic - can be enhanced
        val findings = mutableListOf<ReviewFinding>()
        
        // Look for common patterns indicating severity
        val lines = response.lines()
        var currentSeverity = Severity.INFO
        
        for (line in lines) {
            when {
                line.contains("CRITICAL", ignoreCase = true) -> currentSeverity = Severity.CRITICAL
                line.contains("HIGH", ignoreCase = true) -> currentSeverity = Severity.HIGH
                line.contains("MEDIUM", ignoreCase = true) -> currentSeverity = Severity.MEDIUM
                line.contains("LOW", ignoreCase = true) -> currentSeverity = Severity.LOW
                line.startsWith("-") || line.startsWith("*") -> {
                    // Extract finding from bullet point
                    val description = line.trimStart('-', '*', ' ')
                    if (description.length > 10) {
                        findings.add(
                            ReviewFinding(
                                severity = currentSeverity,
                                category = "General",
                                description = description
                            )
                        )
                    }
                }
            }
        }
        
        return findings
    }
}
