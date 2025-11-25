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
import cc.unitmesh.agent.tool.schema.AgentToolFormatter
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tracker.IssueTracker
import cc.unitmesh.agent.util.WalkthroughExtractor
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
    val additionalContext: String = "",
    val patch: String? = null,
    val lintResults: List<cc.unitmesh.agent.linter.LintFileResult>? = null
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

    suspend fun generateFixes(
        patch: String,
        lintResults: List<cc.unitmesh.agent.linter.LintFileResult>,
        analysisOutput: String,
        userFeedback: String = "",
        language: String = "EN",
        renderer: CodingAgentRenderer? = null,
        onProgress: (String) -> Unit = {}
    ): AnalysisResult {
        logger.info { "Starting fix generation using CodingAgent" }
        
        val actualRenderer = renderer ?: this.renderer

        try {
            if (patch.isBlank()) {
                logger.warn { "Empty patch provided for fix generation" }
                // Align error message with test expectation (looks for 'No code changes')
                val msg = "No code changes found in patch."
                actualRenderer.renderError(msg)
                return AnalysisResult(
                    success = false,
                    content = msg,
                    usedTools = false
                )
            }

            // 2. Extract changed code hunks from the patch
            val extractor = cc.unitmesh.agent.vcs.context.ChangedCodeExtractor()
            val changedHunks = extractor.extractChangedHunks(patch, contextLines = 3)
            
            if (changedHunks.isEmpty()) {
                logger.warn { "No changed code hunks extracted from patch" }
                return AnalysisResult(
                    success = false,
                    content = "No code changes found in patch.",
                    usedTools = false
                )
            }
            
            logger.info { "Extracted changes from ${changedHunks.size} files, ${changedHunks.values.sumOf { it.size }} total hunks" }
            
            // 3. Filter lint results to only include files that were actually changed
            val relevantFiles = changedHunks.keys
            val filteredLintResults = lintResults.filter { it.filePath in relevantFiles }
            
            logger.info { "Filtered lint results: ${lintResults.size} -> ${filteredLintResults.size} (only changed files)" }
            
            // 4. Build requirement string for CodingAgent (inline to simplify)
            val isZh = language.uppercase() in listOf("ZH", "CN")
            val requirement = buildString {
                if (isZh) {
                    appendLine("# 代码修复任务")
                    appendLine()
                    appendLine("基于代码审查分析结果，修复以下代码问题。")
                } else {
                    appendLine("# Code Fix Task")
                    appendLine()
                    appendLine("Based on the code review analysis, fix the following code issues.")
                }
                appendLine()

                // Extract walkthrough content from analysis output
                if (analysisOutput.isNotBlank()) {
                    val walkthroughContent = WalkthroughExtractor.extract(analysisOutput)
                    if (walkthroughContent.isNotBlank()) {
                        appendLine(walkthroughContent)
                        appendLine()
                    }
                }

                if (filteredLintResults.isNotEmpty()) {
                    val filesWithErrors = filteredLintResults.filter { it.errorCount > 0 }
                    if (filesWithErrors.isNotEmpty()) {
                        if (isZh) {
                            appendLine("## 🚨 关键优先级 - 有错误的文件（必须优先修复）")
                        } else {
                            appendLine("## 🚨 CRITICAL PRIORITY - Files with Errors (MUST FIX FIRST)")
                        }
                        appendLine()

                        filesWithErrors.forEach { fileResult ->
                            appendLine("### ❌ ${fileResult.filePath}")
                            if (isZh) {
                                appendLine("**优先级: 关键** - ${fileResult.errorCount} 个错误, ${fileResult.warningCount} 个警告")
                            } else {
                                appendLine("**Priority: CRITICAL** - ${fileResult.errorCount} error(s), ${fileResult.warningCount} warning(s)")
                            }
                            appendLine()

                            val errors = fileResult.issues.filter { it.severity == cc.unitmesh.agent.linter.LintSeverity.ERROR }
                            if (errors.isNotEmpty()) {
                                if (isZh) {
                                    appendLine("**🔴 错误（必须修复）:**")
                                } else {
                                    appendLine("**🔴 ERRORS (Fix Required):**")
                                }
                                errors.take(5).forEach { issue ->
                                    appendLine("- Line ${issue.line}: ${issue.message}")
                                }
                                appendLine()
                            }
                        }
                    }
                }

                if (userFeedback.isNotBlank()) {
                    appendLine(userFeedback)
                    appendLine()
                }
            }

            logger.debug { "Fix requirement size: ${requirement.length} chars" }

            // 5. Create CodingAgent instance
            val codingAgent = CodingAgent(
                projectPath = projectPath,
                llmService = llmService,
                maxIterations = 50,
                renderer = actualRenderer,
                fileSystem = actualFileSystem,
                shellExecutor = shellExecutor ?: DefaultShellExecutor(),
                mcpServers = null,
                mcpToolConfigService = mcpToolConfigService,
                enableLLMStreaming = enableLLMStreaming
            )

            // 6. Execute fix task using CodingAgent
            onProgress("🚀 Starting code fix generation using CodingAgent...\n")
            
            val agentTask = AgentTask(
                requirement = requirement,
                projectPath = projectPath
            )

            val agentResult = codingAgent.execute(agentTask) { progress ->
                onProgress(progress)
            }

            logger.info { "Fix generation completed - success: ${agentResult.success}" }

            return AnalysisResult(
                success = agentResult.success,
                content = agentResult.content,
                usedTools = true,
                issuesAnalyzed = filteredLintResults.map { it.filePath }
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate fixes: ${e.message}" }
            actualRenderer.renderError("Error generating fixes: ${e.message}")
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
        val linterSummary = if (task.lintResults != null) {
            // Use provided lint results (already filtered)
            logger.info { "Using provided lint results: ${task.lintResults.size} files" }
            val fileIssues = task.lintResults.map { result ->
                cc.unitmesh.agent.linter.FileLintSummary(
                    filePath = result.filePath,
                    linterName = result.linterName,
                    totalIssues = result.issues.size,
                    errorCount = result.errorCount,
                    warningCount = result.warningCount,
                    infoCount = result.infoCount,
                    topIssues = result.issues.take(5),
                    hasMoreIssues = result.issues.size > 5
                )
            }
            
            val totalIssues = fileIssues.sumOf { it.totalIssues }
            val errorCount = fileIssues.sumOf { it.errorCount }
            val warningCount = fileIssues.sumOf { it.warningCount }
            val infoCount = fileIssues.sumOf { it.infoCount }
            
            LinterSummary(
                totalFiles = task.filePaths.size,
                filesWithIssues = fileIssues.size,
                totalIssues = totalIssues,
                errorCount = errorCount,
                warningCount = warningCount,
                infoCount = infoCount,
                fileIssues = fileIssues,
                executedLinters = fileIssues.map { it.linterName }.distinct()
            )
        } else if (task.filePaths.isNotEmpty()) {
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
)

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
