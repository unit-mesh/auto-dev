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
    private val enableLLMStreaming: Boolean = true
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
