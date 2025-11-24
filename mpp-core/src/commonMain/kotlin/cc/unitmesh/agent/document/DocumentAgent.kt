package cc.unitmesh.agent.document

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.executor.DocumentAgentExecutor
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.impl.DocQLTool
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.schema.AgentToolFormatter
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.devins.document.DocumentParserService
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Document Task - represents a user query about a document
 */
data class DocumentTask(
    val query: String,
    val documentPath: String? = null
)

/**
 * Document Agent - handles document queries using HeadingQL and ChapterQL
 * Extends MainAgent for consistency with the agent framework
 */
class DocumentAgent(
    private val llmService: KoogLLMService,
    private val parserService: DocumentParserService,
    private val renderer: CodingAgentRenderer,
    private val fileSystem: ToolFileSystem? = null,
    private val shellExecutor: ShellExecutor? = null,
    private val mcpToolConfigService: McpToolConfigService,
    private val enableLLMStreaming: Boolean = true
) : MainAgent<DocumentTask, ToolResult.AgentResult>(
    AgentDefinition(
        name = "DocumentAgent",
        displayName = "Document Query Agent",
        description = "Agent for answering questions about documents using DocQL",
        promptConfig = PromptConfig(
            systemPrompt = "You are a helpful document assistant with advanced query capabilities.",
            queryTemplate = null,
            initialMessages = emptyList()
        ),
        modelConfig = cc.unitmesh.llm.ModelConfig.default(),
        runConfig = RunConfig(
            maxTurns = 10,
            maxTimeMinutes = 5,
            terminateOnError = false
        )
    )
) {
    private val actualFileSystem = fileSystem ?: DefaultToolFileSystem(projectPath = ".")

    private val toolRegistry = run {
        ToolRegistry(
            fileSystem = actualFileSystem,
            shellExecutor = shellExecutor ?: DefaultShellExecutor(),
            configService = mcpToolConfigService,
            subAgentManager = cc.unitmesh.agent.core.SubAgentManager(),
            llmService = llmService
        ).apply {
            // Register DocQLTool
            registerTool(DocQLTool())
        }
    }

    private val policyEngine = DefaultPolicyEngine()

    private val toolOrchestrator = ToolOrchestrator(
        registry = toolRegistry,
        policyEngine = policyEngine,
        renderer = renderer,
        mcpConfigService = mcpToolConfigService
    )

    private val executor = DocumentAgentExecutor(
        projectPath = ".",
        llmService = llmService,
        toolOrchestrator = toolOrchestrator,
        renderer = renderer,
        maxIterations = maxIterations,
        enableLLMStreaming = enableLLMStreaming
    )

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            // Initialize any necessary resources
        }
    }

    /**
     * Validate and parse input parameters
     */
    override fun validateInput(input: Map<String, Any>): DocumentTask {
        val query = input["query"] as? String
            ?: throw IllegalArgumentException("Missing required parameter 'query'")
        val documentPath = input["documentPath"] as? String

        return DocumentTask(query = query, documentPath = documentPath)
    }

    /**
     * Format agent result as string
     */
    override fun formatOutput(output: ToolResult.AgentResult): String {
        return if (output.success) {
            output.content
        } else {
            "Error: ${output.content}"
        }
    }

    override suspend fun execute(
        input: DocumentTask,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        val context = buildContext(input)
        val systemPrompt = buildSystemPrompt(context)

        val result = executor.execute(
            input,
            systemPrompt,
            onProgress
        )
        
        return ToolResult.AgentResult(
            success = result.success,
            content = result.content,
            metadata = result.metadata.mapValues { it.value.toString() }
        )
    }

    private fun buildContext(task: DocumentTask): DocumentContext {
        return DocumentContext(
            query = task.query,
            documentPath = task.documentPath
        )
    }

    private fun buildSystemPrompt(context: DocumentContext): String {
        return """
            You are a helpful document assistant with advanced query capabilities.
            You can query the document using DocQL (Document Query Language) via the `docql` tool.
            
            User Query: ${context.query}
            Document Path: ${context.documentPath ?: "Not specified"}
            
            Available Tools:
            ${AgentToolFormatter.formatToolListForAI(toolRegistry.getAllTools().values.toList())}
            
            ## Tool Usage Format
            
            All tools use the DevIns format with JSON parameters:
            <devin>
            /tool-name
            ```json
            {"parameter": "value"}
            ```
            </devin>
            
            **IMPORTANT: Execute ONE tool at a time**
            - ✅ Correct: One <devin> block with one tool call per response
            - ❌ Wrong: Multiple <devin> blocks or multiple tools in one response
            
            ## Response Format
            
            When you need to query the document:
            1. Explain what you're looking for
            2. Use EXACTLY ONE tool call wrapped in <devin></devin> tags
            3. Wait for the tool result
            
            After gathering the information, provide your final answer WITHOUT any tool calls.
            
            ## DocQL Syntax Examples
            
            **Table of Contents:**
            - `$.toc[*]` - Get all Table of Contents items (recommended as first step)
            
            **Content Queries:**
            - `$.content.chunks()` - Get all document content (use when you need full context)
            - `$.content.heading("Design")` - Get sections with "Design" in title (partial match)
            - `$.content.h1("Overview")` - Get level 1 headings matching "Overview"
            - `$.content.chapter("chapter-id")` - Get specific chapter by ID
            
            **Entities:**
            - `$.entities[?(@.type=="API")]` - Get all API entities
            
            **Best Practices:**
            1. Start with `$.toc[*]` to understand document structure
            2. Use `$.content.heading("keyword")` for specific sections (supports partial match)
            3. Use `$.content.chunks()` if you need all content or if heading queries return empty
            4. Document paths in docql tool are already registered - just use the relative path shown
            5. For read-file tool, use full path from project root (e.g., "docs/design-system/design-system-color.md")
            
            Always use the `docql` tool to retrieve information. Do not guess.
        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
