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
        val docsInfo = cc.unitmesh.devins.document.DocumentRegistry.getCompressedPathsSummary(threshold = 20)

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
            
            ## Querying Source Code Files
            
            **Source code files (.kt, .java, .py, .js, .ts, etc.) are indexed with hierarchical structure:**
            
            ### Code Structure
            - 📦 **Packages/Modules** → 📘 **Classes/Interfaces** → ⚡ **Methods/Functions** → 📌 **Fields/Properties**
            - Each code element has complete source code preserved (method-level)
            - TOC reflects the nested structure of code
            
            ### Querying Code
            
            1. **Find Classes**:
               - `$.content.heading("ClassName")` - Find classes by name
               - `$.entities[?(@.type == "ClassEntity")]` - All classes
               
            2. **Find Methods/Functions**:
               - `$.content.heading("methodName")` - Find by method name
               - `$.entities[?(@.type == "FunctionEntity")]` - All functions
               
            3. **Find Implementation**:
               - Query by class name first to get context
               - Then query by method name to see implementation
               - Method bodies are preserved with full code
            
            4. **Understand Code Flow**:
               - Start with class structure: `$.toc[*]` shows hierarchy
               - Then query specific methods for details
               - Use package names to narrow down scope
            
            ### Examples for Code Queries
            
            **Q: "What is DocQLExecutor and how does it work?"**
            ```json
            {"query": "$.content.heading(\"DocQLExecutor\")", "documentPath": null}
            ```
            ✅ Will find the class and show its methods with implementations
            
            **Q: "How does the execute method work in DocQLExecutor?"**
            ```json
            {"query": "$.content.heading(\"execute\")", "documentPath": null}
            ```
            ✅ Will find all execute methods and show their code
            
            **Q: "Find all parse methods"**
            ```json
            {"query": "$.content.heading(\"parse\")", "documentPath": null}
            ```
            ✅ Will find parseDocument, parseMarkdown, etc. with implementations
            
            ---
            
            ## Response Workflow
            
            1. **Plan**: Analyze the query and identify target documents from filename patterns
            2. **Query**: Make **exactly one** DocQL call with appropriate query and documentPath
            3. **Respond**: After tool results, synthesize answer naturally (no more tool use)
            
            ---
            
            ## Querying Available Documents
            
            When document list shows compressed directory structure:
            
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
            
            ## DocQL Retry Strategy
            
            When results are empty:
            
            1. **Specific → Broader → Full**
            
               * Start: `$.content.heading("keyword")`, `$.content.h1("title")`
               * Broaden: partial matches, synonyms, translations
               * Check TOC: `$.toc[*]`
               * Full fallback: `$.content.chunks()`
            
            2. Only if no documents are registered → use grep/glob.
            
            ---
            
            ## Keyword Expansion (before querying)
            
            * **Morphology**: encode → encoding/encoded; design → designed/designer
            * **Synonyms**: color → colour/hue/palette; document → doc/file
            * **Multi-language**: 颜色→color; 设计→design; 架构→architecture
            * **Compounds**: base64→base-64; API→api/interface
            
            ### Code-Specific Expansions
            * **Naming Patterns**: Parser → DocumentParser/CodeParser/JsonParser
            * **Method Patterns**: get/set/is/has, create/build/make, parse/read/load
            * **Class Types**: Service/Manager/Handler/Controller/Repository/Factory
            * **Interfaces**: Interface/Impl/Abstract patterns
            
            ---
            
            ## Smart Document Selection
            
            1. Check filenames.
            2. Expand keywords to match variants.
            3. If filename matches → set `documentPath`.
            4. If multiple matches → query each then synthesize.
            5. If no clear match → query all docs via TOC.
            
            ---
            
            ## StepChain GraphRAG Fallback (when all normal search fails)
            
            If filenames, TOC, headings, and chunks yield no direction:
            
            1. **Decompose** the question into small hops.
            2. **Iteratively retrieve**: each hop’s result drives the next query.
            3. **Maintain a short reasoning chain** (step → result → next-step).
            4. Stop when a relevant document emerges → synthesize.
            
            Use StepChain only as a last resort; never guess.
            
            ---
            
            ## Best Practices
            
            ✅ **DO:**
            - Start with `heading()` for targeted searches, fall back to `chunks()` if empty
            - Every document maybe big, Never directly view file. 
            - Expand keywords BEFORE first query: synonyms, morphology, translations
            - Try 2-3 different queries before concluding "no information found"
            - Query multiple related documents for cross-cutting topics
            
            ✅ **FOR CODE QUERIES:**
            - Identify if query is about code (class, method, implementation, "how it works")
            - Look for source file extensions (.kt, .java, .py, .js, .ts, .go, .rs, .cs)
            - Start with class/interface name to understand structure
            - Then query specific methods for implementation details
            - Method bodies contain full code with comments
            - Use package structure to narrow search scope
            
            ❌ **DON'T:**
            - Don't use filesystem tools on registered documents
            - Don't give up after one failed query - retry with broader terms
            - Don't use `chunks()` as your first choice unless query is very broad
            - Don't read source files directly - they are already indexed with structure
            
            ---
            
            ## Query Type Detection
            
            **Documentation Queries** (use $.content.heading or $.content.chunks):
            - "What is X?" → Concepts, definitions
            - "How to use X?" → Usage guides, tutorials
            - "Design of X" → Architecture docs
            
            **Code Queries** (use $.content.heading focusing on code files):
            - "How does X work?" → Find class X and its methods
            - "Implementation of Y" → Find method Y with code
            - "Where is Z defined?" → Find class/method Z
            - "What does method M do?" → Find method M implementation
            
            ---
            
            ## Successful Query Examples
            
            **Query: "What colors are used in the design system?"**
            ```json
            {
              "query": "$.content.heading(\"color\")",
              "documentPath": "design-system-color.md"
            }
            ```
            ✅ Direct filename match + targeted heading search
            
            **Query: "How do I use custom icons?"**
            First attempt:
            ```json
            {
              "query": "$.content.heading(\"custom icons\")",
              "documentPath": "custom-icons-usage.md"
            }
            ```
            If empty, retry with:
            ```json
            {
              "query": "$.content.chunks(\"icons\")",
              "documentPath": "custom-icons-usage.md"
            }
            ```
            ✅ Good retry strategy: specific → broader
            
            **Query: "Tell me about architecture" (ambiguous)**
            ```json
            {
              "query": "$.toc[*]"
            }
            ```
            Then identify relevant doc(s) from TOC and query specifically
            ✅ Use TOC when multiple architecture-related files exist

        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
