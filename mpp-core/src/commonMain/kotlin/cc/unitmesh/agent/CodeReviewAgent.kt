package cc.unitmesh.agent

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.executor.CodeReviewAgentExecutor
import cc.unitmesh.agent.linter.LinterRegistry
import cc.unitmesh.agent.linter.LinterSummary
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tracker.IssueTracker
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Review types for code review
 */
enum class ReviewType {
    COMPREHENSIVE,
    SECURITY,
    PERFORMANCE,
    STYLE
}

@Serializable
data class ReviewTask(
    val filePaths: List<String> = emptyList(),
    val reviewType: ReviewType = ReviewType.COMPREHENSIVE,
    val projectPath: String,
    val additionalContext: String = ""
)

@Serializable
data class AnalysisTask(
    val reviewType: String = "COMPREHENSIVE",
    val filePaths: List<String> = emptyList(),
    val projectPath: String,

    // Optional: Pre-collected data for data-driven approach
    val codeContent: Map<String, String> = emptyMap(),
    val lintResults: Map<String, String> = emptyMap(),
    val diffContext: String = "",

    // Optional: Commit context for intent analysis
    val commitMessage: String = "",
    val commitId: String = "",
    val codeChanges: Map<String, String> = emptyMap(),

    // Optional: Issue tracker configuration
    val repoUrl: String = "",
    val issueToken: String = "",

    // Control flags
    val useTools: Boolean = true,  // If false and codeContent provided, use data-driven
    val analyzeIntent: Boolean = false  // If true, focus on intent analysis with mermaid
)

class CodeReviewAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    override val maxIterations: Int = 50,
    private val renderer: CodingAgentRenderer = DefaultCodingAgentRenderer(),
    private val fileSystem: ToolFileSystem? = null,
    private val shellExecutor: ShellExecutor? = null,
    private val mcpToolConfigService: McpToolConfigService,
    private val enableLLMStreaming: Boolean = true,
    private val issueTracker: IssueTracker? = null  // Optional issue tracker
) : MainAgent<ReviewTask, ToolResult.AgentResult>(
    AgentDefinition(
        name = "CodeReviewAgent",
        displayName = "Code Review Agent",
        description = "Expert code reviewer analyzing quality, security, performance, and best practices",
        promptConfig = PromptConfig(
            systemPrompt = "You are an expert code reviewer with deep knowledge of software engineering best practices.",
            queryTemplate = null,
            initialMessages = emptyList()
        ),
        modelConfig = ModelConfig.default(),
        runConfig = RunConfig(
            maxTurns = 50,
            maxTimeMinutes = 20,
            terminateOnError = false
        )
    )
), CodeReviewService {

    private val logger = getLogger("CodeReviewAgent")
    private val promptRenderer = CodeReviewAgentPromptRenderer()

    private val actualFileSystem = fileSystem ?: DefaultToolFileSystem(projectPath = projectPath)

    private val toolRegistry = run {
        logger.info { "Initializing ToolRegistry for CodeReviewAgent" }
        ToolRegistry(
            fileSystem = actualFileSystem,
            shellExecutor = shellExecutor ?: DefaultShellExecutor(),
            configService = mcpToolConfigService,
            subAgentManager = cc.unitmesh.agent.core.SubAgentManager(),
            llmService = llmService
        )
    }

    private val policyEngine = DefaultPolicyEngine()

    private val toolOrchestrator = ToolOrchestrator(
        registry = toolRegistry,
        policyEngine = policyEngine,
        renderer = renderer,
        mcpConfigService = mcpToolConfigService
    )

    private val executor = CodeReviewAgentExecutor(
        projectPath = projectPath,
        llmService = llmService,
        toolOrchestrator = toolOrchestrator,
        renderer = renderer,
        maxIterations = maxIterations,
        enableLLMStreaming = enableLLMStreaming
    )

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            initializeWorkspace(projectPath)
        }
    }

    override suspend fun execute(
        input: ReviewTask,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        initializeWorkspace(input.projectPath)

        val context = buildContext(input)
        val systemPrompt = buildSystemPrompt(context)

        val codeReviewResult = executor.execute(input, systemPrompt, context.linterSummary, onProgress)

        return ToolResult.AgentResult(
            success = codeReviewResult.success,
            content = codeReviewResult.message,
            metadata = mapOf(
                "reviewType" to input.reviewType.name,
                "filesReviewed" to input.filePaths.toString(),
                "findings" to Json.encodeToString(codeReviewResult.findings)
            )
        )
    }

    override suspend fun executeTask(task: ReviewTask): AgentResult {
        val context = buildContext(task)
        val systemPrompt = buildSystemPrompt(context)

        val codeReviewResult = executor.execute(task, systemPrompt, context.linterSummary) {}

        return AgentResult(
            success = codeReviewResult.success,
            message = codeReviewResult.message,
            steps = listOf(
                AgentStep(
                    step = 1,
                    action = "code_review",
                    tool = null,
                    params = mapOf(
                        "reviewType" to task.reviewType.name,
                        "filesReviewed" to task.filePaths.size,
                        "findings" to codeReviewResult.findings
                    ),
                    result = codeReviewResult.message,
                    success = codeReviewResult.success
                )
            ),
            edits = emptyList()
        )
    }

    suspend fun analyze(
        task: AnalysisTask,
        language: String = "ZH",
        onProgress: (String) -> Unit = {}
    ): AnalysisResult {
        logger.info { "Starting unified analysis - useTools: ${task.useTools}, analyzeIntent: ${task.analyzeIntent}" }
        return analyzeWithTools(task, language, onProgress)
    }

    private suspend fun analyzeWithTools(
        task: AnalysisTask,
        language: String,
        onProgress: (String) -> Unit
    ): AnalysisResult {
        logger.info { "Using unified analysis approach with CodeReviewAgentPromptRenderer" }

        initializeWorkspace(task.projectPath)
        val linterSummary = if (task.filePaths.isNotEmpty()) {
            try {
                val linterRegistry = LinterRegistry.getInstance()
                linterRegistry.getLinterSummaryForFiles(task.filePaths, task.projectPath)
            } catch (e: Exception) {
                logger.warn { "Failed to get linter summary: ${e.message}" }
                null
            }
        } else {
            null
        }

        val codeContent: Map<String, String> = task.codeContent.ifEmpty {
            task.filePaths.associateWith { filePath ->
                try {
                    actualFileSystem.readFile(filePath) ?: ""
                } catch (e: Exception) {
                    ""
                }
            }
        }

        // Format lint results
        val lintResults = if (task.lintResults.isNotEmpty()) {
            task.lintResults
        } else if (linterSummary != null) {
            task.filePaths.associateWith { file ->
                LinterSummary.format(linterSummary)
            }
        } else {
            emptyMap()
        }

        val systemPrompt = promptRenderer.renderAnalysisPrompt(
            reviewType = task.reviewType,
            filePaths = task.filePaths,
            codeContent = codeContent,
            lintResults = lintResults,
            diffContext = task.diffContext,
            toolList = AgentToolFormatter.formatToolListForAI(toolRegistry.getAllTools().values.toList()),
            language = language
        )

        val conversationManager = cc.unitmesh.agent.conversation.ConversationManager(llmService, systemPrompt)
        val toolCallParser = cc.unitmesh.agent.parser.ToolCallParser()
        val analysisOutput = StringBuilder()
        var currentIteration = 0
        var usedTools = false

        try {
            while (currentIteration < maxIterations) {
                currentIteration++
                logger.debug { "Analysis iteration $currentIteration/$maxIterations" }
                renderer.renderIterationHeader(currentIteration, maxIterations)

                val llmResponse = StringBuilder()
                try {
                    renderer.renderLLMResponseStart()
                    if (currentIteration == 1) {
                        conversationManager.sendMessage("Start analysis", compileDevIns = true).collect { chunk: String ->
                            llmResponse.append(chunk)
                            renderer.renderLLMResponseChunk(chunk)
                            onProgress(chunk)
                        }
                    } else {
                        conversationManager.sendMessage(
                            "Please continue with your analysis based on the tool results above. " +
                                    "Use additional tools if needed, or provide your final analysis if you have all the information.",
                            compileDevIns = true
                        ).collect { chunk: String ->
                            llmResponse.append(chunk)
                            renderer.renderLLMResponseChunk(chunk)
                            onProgress(chunk)
                        }
                    }
                    renderer.renderLLMResponseEnd()
                    conversationManager.addAssistantResponse(llmResponse.toString())
                    analysisOutput.append(llmResponse.toString())
                } catch (e: Exception) {
                    logger.error(e) { "LLM call failed during analysis: ${e.message}" }
                    renderer.renderError("❌ Analysis failed: ${e.message}")
                    return AnalysisResult(
                        success = false,
                        content = "❌ Analysis failed: ${e.message}",
                        usedTools = usedTools
                    )
                }

                // Parse tool calls from LLM response
                val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
                if (toolCalls.isEmpty()) {
                    logger.info { "No tool calls found, analysis complete" }
                    renderer.renderTaskComplete()
                    break
                }

                usedTools = true
                logger.info { "Found ${toolCalls.size} tool call(s), executing..." }

                // Execute tool calls
                val toolResults = executeToolCallsForAnalysis(toolCalls)
                val toolResultsText = formatToolResults(toolResults)
                conversationManager.addToolResults(toolResultsText)
                
                // Also append tool results to analysis output for visibility
                analysisOutput.append("\n\n<!-- Tool Execution Results -->\n")
                analysisOutput.append(toolResultsText)
            }

            if (currentIteration >= maxIterations) {
                logger.warn { "Analysis reached max iterations ($maxIterations)" }
                renderer.renderError("⚠️ Analysis reached max iterations ($maxIterations)")
            }
        } catch (e: Exception) {
            logger.error(e) { "Analysis failed: ${e.message}" }
            renderer.renderError("❌ Analysis failed: ${e.message}")
            return AnalysisResult(
                success = false,
                content = "❌ Analysis failed: ${e.message}",
                usedTools = usedTools
            )
        }

        return AnalysisResult(
            success = true,
            content = analysisOutput.toString(),
            mermaidDiagram = null,
            issuesAnalyzed = emptyList(),
            usedTools = usedTools
        )
    }

    /**
     * Execute tool calls for analysis and return results
     */
    private suspend fun executeToolCallsForAnalysis(
        toolCalls: List<cc.unitmesh.agent.state.ToolCall>
    ): List<Triple<String, Map<String, Any>, cc.unitmesh.agent.orchestrator.ToolExecutionResult>> {
        val results = mutableListOf<Triple<String, Map<String, Any>, cc.unitmesh.agent.orchestrator.ToolExecutionResult>>()

        for (toolCall in toolCalls) {
            val toolName = toolCall.toolName
            val params = toolCall.params.mapValues { it.value as Any }
            val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

            try {
                logger.info { "Executing tool: $toolName" }
                val paramsStr = params.entries.joinToString(" ") { (key, value) ->
                    "$key=\"$value\""
                }
                renderer.renderToolCall(toolName, paramsStr)

                val context = cc.unitmesh.agent.orchestrator.ToolExecutionContext(
                    workingDirectory = projectPath,
                    environment = emptyMap()
                )

                val executionResult = toolOrchestrator.executeToolCall(
                    toolName,
                    params,
                    context
                )

                results.add(Triple(toolName, params, executionResult))
                
                // Render tool result
                val fullOutput = when (val result = executionResult.result) {
                    is cc.unitmesh.agent.tool.ToolResult.Error -> "Error: ${result.message}"
                    else -> executionResult.content
                }
                renderer.renderToolResult(
                    toolName,
                    executionResult.isSuccess,
                    executionResult.content,
                    fullOutput
                )
            } catch (e: Exception) {
                logger.error(e) { "Tool execution failed: ${e.message}" }
                val endTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                val errorResult = cc.unitmesh.agent.orchestrator.ToolExecutionResult.failure(
                    executionId = "exec_error_${endTime}",
                    toolName = toolName,
                    error = "Tool execution failed: ${e.message}",
                    startTime = startTime,
                    endTime = endTime
                )
                results.add(Triple(toolName, params, errorResult))
                renderer.renderError("Tool execution failed: ${e.message}")
            }
        }

        return results
    }

    /**
     * Format tool results for feedback to LLM
     */
    private fun formatToolResults(
        results: List<Triple<String, Map<String, Any>, cc.unitmesh.agent.orchestrator.ToolExecutionResult>>
    ): String = buildString {
        appendLine("## Tool Execution Results")
        appendLine()

        results.forEachIndexed { index, (toolName, params, executionResult) ->
            appendLine("### Tool ${index + 1}: $toolName")
            
            if (params.isNotEmpty()) {
                appendLine("**Parameters:**")
                params.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
            }
            
            appendLine("**Result:**")
            when (val result = executionResult.result) {
                is cc.unitmesh.agent.tool.ToolResult.Success -> {
                    appendLine("```")
                    appendLine(result.content)
                    appendLine("```")
                }
                is cc.unitmesh.agent.tool.ToolResult.Error -> {
                    appendLine("❌ Error: ${result.message}")
                }
                is cc.unitmesh.agent.tool.ToolResult.AgentResult -> {
                    appendLine(if (result.success) "✅ Success" else "❌ Failed")
                    appendLine(result.content)
                }
            }
            appendLine()
        }
    }

    /**
     * Generate fixes for identified issues
     * Uses code content, lint results, and analysis output to provide actionable fixes in unified diff format
     *
     * @param codeContent Map of file paths to their content
     * @param lintResults List of lint results for files
     * @param analysisOutput The AI analysis output from previous analysis step
     * @param language Language for the prompt ("EN" or "ZH")
     * @param onProgress Callback for streaming progress
     * @return AnalysisResult containing the fix generation output
     */
    suspend fun generateFixes(
        codeContent: Map<String, String>,
        lintResults: List<cc.unitmesh.agent.linter.LintFileResult>,
        analysisOutput: String,
        language: String = "EN",
        onProgress: (String) -> Unit = {}
    ): AnalysisResult {
        logger.info { "Starting fix generation - ${codeContent.size} files, ${lintResults.size} lint results" }

        try {
            val prompt = promptRenderer.renderFixGenerationPrompt(
                codeContent = codeContent,
                lintResults = lintResults,
                analysisOutput = analysisOutput,
                language = language
            )

            logger.debug { "Fix generation prompt size: ${prompt.length} chars" }

            val fixOutput = StringBuilder()
            try {
                // Use renderer for better UI experience
                renderer.renderLLMResponseStart()
                llmService.streamPrompt(prompt, compileDevIns = false).collect { chunk ->
                    fixOutput.append(chunk)
                    renderer.renderLLMResponseChunk(chunk)
                    onProgress(chunk)
                }
                renderer.renderLLMResponseEnd()
            } catch (e: Exception) {
                logger.error(e) { "LLM call failed during fix generation: ${e.message}" }
                renderer.renderError("❌ Fix generation failed: ${e.message}")
                return AnalysisResult(
                    success = false,
                    content = "❌ Fix generation failed: ${e.message}",
                    usedTools = false
                )
            }

            logger.info { "Fix generation completed - ${fixOutput.length} chars output" }

            return AnalysisResult(
                success = true,
                content = fixOutput.toString(),
                usedTools = false
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate fixes: ${e.message}" }
            renderer.renderError("Error generating fixes: ${e.message}")
            return AnalysisResult(
                success = false,
                content = "Error generating fixes: ${e.message}",
                usedTools = false
            )
        }
    }

    override fun buildSystemPrompt(context: CodeReviewContext, language: String): String {
        return CodeReviewAgentPromptRenderer().renderAnalysisPrompt(
            reviewType = context.reviewType.name,
            filePaths = context.filePaths,
            codeContent = mapOf(),
            lintResults = mapOf(),
            diffContext = "",
            toolList = AgentToolFormatter.formatToolListForAI(toolRegistry.getAllTools().values.toList()),
            language = language
        )
    }

    private fun initializeWorkspace(projectPath: String) {
    }

    private suspend fun buildContext(task: ReviewTask): CodeReviewContext {
        val linterSummary = if (task.filePaths.isNotEmpty()) {
            try {
                LinterRegistry.getInstance().getLinterSummaryForFiles(task.filePaths, task.projectPath)
            } catch (e: Exception) {
                logger.warn { "Failed to get linter summary: ${e.message}" }
                null
            }
        } else {
            null
        }

        logger.info { "Linters summary: $linterSummary" }

        val allTools = toolRegistry.getAllTools()
        return CodeReviewContext.fromTask(task, allTools.values.toList(), linterSummary)
    }

    override fun validateInput(input: Map<String, Any>): ReviewTask {
        val filePaths = (input["filePaths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val reviewTypeStr = input["reviewType"] as? String ?: "COMPREHENSIVE"
        val reviewType = try {
            ReviewType.valueOf(reviewTypeStr)
        } catch (e: Exception) {
            ReviewType.COMPREHENSIVE
        }
        val projectPath = input["projectPath"] as? String
            ?: throw IllegalArgumentException("projectPath is required")
        val additionalContext = input["additionalContext"] as? String ?: ""

        return ReviewTask(
            filePaths = filePaths,
            reviewType = reviewType,
            projectPath = projectPath,
            additionalContext = additionalContext
        )
    }

    override fun formatOutput(output: ToolResult.AgentResult): String {
        return output.content
    }

    override fun getParameterClass(): String = ReviewTask::class.simpleName ?: "ReviewTask"

    override val name: String = definition.name
    override val description: String = definition.description
}

interface CodeReviewService {
    suspend fun executeTask(task: ReviewTask): AgentResult
    fun buildSystemPrompt(context: CodeReviewContext, language: String = "ZH"): String
}

/**
 * Result of code review
 */
@Serializable
data class CodeReviewResult(
    val success: Boolean,
    val message: String,
    val findings: List<ReviewFinding> = emptyList()
)

/**
 * A single finding from code review
 */
@Serializable
data class ReviewFinding(
    val severity: Severity,
    val category: String,
    val description: String,
    val filePath: String? = null,
    val lineNumber: Int? = null,
    val suggestion: String? = null
) {
    companion object {
        fun parseFindings(content: String): List<ReviewFinding> {
            val findings = mutableListOf<ReviewFinding>()
            val lines = content.lines()
            var currentSeverity = Severity.INFO

            for (line in lines) {
                when {
                    line.contains("CRITICAL", ignoreCase = true) -> currentSeverity = Severity.CRITICAL
                    line.contains("HIGH", ignoreCase = true) -> currentSeverity = Severity.HIGH
                    line.contains("MEDIUM", ignoreCase = true) -> currentSeverity = Severity.MEDIUM
                    line.contains("LOW", ignoreCase = true) -> currentSeverity = Severity.LOW
                    line.startsWith("-") || line.startsWith("*") || line.startsWith("####") -> {
                        val description = line.trimStart('-', '*', '#', ' ')
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
}

enum class Severity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

data class AnalysisResult(
    val success: Boolean,
    val content: String,
    val mermaidDiagram: String? = null,
    val issuesAnalyzed: List<String> = emptyList(),
    val usedTools: Boolean = false
)
