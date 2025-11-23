package cc.unitmesh.agent.executor

import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.agent.document.DocumentTask
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.orchestrator.ToolExecutionResult
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.state.ToolExecutionState
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.schema.ToolResultFormatter
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock
import cc.unitmesh.agent.orchestrator.ToolExecutionContext as OrchestratorContext

/**
 * Document Agent Result
 */
data class DocumentAgentResult(
    val success: Boolean,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Executor for DocumentAgent
 * Handles the execution flow for document query tasks with DocQL tool calling support
 */
class DocumentAgentExecutor(
    projectPath: String,
    llmService: KoogLLMService,
    toolOrchestrator: ToolOrchestrator,
    renderer: CodingAgentRenderer,
    maxIterations: Int = 10,
    enableLLMStreaming: Boolean = true
) : BaseAgentExecutor(
    projectPath = projectPath,
    llmService = llmService,
    toolOrchestrator = toolOrchestrator,
    renderer = renderer,
    maxIterations = maxIterations,
    enableLLMStreaming = enableLLMStreaming
) {
    private val logger = getLogger("DocumentAgentExecutor")

    /**
     * Reset execution state
     */
    protected fun resetExecution() {
        currentIteration = 0
        conversationManager = null
    }

    suspend fun execute(
        task: DocumentTask,
        systemPrompt: String,
        onProgress: (String) -> Unit = {}
    ): DocumentAgentResult {
        resetExecution()
        conversationManager = ConversationManager(llmService, systemPrompt)

        val docPath = task.documentPath ?: "unspecified document"
        logger.info { "Starting document query: '${task.query}' for $docPath" }

        val initialUserMessage = buildInitialUserMessage(task)
        val finalResponse = StringBuilder()

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
                return DocumentAgentResult(
                    success = false,
                    content = "Error: ${e.message}"
                )
            }

            // Parse tool calls from LLM response
            val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
            if (toolCalls.isEmpty()) {
                // No tool calls, query is complete
                logger.info { "No tool calls found, query complete" }
                renderer.renderTaskComplete()
                finalResponse.append(llmResponse.toString())
                break
            }

            // Execute tool calls
            val toolResults = executeToolCalls(toolCalls)
            val toolResultsText = ToolResultFormatter.formatMultipleToolResults(toolResults)
            conversationManager!!.addToolResults(toolResultsText)

            // Check if query is complete
            if (isQueryComplete(llmResponse.toString())) {
                logger.info { "Query complete indicator found" }
                renderer.renderTaskComplete()
                finalResponse.append(llmResponse.toString())
                break
            }
        }

        if (currentIteration >= maxIterations) {
            logger.warn { "Reached maximum iterations ($maxIterations)" }
            renderer.renderError("⚠️  Reached maximum iterations")
            finalResponse.append("\n\n(Note: Reached maximum iterations)")
        }

        val result = finalResponse.toString().ifBlank {
            "Query completed but no response was generated."
        }

        return DocumentAgentResult(
            success = true,
            content = result,
            metadata = mapOf(
                "iterations" to currentIteration,
                "query" to task.query,
                "documentPath" to (task.documentPath ?: "N/A")
            )
        )
    }

    private fun buildInitialUserMessage(task: DocumentTask): String {
        return buildString {
            appendLine("## User Query")
            appendLine()
            appendLine(task.query)
            appendLine()

            if (task.documentPath != null) {
                appendLine("## Document Context")
                appendLine()
                appendLine("**Document Path**: ${task.documentPath}")
                appendLine()
            }

            appendLine("**Instructions**:")
            appendLine("1. Analyze the user's query carefully")
            appendLine("2. Use the `docql` tool to query the document and extract relevant information")
            appendLine("3. Synthesize the information and provide a clear, helpful answer")
            appendLine("4. If the document doesn't contain the needed information, say so clearly")
            appendLine()
            appendLine("Remember to use DocQL syntax like:")
            appendLine("- `\$.content.heading(\"Section Name\")` - Get content of a specific section")
            appendLine("- `\$.toc[*]` - Get all table of contents items")
            appendLine("- `\$.entities[?(@.type==\"API\")]` - Get entities of specific type")
        }
    }

    override fun buildContinuationMessage(): String {
        return "Based on the tool execution results above, please synthesize the information and provide a complete answer to the user's query. " +
                "Use additional tools if you need more information, or provide your final response if you have enough information."
    }

    private fun isQueryComplete(response: String): Boolean {
        return hasCompletionIndicator(response, listOf(
            "query complete",
            "answer is",
            "in summary",
            "to summarize",
            "final answer",
            "here's the information",
            "based on the document"
        ))
    }

    /**
     * Execute tool calls sequentially
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

                // Execute tool using ToolOrchestrator
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
                logger.error(e) { "Tool execution failed: ${toolName}" }
                renderer.renderError("Tool execution failed: ${e.message}")

                val currentTime = Clock.System.now().toEpochMilliseconds()
                val errorResult = ToolExecutionResult(
                    executionId = "error-$currentTime",
                    toolName = toolName,
                    result = ToolResult.Error(e.message ?: "Unknown error", "EXECUTION_FAILED"),
                    startTime = currentTime,
                    endTime = currentTime,
                    state = ToolExecutionState.Failed(
                        callId = "error-$currentTime",
                        error = e.message ?: "Unknown error",
                        duration = 0
                    ),
                    metadata = emptyMap()
                )
                results.add(Triple(toolName, params, errorResult))
            }
        }

        return results
    }
}
