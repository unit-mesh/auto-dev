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
        // Get list of registered documents
        val registeredDocs = cc.unitmesh.devins.document.DocumentRegistry.getRegisteredPaths()
        val docsInfo = if (registeredDocs.isNotEmpty()) {
            """
            ## Available Documents
            
            The following documents are registered and ready to query:
            ${registeredDocs.joinToString("\n") { "- $it" }}
            
            **Total: ${registeredDocs.size} document(s)**
            """.trimIndent()
        } else {
            "## Available Documents\n\nNo documents currently registered."
        }
        
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
            ## Keyword Expansion for Better Search

            **Before querying, always expand keywords to improve matching accuracy.**

            ### 1. Morphological Variations
            Generate different word forms:
            - encode → encoding, encoded, encoder, encoders  
            - design → designing, designed, designer  
            - color → colors, colour, colours  

            ### 2. Synonyms & Related Terms
            Consider conceptually related words:
            - color → colour, hue, palette, theme  
            - document → doc, documentation, file  
            - architecture → arch, structure, design  

            ### 3. Multi-Language Expansion (Chinese ↔ English)
            Translate and expand cross-language keywords:
            - 颜色 → color, colour  
            - 设计 → design  
            - 导航 → navigation, nav  
            - 架构 → architecture, arch  
            - 文档 → document, doc  

            ### 4. Abbreviations & Compounds
            Include naming variations:
            - base64 → base 64, base-64  
            - API → api, interface  
            - SVG → svg, vector  

            ---

            ## Examples

            ### Example 1 — English
            Query: “Where’s the code for base64 encoding?”  
            Expanded keywords: base64, base 64, encoding, encode, encoder, code  
            Match: any filename containing **base64** or **encode**

            ### Example 2 — Chinese
            Query: “设计系统的颜色在哪里？”  
            Expanded keywords: 设计系统, design system, design-system, 颜色, color, colour  
            Match: **design-system-color.md**

            ### Example 3 — Mixed
            Query: “navigation 架构文档”  
            Expanded keywords: navigation, nav, 导航, 架构, architecture, 文档, document, doc  
            Match: **navigation-architecture.md**

            ---

            ## Smart Document Selection

            **Use document filenames to guide precise querying.**

            ### Strategy
            1. **Review available documents**
            2. **Expand user keywords**
            3. **Match expanded keywords to filenames**
            4. **Use `documentPath` to target relevant documents**

            ### When to Use `documentPath`
            Use it when:
            - The filename clearly matches keywords  
            - The topic is specific  
            - Irrelevant documents should be avoided  

            Query all documents when:
            - No filename clearly matches  
            - There are only a few documents  
            - The user query is broad or exploratory  

            ---

            ## Examples

            ### Example 1 — Clear Match
            User: “What are the color principles?”  
            Docs: design-system-color.md, navigation.md, icons.md  
            Match: **design-system-color.md**  
            Action: `documentPath="design-system-color.md"`

            ### Example 2 — Multiple Matches
            User: “How do I use custom icons?”  
            Docs: custom-icons-usage.md, SVG-to-ImageVector-conversion.md  
            Matches: both  
            Action: Query both → synthesize

            ### Example 3 — No Clear Match
            User: “Show me everything.”  
            Action: Query all documents using `$.toc[*]`

            ---

            ## Best Practices

            1. **Check filenames first**  
            2. **Use `documentPath` whenever possible**  
            3. **Expand keywords (variations, synonyms, translations)**  
            4. **Start with the TOC** using `$.toc[*]`  
            5. **Use `heading()` for targeted sections**  
            6. **Try alternative keywords if matching fails**  
            7. **Use `chunks()` as a fallback**  
            8. **Never guess — always retrieve via tools**

            Always use the `docql` tool to retrieve information. Do not guess.
        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
