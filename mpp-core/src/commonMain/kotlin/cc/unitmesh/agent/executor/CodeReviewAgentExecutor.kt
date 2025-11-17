package cc.unitmesh.agent.executor

import cc.unitmesh.agent.CodeReviewResult
import cc.unitmesh.agent.ReviewFinding
import cc.unitmesh.agent.ReviewTask
import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.agent.linter.LinterSummary
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.orchestrator.ToolExecutionResult
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolResultFormatter
import cc.unitmesh.agent.vcs.context.DiffContextCompressor
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.yield
import cc.unitmesh.agent.orchestrator.ToolExecutionContext as OrchestratorContext

/**
 * Executor for CodeReviewAgent
 * Handles the execution flow for code review tasks with tool calling support
 */
class CodeReviewAgentExecutor(
    projectPath: String,
    llmService: KoogLLMService,
    toolOrchestrator: ToolOrchestrator,
    renderer: CodingAgentRenderer,
    maxIterations: Int = 50,
    enableLLMStreaming: Boolean = true
) : BaseAgentExecutor(
    projectPath = projectPath,
    llmService = llmService,
    toolOrchestrator = toolOrchestrator,
    renderer = renderer,
    maxIterations = maxIterations,
    enableLLMStreaming = enableLLMStreaming
) {
    private val logger = getLogger("CodeReviewAgentExecutor")
    private val findings = mutableListOf<ReviewFinding>()
    private val diffCompressor = DiffContextCompressor(
        maxLinesPerFile = 500,
        maxTotalLines = 10000
    )

    suspend fun execute(
        task: ReviewTask,
        systemPrompt: String,
        linterSummary: LinterSummary? = null,
        onProgress: (String) -> Unit = {}
    ): CodeReviewResult {
        resetExecution()
        conversationManager = ConversationManager(llmService, systemPrompt)
        val initialUserMessage = buildInitialUserMessage(task, linterSummary)

        val reviewTarget = when {
            task.patch != null -> task.patch
            task.filePaths.isNotEmpty() -> "${task.filePaths.size} files"
            else -> "code"
        }
        logger.info { "Starting code review: ${task.reviewType} for $reviewTarget" }

        while (shouldContinue()) {
            yield()

            currentIteration++
            renderer.renderIterationHeader(currentIteration, maxIterations)

            val llmResponse = StringBuilder()

            try {
                val message = if (currentIteration == 1) initialUserMessage else buildContinuationMessage()
                val response = getLLMResponse(message, compileDevIns = false) { chunk ->
                    onProgress(chunk)
                }
                llmResponse.append(response)
            } catch (e: Exception) {
                logger.error(e) { "LLM call failed: ${e.message}" }
                onProgress("❌ LLM call failed: ${e.message}")
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

    private suspend fun buildInitialUserMessage(
        task: ReviewTask,
        linterSummary: LinterSummary?
    ): String {
        return buildString {
            appendLine("Please review the following code changes:")
            appendLine()
            appendLine("**Project Path**: ${task.projectPath}")
            appendLine("**Review Type**: ${task.reviewType}")
            appendLine()

            // Add Git diff information if available
            if (task.patch != null) {
                appendLine("## Code Changes (Git Diff)")
                appendLine()
                
                // Compress the patch to fit within context limits
                val compressedPatch = diffCompressor.compress(task.patch)
                appendLine(compressedPatch)
            } else if (task.filePaths.isNotEmpty()) {
                // Fallback to file list if no diff info provided
                appendLine("**Files to review** (${task.filePaths.size} files):")
                task.filePaths.forEach { filePath ->
                    appendLine("  - $filePath")
                }
                appendLine()
            }

            if (task.additionalContext.isNotBlank()) {
                appendLine("**Additional context**:")
                appendLine(task.additionalContext)
                appendLine()
            }

            // Add linter information to user message
            if (linterSummary != null) {
                appendLine("## Linter Information")
                appendLine()
                appendLine(LinterSummary.format(linterSummary))
                appendLine()
            }

            appendLine("**Instructions**:")
            if (task.patch != null) {
                appendLine("1. First, analyze the linter results above (if provided)")
                appendLine("2. Review the git diff changes shown above")
                appendLine("3. Use the read-file tool ONLY if you need additional context beyond the diff")
                appendLine("4. Provide a thorough code review following the guidelines in the system prompt")
                appendLine("5. Focus on issues beyond what linters can detect")
            } else if (task.filePaths.isNotEmpty()) {
                appendLine("1. First, analyze the linter results above (if provided)")
                appendLine("2. Use the read-file tool to read the content of each file")
                appendLine("3. Provide a thorough code review following the guidelines in the system prompt")
                appendLine("4. Focus on issues beyond what linters can detect")
            } else {
                appendLine("Please provide a thorough code review following the guidelines in the system prompt.")
                appendLine("Use tools as needed to read files and gather information.")
            }
        }
    }

    override fun buildContinuationMessage(): String {
        return "Please continue with the code review based on the tool execution results above. " +
                "Use additional tools if needed, or provide your final review if you have all the information."
    }

    private fun isReviewComplete(response: String): Boolean {
        return hasCompletionIndicator(response, listOf(
            "review complete",
            "review is complete",
            "finished reviewing",
            "completed the review",
            "final review",
            "summary:",
            "## summary"
        ))
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

        val parsedFindings = ReviewFinding.parseFindings(finalResponse)
        findings.addAll(parsedFindings)

        return CodeReviewResult(
            success = true,
            message = finalResponse,
            findings = findings.toList()
        )
    }
}
