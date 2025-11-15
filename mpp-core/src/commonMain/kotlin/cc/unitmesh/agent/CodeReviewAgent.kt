package cc.unitmesh.agent

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.executor.CodeReviewAgentExecutor
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
import cc.unitmesh.agent.tracker.IssueInfo
import cc.unitmesh.agent.tracker.IssueTracker
import cc.unitmesh.agent.tracker.NoOpIssueTracker
import cc.unitmesh.agent.tracker.GitHubIssueTracker
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Review types for code review
 */
enum class ReviewType {
    COMPREHENSIVE,  // Full review including style, security, performance, best practices
    SECURITY,       // Focus on security vulnerabilities
    PERFORMANCE,    // Focus on performance issues
    STYLE          // Focus on code style and conventions
}

/**
 * Input task for code review agent
 */
@Serializable
data class ReviewTask(
    val filePaths: List<String> = emptyList(),
    val reviewType: ReviewType = ReviewType.COMPREHENSIVE,
    val projectPath: String,
    val additionalContext: String = ""
)

/**
 * Input task for intent analysis from commit
 */
@Serializable
data class IntentAnalysisTask(
    val commitMessage: String,
    val commitId: String = "",
    val codeChanges: Map<String, String> = emptyMap(), // file path -> diff content
    val projectPath: String,
    val repoUrl: String = "",  // Repository URL for issue tracker
    val issueToken: String = ""  // Issue tracker token (optional for public repos)
)

