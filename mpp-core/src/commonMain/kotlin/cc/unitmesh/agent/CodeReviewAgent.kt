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
import kotlinx.serialization.json.Json

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
 * Unified analysis task
 * Supports both data-driven (pre-collected data) and tool-driven (dynamic tool usage) approaches
 */
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
        logger.info { "Using unified analysis approach" }

        initializeWorkspace(task.projectPath)
        val issueInfo = if (task.analyzeIntent && task.commitMessage.isNotBlank()) {
            val issueRefs = parseIssueReferences(task.commitMessage)
            if (issueRefs.isNotEmpty()) {
                fetchIssueInfo(task, issueRefs)
            } else {
                emptyMap()
            }
        } else {
            emptyMap()
        }

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

        val systemPrompt = "You are a code review assistant. Analyze the code and provide feedback."
        val conversationManager = cc.unitmesh.agent.conversation.ConversationManager(llmService, systemPrompt)
        val initialMessage = buildToolDrivenMessage(task, issueInfo, linterSummary)
        
        var currentIteration = 0
        val maxIter = if (task.analyzeIntent) 10 else maxIterations
        
        while (currentIteration < maxIter) {
            currentIteration++
            
            val llmResponse = StringBuilder()
            
            try {
                val message = if (currentIteration == 1) initialMessage else buildContinuationMessage()
                conversationManager.sendMessage(message, compileDevIns = false).collect { chunk: String ->
                    llmResponse.append(chunk)
                    onProgress(chunk)
                }
                
                conversationManager.addAssistantResponse(llmResponse.toString())
            } catch (e: Exception) {
                logger.error(e) { "LLM call failed: ${e.message}" }
                onProgress("❌ LLM call failed: ${e.message}\n")
                break
            }
            
            val toolCallParser = cc.unitmesh.agent.parser.ToolCallParser()
            val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
            
            if (toolCalls.isEmpty()) {
                logger.info { "No tool calls found, analysis complete" }
                break
            }
            
            val toolResults = executeToolCallsForIntent(toolCalls, task.projectPath)
            val toolResultsText = cc.unitmesh.agent.tool.ToolResultFormatter.formatMultipleToolResults(toolResults)
            conversationManager.addToolResults(toolResultsText)
            
            if (isAnalysisComplete(llmResponse.toString(), task.analyzeIntent)) {
                logger.info { "Analysis complete" }
                break
            }
        }
        
        val finalAnalysis = conversationManager.getHistory()
            .lastOrNull { msg -> msg.role == cc.unitmesh.devins.llm.MessageRole.ASSISTANT }
            ?.content ?: "No analysis generated"
        
        return AnalysisResult(
            success = true,
            content = finalAnalysis,
            mermaidDiagram = if (task.analyzeIntent) extractMermaidDiagram(finalAnalysis) else null,
            issuesAnalyzed = issueInfo.keys.toList(),
            usedTools = true
        )
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
     * Build message for tool-driven analysis
     */
    private suspend fun buildToolDrivenMessage(
        task: AnalysisTask,
        issueInfo: Map<String, IssueInfo>,
        linterSummary: cc.unitmesh.agent.linter.LinterSummary?
    ): String {
        return if (task.analyzeIntent) {
            buildIntentAnalysisUserMessage(task, issueInfo)
        } else {
            buildString {
                appendLine("Please review the following code:")
                appendLine()
                appendLine("**Project Path**: ${task.projectPath}")
                appendLine("**Review Type**: ${task.reviewType}")
                appendLine()

                if (task.filePaths.isNotEmpty()) {
                    appendLine("**Files to review** (${task.filePaths.size} files):")
                    task.filePaths.forEach { appendLine("  - $it") }
                    appendLine()
                }

                // Add linter information
                if (linterSummary != null) {
                    appendLine("## Linter Information")
                    appendLine()
                    appendLine(formatLinterInfo(linterSummary))
                    appendLine()
                }

                if (task.diffContext.isNotBlank()) {
                    appendLine("**Diff Context**:")
                    appendLine(task.diffContext)
                    appendLine()
                }

                appendLine("**Instructions**:")
                appendLine("1. First, analyze the linter results above (if provided)")
                appendLine("2. Use tools to read and analyze the code")
                appendLine("3. Provide thorough code review combining:")
                appendLine("   - Technical issues (security, performance, bugs)")
                appendLine("   - Business logic concerns")
                appendLine("   - Suggestions beyond what linters can detect")
                appendLine("4. Focus on actionable improvements")
            }
        }
    }

    /**
     * Format linter information for display in user messages
     */
    private fun formatLinterInfo(linterSummary: cc.unitmesh.agent.linter.LinterSummary): String {
        return buildString {
            if (linterSummary.availableLinters.isNotEmpty()) {
                appendLine("**Available Linters (${linterSummary.availableLinters.size}):**")
                linterSummary.availableLinters.forEach { linter ->
                    appendLine("- **${linter.name}** ${linter.version?.let { "($it)" } ?: ""}")
                    if (linter.supportedFiles.isNotEmpty()) {
                        appendLine("  - Supported files: ${linter.supportedFiles.joinToString(", ")}")
                    }
                }
                appendLine()
            }

            if (linterSummary.unavailableLinters.isNotEmpty()) {
                appendLine("**Unavailable Linters (${linterSummary.unavailableLinters.size}):**")
                linterSummary.unavailableLinters.forEach { linter ->
                    appendLine("- **${linter.name}** (not installed)")
                    linter.installationInstructions?.let {
                        appendLine("  - Install: $it")
                    }
                }
                appendLine()
            }

            if (linterSummary.fileMapping.isNotEmpty()) {
                appendLine("**File-Linter Mapping:**")
                linterSummary.fileMapping.forEach { (file, linters) ->
                    appendLine("- `$file` → ${linters.joinToString(", ")}")
                }
            }
        }
    }
    
    /**
     * Build initial user message for intent analysis
     */
    private fun buildIntentAnalysisUserMessage(
        task: AnalysisTask,
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
            
            val changes = task.codeChanges.ifEmpty { task.codeContent }
            if (changes.isNotEmpty()) {
                appendLine("## Code Changes (${changes.size} files)")
                changes.forEach { (file, content) ->
                    appendLine("### $file")
                    appendLine("```diff")
                    appendLine(content)
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
    
    /**
     * Fetch issue information from issue tracker
     */
    private suspend fun fetchIssueInfo(
        task: AnalysisTask,
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
    private fun getOrCreateIssueTracker(task: AnalysisTask): IssueTracker? {
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
        task: AnalysisTask,
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
     * Check if analysis is complete
     */
    private fun isAnalysisComplete(response: String, isIntentAnalysis: Boolean): Boolean {
        val completionIndicators = if (isIntentAnalysis) {
            listOf(
                "analysis complete",
                "intent analysis complete",
                "mermaid diagram",
                "## final analysis",
                "## summary"
            )
        } else {
            listOf(
                "review complete",
                "review is complete",
                "finished reviewing",
                "completed the review",
                "final review",
                "summary:",
                "## summary"
            )
        }
        
        val lowerResponse = response.lowercase()
        val hasIndicator = completionIndicators.any { lowerResponse.contains(it) }
        
        return if (isIntentAnalysis) {
            hasIndicator && response.contains("```mermaid", ignoreCase = true)
        } else {
            hasIndicator
        }
    }

    private fun extractMermaidDiagram(analysis: String): String? {
        val mermaidPattern = Regex("```mermaid\\s*\\n([\\s\\S]+?)\\n```")
        val matchResult = mermaidPattern.find(analysis)
        return matchResult?.groupValues?.getOrNull(1)?.trim()
    }

    override fun buildSystemPrompt(context: CodeReviewContext, language: String): String {
        return "You are a code review assistant. Analyze the code and provide feedback."
    }

    private fun initializeWorkspace(projectPath: String) {
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
 * Unified analysis result
 */
data class AnalysisResult(
    val success: Boolean,
    val content: String,
    val mermaidDiagram: String? = null,
    val issuesAnalyzed: List<String> = emptyList(),
    val usedTools: Boolean = false
)

data class LintFileResult(
    val filePath: String,
    val linterName: String,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val issues: List<LintIssueUI>
)

/**
 * UI-friendly lint issue
 */
data class LintIssueUI(
    val line: Int,
    val column: Int,
    val severity: LintSeverityUI,
    val message: String,
    val rule: String? = null,
    val suggestion: String? = null
)

/**
 * UI-friendly lint severity
 */
enum class LintSeverityUI {
    ERROR,
    WARNING,
    INFO
}
