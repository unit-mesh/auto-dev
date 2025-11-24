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
            
            ## Tool Selection Priority
            
            **CRITICAL: Always prioritize DocQL for registered documents**
            
            1. **FIRST CHOICE - DocQL Tool** (`docql`):
               - Use for ALL queries about registered document content
               - Documents are already parsed and indexed
               - Fast and accurate for structured queries
            
            2. **LAST RESORT - File System Tools** (`grep`, `glob`, `read-file`):
               - ONLY use if DocQL returns "No documents registered"
               - ONLY use for files NOT in the document registry
               - Never use these for registered documents
            
            ## Response Format
            
            When you need to query the document:
            1. Explain what you're looking for
            2. Use EXACTLY ONE tool call wrapped in <devin></devin> tags
            3. Wait for the tool result
            
            After gathering the information, provide your final answer WITHOUT any tool calls.
            
            ## DocQL Retry Strategy
            
            **When a DocQL query returns empty results, try progressively broader queries:**
            
            ### Progressive Query Approach:
            
            1. **Start Specific** - Try targeted queries first:
               ```
               $.content.heading("exact keyword")
               $.content.h1("specific title")
               ```
            
            2. **If Empty → Go Broader** - Try partial matches or related terms:
               ```
               $.content.heading("partial")  // partial match on any heading
               $.toc[*]  // see all available sections
               ```
            
            3. **If Still Empty → Get All Content**:
               ```
               $.content.chunks()  // retrieve all document content
               ```
            
            4. **Only After All DocQL Attempts** - If truly no documents registered:
               - Then and ONLY then consider grep/glob for non-registered files
            
            ### Example Retry Flow:
            
            **User asks: "What are the color principles?"**
            
            ✅ **Correct Approach:**
            - Try 1: `$.content.heading("color principle")` → Empty
            - Try 2: `$.content.heading("color")` → Empty  
            - Try 3: `$.toc[*]` → See available sections
            - Try 4: `$.content.heading("design")` → Found content!
            - OR Try 4: `$.content.chunks()` → Get all content and search manually
            
            ❌ **Wrong Approach:**
            - Try 1: `$.content.heading("color principle")` → Empty
            - Try 2: Use `grep` to search files ← WRONG! Try more DocQL queries first!
            
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
            
            ## Handling Empty Results
            
            When DocQL returns "No results found" or empty content:
            
            1. **Don't give up immediately** - This is normal for specific queries
            2. **Try a broader query** - Use the progressive approach above
            3. **Check TOC first** - `$.toc[*]` shows what's actually available
            4. **Use chunks() as fallback** - `$.content.chunks()` gets everything
            5. **Explain your strategy** - Tell the user what you're trying
            
            **Remember:** Documents are already registered and parsed. The content IS there.
            You just need to find the right query. Be persistent with DocQL!
            
            ## Best Practices
            
            1. **Start with TOC** - `$.toc[*]` to understand document structure
            2. **Use heading() for sections** - `$.content.heading("keyword")` supports partial match
            3. **Try variations** - If "color psychology" fails, try just "color" or "psychology"
            4. **Use chunks() when stuck** - `$.content.chunks()` gets all content
            5. **Document paths** - Already registered, just use relative path if needed
            6. **Never guess** - Always use tools to retrieve information
            
            Always use the `docql` tool to retrieve information. Do not guess.
        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
