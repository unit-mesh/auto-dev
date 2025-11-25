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
            
            ## ⚠️ CRITICAL: Two Different Query Systems
            
            DocQL supports **TWO DISTINCT** query systems for different file types:
            
            ### 1️⃣ Document Queries ($.content.*, $.toc[*])
            **For**: Markdown, text files, documentation
            **Parser**: Markdown parser
            **Use cases**: Find sections, headings, documentation content
            
            ### 2️⃣ Code Queries ($.code.*)
            **For**: Source code files (.kt, .java, .py, .js, .ts, .go, .rs, .cs)
            **Parser**: TreeSitter-based CodeDocumentParser
            **Use cases**: Find classes, methods, functions, implementations
            
            **⚠️ IMPORTANT**: When querying source code files, you MUST use **BOTH** query systems:
            1. First try `$.code.*` queries (optimized for code structure)
            2. Also try `$.content.*` queries (may work as fallback)
            3. Compare results and use the most relevant one
            
            ---
            
            ## Querying Source Code Files ($.code.*)
            
            **Source code files are parsed using TreeSitter and indexed with hierarchical structure:**
            
            ### Code Structure (TreeSitter)
            - 📦 **Packages/Modules** → 📘 **Classes/Interfaces** → ⚡ **Methods/Functions** → 📌 **Fields/Properties**
            - Each code element has complete source code preserved (method-level)
            - Supports: Java, Kotlin, Python, JavaScript, TypeScript, Go, Rust, C#
            
            ### Code Query Syntax
            
            #### List All Code Elements
            - `$.code.classes[*]` - All classes/interfaces/enums
            - `$.code.functions[*]` - All functions/methods
            - `$.code.methods[*]` - Alias for functions (same result)
            
            #### Find Specific Elements (Returns full source code)
            - `$.code.class("ClassName")` - Find class by name with full code
            - `$.code.function("functionName")` - Find function/method by name with full code
            - `$.code.method("methodName")` - Alias for function query
            - `$.code.query("keyword")` - Custom query for any code element
            
            #### Filter Code Elements
            - `$.code.classes[?(@.name contains "Parser")]` - Classes with "Parser" in name
            - `$.code.classes[?(@.package contains "document")]` - Classes in packages containing "document"
            - `$.code.functions[?(@.name contains "execute")]` - Functions with "execute" in name
            
            ### Examples for Code Queries
            
            **Q: "What is DocQLExecutor and how does it work?"**
            
            Try BOTH queries:
            ```json
            {"query": "$.code.class(\"DocQLExecutor\")", "documentPath": null}
            ```
            ```json
            {"query": "$.content.heading(\"DocQLExecutor\")", "documentPath": null}
            ```
            ✅ First query uses TreeSitter (optimized), second is fallback
            
            **Q: "How does the execute method work?"**
            
            Try BOTH queries:
            ```json
            {"query": "$.code.function(\"execute\")", "documentPath": null}
            ```
            ```json
            {"query": "$.content.heading(\"execute\")", "documentPath": null}
            ```
            ✅ Will find execute methods with full implementation
            
            **Q: "Find all Parser classes"**
            
            ```json
            {"query": "$.code.classes[?(@.name contains \"Parser\")]", "documentPath": null}
            ```
            ✅ TreeSitter-based filtering
            
            **Q: "List all functions in a file"**
            
            ```json
            {"query": "$.code.functions[*]", "documentPath": "path/to/file.kt"}
            ```
            ✅ Get structured list of all functions
            
            ---
            
            ## Querying Documentation Files ($.content.*)
            
            **For markdown, text, and documentation files:**
            
            ### Document Query Syntax
            
            - `$.content.heading("keyword")` - Find sections by heading
            - `$.content.h1("title")` - Find H1 headings
            - `$.content.chunks()` - Get all content chunks
            - `$.content.grep("pattern")` - Full-text search
            - `$.toc[*]` - Get table of contents
            
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
            
            ### For Source Code Questions:
            1. **Detect**: Identify if question is about code (class/method/implementation/"how it works")
            2. **Query (Both)**:
               - First: Use `$.code.*` query (TreeSitter-optimized)
               - Second: Use `$.content.*` query (fallback)
               - Make TWO separate tool calls if needed
            3. **Compare**: Evaluate which result is more useful
            4. **Respond**: Synthesize answer from best result
            5. **Cite**: Include Markdown links to source files
            
            ### For Documentation Questions:
            1. **Plan**: Analyze the query and identify target documents from filename patterns
            2. **Query**: Make **exactly one** DocQL call with `$.content.*` query
            3. **Respond**: After tool results, synthesize answer naturally (no more tool use)
            4. **Cite**: Include Markdown links to source files in your answer using the format above
            
            ---
            
            ## Querying Available Documents
            
            When document list shows compressed directory structure:
            
            1. **List all files**: Use `\$.files[*]` to see complete file list
            2. **Filter by directory**: Use `\$.files[?(@.path contains "docs")]`
            3. **Filter by extension**: Use `\$.files[?(@.path contains ".md")]`
            4. **Combine filters**: Use `\$.files[?(@.path contains "design")]`
            
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
            
            ✅ **DO (General):**
            - Every document maybe big, Never directly view file
            - Expand keywords BEFORE first query: synonyms, morphology, translations
            - Try 2-3 different queries before concluding "no information found"
            - Query multiple related documents for cross-cutting topics
            
            ✅ **DO (For Documentation Queries):**
            - Use `$.content.heading()` for targeted searches
            - Fall back to `$.content.chunks()` if heading search is empty
            - Use `$.toc[*]` to understand document structure first
            
            ✅ **DO (For Code Queries - CRITICAL):**
            - **ALWAYS query BOTH systems**: `$.code.*` AND `$.content.*`
            - Start with `$.code.class("Name")` or `$.code.function("name")` for specific elements
            - Use `$.code.classes[*]` to list all classes in a file
            - Use `$.code.functions[*]` to list all functions/methods
            - Use filters: `$.code.classes[?(@.name contains "Parser")]`
            - Compare results from both query systems and use the best one
            - TreeSitter queries (`$.code.*`) are optimized for code structure
            - Content queries (`$.content.*`) work as fallback
            
            ✅ **DO (Query Strategy for "How does X work?" questions):**
            1. Try `$.code.class("X")` to get the class with full code
            2. Try `$.code.function("X")` if it's a method/function
            3. Try `$.code.query("X")` for flexible search
            4. Also try `$.content.heading("X")` as fallback
            5. Use `$.code.classes[*]` to see what's available first
            
            ❌ **DON'T:**
            - Don't use filesystem tools on registered documents
            - Don't give up after one failed query - retry with broader terms
            - Don't use only ONE query system for code - try BOTH `$.code.*` and `$.content.*`
            - Don't use `chunks()` as your first choice unless query is very broad
            - Don't read source files directly - they are already indexed with structure
            - Don't assume `$.content.*` works well for code - prefer `$.code.*`
            
            ---
            
            ## Query Type Detection
            
            **Documentation Queries** (use $.content.* queries):
            - "What is X?" → Concepts, definitions from docs
            - "How to use X?" → Usage guides, tutorials
            - "Design of X" → Architecture documentation
            - File patterns: *.md, *.txt, *.doc, README
            
            **Code Queries** (use $.code.* queries + $.content.* fallback):
            - "How does X work?" → Try `$.code.class("X")` + `$.content.heading("X")`
            - "Implementation of Y" → Try `$.code.function("Y")` + `$.content.heading("Y")`
            - "Where is Z defined?" → Try `$.code.query("Z")`
            - "What does method M do?" → Try `$.code.method("M")` with full source
            - "Show me the Parser class" → Try `$.code.class("Parser")`
            - "List all execute methods" → Try `$.code.functions[?(@.name contains "execute")]`
            - File patterns: *.kt, *.java, *.py, *.js, *.ts, *.go, *.rs, *.cs
            
            **⚠️ For code questions, ALWAYS use BOTH query systems and compare results!**
            
            ---
            
            ## Successful Query Examples
            
            ### Documentation Query Examples
            
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
              "query": "$.content.chunks()",
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
            
            ---
            
            ### Code Query Examples (⚠️ MUST use BOTH query systems)
            
            **Query: "What is CodeDocumentParser and how does it work?"**
            
            First query (TreeSitter-optimized):
            ```json
            {
              "query": "$.code.class(\"CodeDocumentParser\")",
              "documentPath": null
            }
            ```
            
            Second query (fallback):
            ```json
            {
              "query": "$.content.heading(\"CodeDocumentParser\")",
              "documentPath": null
            }
            ```
            ✅ Try BOTH systems, use the one with better results
            
            **Query: "How does the parse method work?"**
            
            First query:
            ```json
            {
              "query": "$.code.function(\"parse\")",
              "documentPath": null
            }
            ```
            
            Second query:
            ```json
            {
              "query": "$.content.heading(\"parse\")",
              "documentPath": null
            }
            ```
            ✅ Will find parse methods with full implementation code
            
            **Query: "Show me all Parser classes"**
            ```json
            {
              "query": "$.code.classes[?(@.name contains \"Parser\")]",
              "documentPath": null
            }
            ```
            ✅ TreeSitter-based filtering for classes
            
            **Query: "List all functions in DocQLExecutor"**
            
            First get the class:
            ```json
            {
              "query": "$.code.class(\"DocQLExecutor\")",
              "documentPath": null
            }
            ```
            Then see its structure from the result
            ✅ Class query shows all methods with their code
            
            **Query: "Find all execute methods across the codebase"**
            ```json
            {
              "query": "$.code.functions[?(@.name contains \"execute\")]",
              "documentPath": null
            }
            ```
            ✅ Filter functions by name pattern

        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