/**
 * Code Review Agent - Main agent for code review tasks
 *
 * Analyzes code for quality, security, performance, and best practices.
 * Can review specific files or provide general guidance.
 */
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

        val result = executor.execute(input, systemPrompt, context.linterSummary, onProgress)

        return ToolResult.AgentResult(
            success = result.success,
            content = result.message,
            metadata = mapOf(
                "reviewType" to input.reviewType.name,
                "filesReviewed" to input.filePaths.size.toString(),
                "findings" to result.findings.size.toString()
            )
        )
    }

    override suspend fun executeTask(task: ReviewTask): CodeReviewResult {
        val context = buildContext(task)
        val systemPrompt = buildSystemPrompt(context)
        return executor.execute(task, systemPrompt, context.linterSummary)
    }

    /**
     * Analyze code using Data-Driven approach (more efficient for UI scenarios)
     * This method accepts pre-collected data and performs a single-pass analysis
     * 
     * @param reviewType Type of review (COMPREHENSIVE, SECURITY, PERFORMANCE, STYLE)
     * @param filePaths List of file paths to review
     * @param codeContent Map of file paths to their content
     * @param lintResults Map of file paths to their lint results (formatted as string)
     * @param diffContext Optional diff context showing what changed
     * @param language Language for the prompt (EN or ZH)
     * @return Analysis result as markdown string
     */
    suspend fun analyzeWithDataDriven(
        reviewType: String,
        filePaths: List<String>,
        codeContent: Map<String, String>,
        lintResults: Map<String, String>,
        diffContext: String = "",
        language: String = "EN",
        onChunk: (String) -> Unit = {}
    ): String {
        logger.info { "Starting data-driven analysis for ${filePaths.size} files" }
        
        // Generate analysis prompt
        val prompt = promptRenderer.renderAnalysisPrompt(
            reviewType = reviewType,
            filePaths = filePaths,
            codeContent = codeContent,
            lintResults = lintResults,
            diffContext = diffContext,
            language = language
        )
        
        logger.info { "Generated prompt: ${prompt.length} chars (~${prompt.length / 4} tokens)" }
        
        // Stream LLM response
        val result = StringBuilder()
        llmService.streamPrompt(prompt, compileDevIns = false).collect { chunk ->
            result.append(chunk)
            onChunk(chunk)
        }
        
        logger.info { "Analysis complete: ${result.length} chars" }
        return result.toString()
    }

    /**
     * Analyze user intent from commit (Tool-driven approach)
     * This method uses tools to dynamically gather information and analyze intent
     * 
     * @param task Intent analysis task containing commit info
     * @param language Language for the prompt (EN or ZH)
     * @param onProgress Progress callback
     * @return Intent analysis result with mermaid diagram
     */
    suspend fun analyzeIntentWithTools(
        task: IntentAnalysisTask,
        language: String = "EN",
        onProgress: (String) -> Unit = {}
    ): IntentAnalysisResult {
        logger.info { "Starting intent analysis for commit: ${task.commitId}" }
        
        initializeWorkspace(task.projectPath)
        
        // Parse issue references from commit message
        val issueReferences = parseIssueReferences(task.commitMessage)
        logger.info { "Found ${issueReferences.size} issue references: $issueReferences" }
        
        // Fetch issue information if available
        val issueInfo = if (issueReferences.isNotEmpty()) {
            fetchIssueInfo(task, issueReferences)
        } else {
            emptyMap()
        }
        
        // Build context for intent analysis
        val context = buildIntentAnalysisContext(task, issueInfo)
        val systemPrompt = promptRenderer.renderIntentAnalysisPrompt(context, language)
        
        // Execute intent analysis using tool-driven approach
        val result = executeIntentAnalysis(task, systemPrompt, issueInfo, onProgress)
        
        return result
    }
    
    /**
     * Parse issue references from commit message
     * Supports formats: #123, GH-123, fixes #123, closes #456
     */
    private fun parseIssueReferences(commitMessage: String): List<String> {
        val patterns = listOf(
            Regex("#(\\d+)"),                    // #123
            Regex("GH-(\\d+)", RegexOption.IGNORE_CASE),  // GH-123
            Regex("(?:fixes|closes|resolves)\\s+#(\\d+)", RegexOption.IGNORE_CASE)  // fixes #123
        )
        
        return patterns.flatMap { pattern ->
            pattern.findAll(commitMessage).map { it.groupValues[1] }.toList()
        }.distinct()
    }
    
    /**
     * Fetch issue information from issue tracker
     */
    private suspend fun fetchIssueInfo(
        task: IntentAnalysisTask,
        issueReferences: List<String>
    ): Map<String, IssueInfo> {
        if (issueReferences.isEmpty()) {
            return emptyMap()
        }
        
        // Try to get issue tracker
        val tracker = getOrCreateIssueTracker(task)
        
        if (tracker == null || !tracker.isConfigured()) {
            logger.warn { "No issue tracker configured, using placeholder data" }
            return createPlaceholderIssues(issueReferences)
        }
        
        logger.info { "Fetching ${issueReferences.size} issues from ${tracker.getType()}" }
        val issueInfoMap = tracker.getIssues(issueReferences)
        
        logger.info { "Fetched ${issueInfoMap.size} issues successfully" }
        return issueInfoMap
    }
    
    /**
     * Get or create issue tracker based on task configuration
     */
    private fun getOrCreateIssueTracker(task: IntentAnalysisTask): IssueTracker? {
        // If issue tracker was provided in constructor, use it
        if (issueTracker != null && issueTracker.isConfigured()) {
            return issueTracker
        }
        
        // Try to create from task configuration
        if (task.repoUrl.isNotBlank()) {
            // Try GitHub first
            val githubTracker = GitHubIssueTracker.fromRepoUrl(task.repoUrl, task.issueToken)
            if (githubTracker != null) {
                logger.info { "Created GitHub issue tracker for ${task.repoUrl}" }
                return githubTracker
            }
            
            // Could add GitLab, Jira support here in the future
            logger.warn { "Could not create issue tracker from URL: ${task.repoUrl}" }
        }
        
        return null
    }
    
    /**
     * Create placeholder issue information when tracker is not available
     */
    private fun createPlaceholderIssues(issueReferences: List<String>): Map<String, IssueInfo> {
        val issueInfoMap = mutableMapOf<String, IssueInfo>()
        issueReferences.forEach { issueId ->
            issueInfoMap[issueId] = IssueInfo(
                id = issueId,
                title = "Issue #$issueId",
                description = "(Issue tracker not configured - placeholder data)",
                labels = emptyList(),
                status = "unknown"
            )
        }
        return issueInfoMap
    }
    
    /**
     * Build context for intent analysis
     */
    private fun buildIntentAnalysisContext(
        task: IntentAnalysisTask,
        issueInfo: Map<String, IssueInfo>
    ): IntentAnalysisContext {
        val allTools = toolRegistry.getAllTools()
        return IntentAnalysisContext(
            commitMessage = task.commitMessage,
            commitId = task.commitId,
            codeChanges = task.codeChanges,
            issueInfo = issueInfo,
            projectPath = task.projectPath,
            toolList = AgentToolFormatter.formatToolListForAI(allTools.values.toList())
        )
    }
    
    /**
     * Execute intent analysis with tool calling support
     */
    private suspend fun executeIntentAnalysis(
        task: IntentAnalysisTask,
        systemPrompt: String,
        issueInfo: Map<String, IssueInfo>,
        onProgress: (String) -> Unit
    ): IntentAnalysisResult {
        val conversationManager = cc.unitmesh.agent.conversation.ConversationManager(llmService, systemPrompt)
        val initialUserMessage = buildIntentAnalysisUserMessage(task, issueInfo)
        
        logger.info { "Starting intent analysis iteration loop" }
        onProgress("🔍 Analyzing commit intent...")
        
        var currentIteration = 0
        val maxIterationsForIntent = 10 // Limit iterations for intent analysis
        
        while (currentIteration < maxIterationsForIntent) {
            currentIteration++
            renderer.renderIterationHeader(currentIteration, maxIterationsForIntent)
            
            val llmResponse = StringBuilder()
            
            try {
                renderer.renderLLMResponseStart()
                
                if (enableLLMStreaming) {
                    val message = if (currentIteration == 1) initialUserMessage else buildContinuationMessage()
                    conversationManager.sendMessage(message, compileDevIns = false).collect { chunk: String ->
                        llmResponse.append(chunk)
                        renderer.renderLLMResponseChunk(chunk)
                    }
                } else {
                    val message = if (currentIteration == 1) initialUserMessage else buildContinuationMessage()
                    val response = llmService.sendPrompt(message)
                    llmResponse.append(response)
                    renderer.renderLLMResponseChunk(response)
                }
                
                renderer.renderLLMResponseEnd()
                conversationManager.addAssistantResponse(llmResponse.toString())
            } catch (e: Exception) {
                logger.error(e) { "LLM call failed: ${e.message}" }
                renderer.renderError("LLM call failed: ${e.message}")
                break
            }
            
            // Parse tool calls
            val toolCallParser = cc.unitmesh.agent.parser.ToolCallParser()
            val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
            
            if (toolCalls.isEmpty()) {
                // No more tool calls, analysis complete
                logger.info { "No tool calls found, intent analysis complete" }
                renderer.renderTaskComplete()
                break
            }
            
            // Execute tool calls
            val toolResults = executeToolCallsForIntent(toolCalls, task.projectPath)
            val toolResultsText = cc.unitmesh.agent.tool.ToolResultFormatter.formatMultipleToolResults(toolResults)
            conversationManager.addToolResults(toolResultsText)
            
            // Check if analysis is complete
            if (isIntentAnalysisComplete(llmResponse.toString())) {
                logger.info { "Intent analysis complete" }
                renderer.renderTaskComplete()
                break
            }
        }
        
        onProgress("✅ Intent analysis complete")
        
        // Extract final analysis from conversation
        val finalAnalysis = conversationManager.getHistory()
            .lastOrNull { msg -> msg.role == cc.unitmesh.devins.llm.MessageRole.ASSISTANT }
            ?.content ?: "No analysis generated"
        
        // Extract mermaid diagram if present
        val mermaidDiagram = extractMermaidDiagram(finalAnalysis)
        
        return IntentAnalysisResult(
            success = true,
            analysis = finalAnalysis,
            mermaidDiagram = mermaidDiagram,
            issuesAnalyzed = issueInfo.keys.toList(),
            implementationAccuracy = "To be evaluated", // Placeholder
            suggestedImprovements = emptyList() // Placeholder
        )
    }
    
    /**
     * Build initial user message for intent analysis
     */
    private fun buildIntentAnalysisUserMessage(
        task: IntentAnalysisTask,
        issueInfo: Map<String, IssueInfo>
    ): String {
        return buildString {
            appendLine("Please analyze the intent behind this commit and related issues:")
            appendLine()
            appendLine("## Commit Information")
            if (task.commitId.isNotBlank()) {
                appendLine("**Commit ID**: ${task.commitId}")
            }
            appendLine("**Commit Message**:")
            appendLine("```")
            appendLine(task.commitMessage)
            appendLine("```")
            appendLine()
            
            if (issueInfo.isNotEmpty()) {
                appendLine("## Related Issues")
                issueInfo.forEach { (id, info) ->
                    appendLine("### Issue #$id: ${info.title}")
                    appendLine(info.description)
                    if (info.labels.isNotEmpty()) {
                        appendLine("**Labels**: ${info.labels.joinToString(", ")}")
                    }
                    appendLine("**Status**: ${info.status}")
                    appendLine()
                }
            }
            
            if (task.codeChanges.isNotEmpty()) {
                appendLine("## Code Changes (${task.codeChanges.size} files)")
                task.codeChanges.forEach { (file, diff) ->
                    appendLine("### $file")
                    appendLine("```diff")
                    appendLine(diff)
                    appendLine("```")
                    appendLine()
                }
            }
            
            appendLine("## Your Task")
            appendLine("1. Analyze the user's intent behind this commit")
            appendLine("2. If needed, use tools to read relevant files and understand the codebase context")
            appendLine("3. Create a mermaid diagram to visualize the intent and implementation flow")
            appendLine("4. Evaluate whether the implementation accurately reflects the intent")
            appendLine("5. Identify any issues or suggest improvements")
            appendLine()
            appendLine("Please use the available tools to gather additional context as needed.")
        }
    }
    
    private fun buildContinuationMessage(): String {
        return "Please continue the analysis based on the tool execution results. Use additional tools if needed, or provide your final analysis if you have all the information."
    }
    
    /**
     * Execute tool calls for intent analysis
     */
    private suspend fun executeToolCallsForIntent(
        toolCalls: List<cc.unitmesh.agent.state.ToolCall>,
        projectPath: String
    ): List<Triple<String, Map<String, Any>, cc.unitmesh.agent.orchestrator.ToolExecutionResult>> {
        val results = mutableListOf<Triple<String, Map<String, Any>, cc.unitmesh.agent.orchestrator.ToolExecutionResult>>()
        
        for (toolCall in toolCalls) {
            val toolName = toolCall.toolName
            val params = toolCall.params.mapValues { it.value as Any }
            
            try {
                logger.info { "Executing tool: $toolName" }
                renderer.renderToolCall(toolName, params.toString())
                
                val context = cc.unitmesh.agent.orchestrator.ToolExecutionContext(
                    workingDirectory = projectPath,
                    environment = emptyMap()
                )
                
                val executionResult = toolOrchestrator.executeToolCall(toolName, params, context)
                results.add(Triple(toolName, params, executionResult))
                
                renderer.renderToolResult(
                    toolName, 
                    executionResult.isSuccess, 
                    executionResult.content,
                    executionResult.content
                )
            } catch (e: Exception) {
                logger.error(e) { "Tool execution failed: $toolName" }
                renderer.renderToolResult(toolName, false, null, "Error: ${e.message}")
            }
        }
        
        return results
    }
    
    /**
     * Check if intent analysis is complete
     */
    private fun isIntentAnalysisComplete(response: String): Boolean {
        val completionIndicators = listOf(
            "analysis complete",
            "intent analysis complete",
            "mermaid diagram",
            "## final analysis",
            "## summary"
        )
        
        val lowerResponse = response.lowercase()
        return completionIndicators.any { lowerResponse.contains(it) } &&
                response.contains("```mermaid", ignoreCase = true)
    }
    
    /**
     * Extract mermaid diagram from analysis
     */
    private fun extractMermaidDiagram(analysis: String): String? {
        // Use MULTILINE to match across lines
        val mermaidPattern = Regex("```mermaid\\s*\\n([\\s\\S]+?)\\n```")
        val matchResult = mermaidPattern.find(analysis)
        return matchResult?.groupValues?.getOrNull(1)?.trim()
    }

    override fun buildSystemPrompt(context: CodeReviewContext, language: String): String {
        return promptRenderer.render(context, language)
    }

    private suspend fun initializeWorkspace(projectPath: String) {
    }

    private suspend fun buildContext(task: ReviewTask): CodeReviewContext {
        val linterSummary = if (task.filePaths.isNotEmpty()) {
            try {
                val linterRegistry = cc.unitmesh.agent.linter.LinterRegistry.getInstance()
                linterRegistry.getLinterSummaryForFiles(task.filePaths)
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

/**
 * Service interface for CodeReviewAgent
 */
interface CodeReviewService {
    suspend fun executeTask(task: ReviewTask): CodeReviewResult
    fun buildSystemPrompt(context: CodeReviewContext, language: String = "EN"): String
}

/**
 * Result of code review
 */
data class CodeReviewResult(
    val success: Boolean,
    val message: String,
    val findings: List<ReviewFinding> = emptyList()
)

/**
 * A single finding from code review
 */
data class ReviewFinding(
    val severity: Severity,
    val category: String,
    val description: String,
    val filePath: String? = null,
    val lineNumber: Int? = null,
    val suggestion: String? = null
)

enum class Severity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

/**
 * Context for intent analysis
 */
data class IntentAnalysisContext(
    val commitMessage: String,
    val commitId: String,
    val codeChanges: Map<String, String>,
    val issueInfo: Map<String, IssueInfo>,
    val projectPath: String,
    val toolList: String
) : AgentContext {
    override fun toVariableTable(): cc.unitmesh.devins.compiler.variable.VariableTable {
        val table = cc.unitmesh.devins.compiler.variable.VariableTable()
        table.addVariable("toolList", cc.unitmesh.devins.compiler.variable.VariableType.STRING, toolList)
        table.addVariable("commitMessage", cc.unitmesh.devins.compiler.variable.VariableType.STRING, commitMessage)
        table.addVariable("commitId", cc.unitmesh.devins.compiler.variable.VariableType.STRING, commitId)
        return table
    }
}

/**
 * Result of intent analysis
 */
data class IntentAnalysisResult(
    val success: Boolean,
    val analysis: String,
    val mermaidDiagram: String?,
    val issuesAnalyzed: List<String>,
    val implementationAccuracy: String,
    val suggestedImprovements: List<String>
)
