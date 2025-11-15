package cc.unitmesh.agent.executor

import cc.unitmesh.agent.CodeReviewResult
import cc.unitmesh.agent.ReviewFinding
import cc.unitmesh.agent.ReviewTask
import cc.unitmesh.agent.Severity
import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.orchestrator.ToolExecutionResult
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.parser.ToolCallParser
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolResultFormatter
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.yield
import cc.unitmesh.agent.orchestrator.ToolExecutionContext as OrchestratorContext

/**
 * Executor for CodeReviewAgent
 * Handles the execution flow for code review tasks with tool calling support
 */
class CodeReviewAgentExecutor(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    private val toolOrchestrator: ToolOrchestrator,
    private val renderer: CodingAgentRenderer,
    private val maxIterations: Int = 50,
    private val enableLLMStreaming: Boolean = true
) {
    private val logger = getLogger("CodeReviewAgentExecutor")
    private val toolCallParser = ToolCallParser()
    private var currentIteration = 0
    private val findings = mutableListOf<ReviewFinding>()

    private var conversationManager: ConversationManager? = null

    suspend fun execute(
        task: ReviewTask,
        systemPrompt: String,
        onProgress: (String) -> Unit = {}
    ): CodeReviewResult {
        resetExecution()
        conversationManager = ConversationManager(llmService, systemPrompt)
        val initialUserMessage = buildInitialUserMessage(task)

        logger.info { "Starting code review: ${task.reviewType} for ${task.filePaths.size} files" }
        onProgress("🔍 Starting code review...")
        onProgress("Project: ${task.projectPath}")
        onProgress("Review Type: ${task.reviewType}")

        while (shouldContinue()) {
            yield()

            currentIteration++
            renderer.renderIterationHeader(currentIteration, maxIterations)

            val llmResponse = StringBuilder()

            try {
                renderer.renderLLMResponseStart()

                if (enableLLMStreaming) {
                    // Streaming mode: receive and render chunks
                    if (currentIteration == 1) {
                        conversationManager!!.sendMessage(initialUserMessage, compileDevIns = false).cancellable().collect { chunk ->
                            llmResponse.append(chunk)
                            renderer.renderLLMResponseChunk(chunk)
                        }
                    } else {
                        conversationManager!!.sendMessage(buildContinuationMessage(), compileDevIns = false).cancellable().collect { chunk ->
                            llmResponse.append(chunk)
                            renderer.renderLLMResponseChunk(chunk)
                        }
                    }
                } else {
                    // Non-streaming mode: get complete response at once
                    val message = if (currentIteration == 1) initialUserMessage else buildContinuationMessage()
                    val response = llmService.sendPrompt(message)
                    llmResponse.append(response)
                    // Simulate streaming output by rendering in chunks
                    response.split(Regex("(?<=[.!?。！？]\\s)")).forEach { sentence ->
                        if (sentence.isNotBlank()) {
                            renderer.renderLLMResponseChunk(sentence)
                        }
                    }
                }

                renderer.renderLLMResponseEnd()
                conversationManager!!.addAssistantResponse(llmResponse.toString())
            } catch (e: Exception) {
                logger.error(e) { "LLM call failed: ${e.message}" }
                renderer.renderError("LLM call failed: ${e.message}")
                break
            }

            // Parse tool calls from LLM response
            val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
            if (toolCalls.isEmpty()) {
                // No tool calls, review is complete
                logger.info { "No tool calls found, review complete" }
                renderer.renderTaskComplete()
                break
            }

            // Execute tool calls
            val toolResults = executeToolCalls(toolCalls)
            val toolResultsText = ToolResultFormatter.formatMultipleToolResults(toolResults)
            conversationManager!!.addToolResults(toolResultsText)

            // Check if review is complete
            if (isReviewComplete(llmResponse.toString())) {
                logger.info { "Review complete" }
                renderer.renderTaskComplete()
                break
            }
        }

        onProgress("✅ Review complete")

        return buildResult()
    }

    private fun resetExecution() {
        currentIteration = 0
        findings.clear()
    }

    private fun shouldContinue(): Boolean {
        return currentIteration < maxIterations
    }

    private suspend fun buildInitialUserMessage(task: ReviewTask): String {
        return buildString {
            appendLine("Please review the following code:")
            appendLine()

            if (task.filePaths.isNotEmpty()) {
                appendLine("Files to review (${task.filePaths.size} files):")
                task.filePaths.forEach { filePath ->
                    appendLine("  - $filePath")
                }
                appendLine()
                appendLine("Use the read-file tool to read the content of each file before reviewing.")
            }

            appendLine("Review type: ${task.reviewType}")

            if (task.additionalContext.isNotBlank()) {
                appendLine()
                appendLine("Additional context:")
                appendLine(task.additionalContext)
            }

            appendLine()
            appendLine("Please provide a thorough code review following the guidelines in the system prompt.")
            appendLine("Use tools as needed to read files and gather information.")
        }
    }

    private fun buildContinuationMessage(): String {
        return "Please continue with the code review based on the tool execution results above. " +
                "Use additional tools if needed, or provide your final review if you have all the information."
    }

    private fun isReviewComplete(response: String): Boolean {
        // Check if the response contains review completion indicators
        val completionIndicators = listOf(
            "review complete",
            "review is complete",
            "finished reviewing",
            "completed the review",
            "final review",
            "summary:",
            "## summary"
        )

        val lowerResponse = response.lowercase()
        return completionIndicators.any { lowerResponse.contains(it) }
    }

    /**
     * Execute tool calls sequentially (similar to CodingAgentExecutor)
     */
    private suspend fun executeToolCalls(toolCalls: List<ToolCall>): List<Triple<String, Map<String, Any>, ToolExecutionResult>> {
        logger.info { "Executing ${toolCalls.size} tool calls" }
        val results = mutableListOf<Triple<String, Map<String, Any>, ToolExecutionResult>>()

        for (toolCall in toolCalls) {
            val toolName = toolCall.toolName
            val params = toolCall.params.mapValues { it.value as Any }
            val paramsStr = params.entries.joinToString(" ") { (key, value) ->
                "$key=\"$value\""
            }

            try {
                logger.info { "Executing tool: $toolName with params: $paramsStr" }
                renderer.renderToolCall(toolName, paramsStr)

                val context = OrchestratorContext(
                    workingDirectory = projectPath,
                    environment = emptyMap()
                )

                // Execute tool using ToolOrchestrator (same as CodingAgentExecutor)
                val executionResult = toolOrchestrator.executeToolCall(
                    toolName,
                    params,
                    context
                )

                results.add(Triple(toolName, params, executionResult))

                val fullOutput = when (val result = executionResult.result) {
                    is ToolResult.Error -> {
                        buildString {
                            appendLine("Error: ${result.message}")
                            appendLine("Error Type: ${result.errorType}")
                            executionResult.metadata["stderr"]?.let { stderr ->
                                if (stderr.isNotEmpty()) {
                                    appendLine("\nStderr:")
                                    appendLine(stderr)
                                }
                            }
                            executionResult.metadata["stdout"]?.let { stdout ->
                                if (stdout.isNotEmpty()) {
                                    appendLine("\nStdout:")
                                    appendLine(stdout)
                                }
                            }
                        }
                    }
                    is ToolResult.AgentResult -> if (!result.success) result.content else executionResult.content
                    else -> executionResult.content
                }

                renderer.renderToolResult(toolName, executionResult.isSuccess, executionResult.content, fullOutput)
            } catch (e: Exception) {
                logger.error(e) { "Tool execution failed: $toolName" }
                val currentTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                val errorResult = ToolExecutionResult(
                    executionId = "error-$currentTime",
                    toolName = toolName,
                    result = ToolResult.Error("Tool execution failed: ${e.message}"),
                    startTime = currentTime,
                    endTime = currentTime,
                    state = cc.unitmesh.agent.state.ToolExecutionState.Failed(
                        "error-$currentTime",
                        "Tool execution failed: ${e.message}",
                        0
                    )
                )
                renderer.renderToolResult(toolName, false, null, "Error: ${e.message}")
                results.add(Triple(toolName, params, errorResult))
            }
        }

        return results
    }

    private fun buildResult(): CodeReviewResult {
        val finalResponse = conversationManager?.getHistory()
            ?.lastOrNull { it.role == cc.unitmesh.devins.llm.MessageRole.ASSISTANT }
            ?.content ?: "No review generated"

        // Parse findings from the final response
        val parsedFindings = parseFindings(finalResponse)
        findings.addAll(parsedFindings)

        return CodeReviewResult(
            success = true,
            message = finalResponse,
            findings = findings.toList()
        )
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
