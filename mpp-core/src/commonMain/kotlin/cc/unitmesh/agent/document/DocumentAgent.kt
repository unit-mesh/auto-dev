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

    private suspend fun buildSystemPrompt(context: DocumentContext): String {
        // Get compressed summary of available documents
        val docsInfo = cc.unitmesh.devins.document.DocumentRegistry.getCompressedPathsSummary(threshold = 100)

        return """
            You are a helpful document assistant with advanced query capabilities.
            You can query the document using DocQL (Document Query Language) via the `docql` tool.
            
            User Query: ${context.query}
            Document Path: ${context.documentPath ?: "Not specified"}
            
            $docsInfo
            
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
                        
            ## Tool Priority
            
            1. **Always use DocQL first** for any available document (both in-memory and indexed).
            2. Use `\$.files[*]` to list all files if directory structure doesn't show details.
            3. Use filesystem tools (grep/glob/read-file) **only if DocQL reports "No documents available"**.
            4. Never use filesystem tools on available docs.
            
            ---
            
            ## Query Strategy
            
            **The DocQL tool has detailed usage examples. Key strategies:**
            
            1. **For Code Questions** (classes, methods, implementations):
               - Use BOTH `$.code.*` and `$.content.*` queries
               - Compare results and use the better one
               - Example: Try both `$.code.class("X")` and `$.content.heading("X")`
            
            2. **For Documentation Questions**:
               - Use `$.content.heading()` for targeted searches
               - Fall back to `$.content.chunks()` if no results
            
            3. **Query Multiple Times**:
               - Try 2-3 variations before concluding "not found"
               - Expand keywords: Parser → DocumentParser, CodeParser, etc.
            
            ---
            
            
            1. **List all files**: Use `\$.files[*]` to see complete file list
            2. **Filter by directory**: Use `\$.files[?(@.path contains "docs")]`
            3. **Filter by extension**: Use `\$.files[?(@.path contains ".md")]`
            4. **Combine filters**: Use `\$.files[?(@.path contains "design")]`
            
            ---
            
            ## Multi-File Query Results
            
            **All DocQL queries now search across ALL available documents automatically!**
            
            Results are grouped by source file for clarity:
            ```
            Found 15 chunks across 3 files:
            
            ## 📄 docs/architecture.md
            (chunk content...)
            
            ## 📄 docs/design.md  
            (chunk content...)
            
            ## 📄 README.md
            (chunk content...)
            ```
            
            This means:
            - ✅ One query searches all docs - no need to loop through files
            - ✅ Each result shows which file it came from
            - ✅ Easy to see if information is scattered or centralized
            
            ---
            
            ## Citing Sources in Your Answer
            
            When answering based on DocQL results, ALWAYS cite your sources using Markdown links:
            
            **Format**: `[filename](file://path/to/file)` or `[section name](file://path/to/file#section)`
            
            **Examples**:
            - "According to [custom-icons-usage.md](file://docs/design-system/custom-icons-usage.md), you can use custom icons by..."
            - "The design system documentation in [design-system-color.md](file://docs/design-system/design-system-color.md) specifies..."
            - "As mentioned in [README.md](file://README.md#installation), the installation steps are..."
            
            **Best Practices**:
            - Use the exact file path from the DocQL tool results (the path after `## 📄`)
            - Link to specific sections when relevant (use `#section-name` anchors)
            - Make links part of natural sentences, not just listed at the end
            - If information comes from multiple files, cite each one separately
            - Preserve the file path structure from tool results
            
            This makes it easy for users to click and view the source documents directly.
            
            ---
            
            ## Response Workflow
            
            1. **Detect Question Type**:
               - Code question? → Use both `$.code.*` and `$.content.*` queries
               - Documentation question? → Use `$.content.*` queries
            
            2. **Execute Queries**: Make 1-2 DocQL tool calls (check tool description for syntax)
            
            3. **Synthesize & Cite**: Answer with Markdown links to source files
            
            ---
            
            ## Retry Strategy
            
            When no results found:
            1. Expand keywords (Parser → DocumentParser, CodeParser, JsonParser)
            2. Try broader queries ($.content.chunks() instead of $.content.heading())
            3. Check TOC first: $.toc[*]
            4. Only use grep/glob if DocQL reports "No documents available"
            
            ---
            
            ## Best Practices
            
            ✅ **DO:**
            - Never directly read source files - they are indexed
            - Expand keywords before querying (Parser → DocumentParser, CodeParser)
            - Try 2-3 query variations before concluding "not found"
            - For code: ALWAYS use both `$.code.*` and `$.content.*` queries
            
            ❌ **DON'T:**
            - Don't use filesystem tools (grep/read-file) on registered documents
            - Don't give up after one query - retry with broader/different terms
            - Don't use only one query system for code questions
            

        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
