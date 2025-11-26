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
    private val enableLLMStreaming: Boolean = true,
    private val contentThreshold: Int = 5000  // P0: Long content threshold
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

    private val subAgentManager = cc.unitmesh.agent.core.SubAgentManager()

    private val toolRegistry = run {
        ToolRegistry(
            fileSystem = actualFileSystem,
            shellExecutor = shellExecutor ?: DefaultShellExecutor(),
            configService = mcpToolConfigService,
            subAgentManager = subAgentManager,
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
        subAgentManager = subAgentManager,  // P0: Pass SubAgentManager
        enableLLMStreaming = enableLLMStreaming
    )

    init {
        val analysisAgent = cc.unitmesh.agent.subagent.AnalysisAgent(llmService, contentThreshold)
        toolRegistry.registerTool(analysisAgent)
        subAgentManager.registerSubAgent(analysisAgent)
        
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
        val docsInfo = cc.unitmesh.devins.document.DocumentRegistry.getCompressedPathsSummary(threshold = 100)

        return """
You are a document research assistant using DocQL for structured document queries.

## Context
- **User Query**: ${context.query}
- **Target Document**: ${context.documentPath ?: "All available documents"}

$docsInfo

## TWO QUERY SYSTEMS - Use Both for Code Questions!

### 1. Code Queries ($.code.*) - For Source Code Files (.kt, .java, .py, .ts, .js)
**Returns actual source code with full implementation.**

| Query | Description | Example |
|-------|-------------|---------|
| `$.code.class("ClassName")` | Get full class source code | `$.code.class("DocQLLexer")` |
| `$.code.function("funcName")` | Get function implementation | `$.code.function("tokenize")` |
| `$.code.classes[*]` | List all classes | `$.code.classes[*]` |
| `$.code.functions[*]` | List all functions | `$.code.functions[*]` |
| `$.code.classes[?(@.name ~= "Pattern")]` | Find classes by pattern | `$.code.classes[?(@.name ~= "Lexer")]` |
| `$.code.query("keyword")` | Search code for keyword | `$.code.query("tokenize")` |

### 2. Document Queries ($.content.*) - For Documentation (.md, .txt, README)
**Returns documentation content, headings, sections.**

| Query | Description | Example |
|-------|-------------|---------|
| `$.content.heading("title")` | Find section by heading | `$.content.heading("Architecture")` |
| `$.content.chunks()` | Get all content chunks | `$.content.chunks()` |
| `$.toc[*]` | Get table of contents | `$.toc[*]` |
| `$.files[*]` | List all files | `$.files[*]` |

## CRITICAL: JSON Format for Tool Calls

Always use complete JSON with the full query string:

```
<devin>
/docql
```json
{"query": "$.code.class(\"DocQLLexer\")", "maxResults": 10}
```
</devin>
```

❌ WRONG: `$.code.class(`  (incomplete)
✅ RIGHT: `$.code.class("ClassName")` (with quotes and class name)

## Smart Capabilities & SubAgent System
- **Automatic Summarization**: Large content (>$contentThreshold chars) is automatically summarized by an Analysis Agent.
- **Code Files**: For code queries, you'll get actual source code (not summaries).
- **SubAgent Conversation**: Use `/ask-agent agentName="analysis-agent" question="..."` to ask follow-up questions about analyzed content.

## Available Tools
${AgentToolFormatter.formatToolListForAI(toolRegistry.getAllTools().values.toList())}

## Tool Format
```
<devin>
/tool-name
```json
{"param": "value"}
```
</devin>
```
⚠️ Execute ONE tool per response.

---

## Query Strategy for Code Questions

When asked about code implementation:

1. **First**: Try `$.code.class("ClassName")` or `$.code.function("funcName")`
2. **If not found**: Try `$.code.classes[?(@.name ~= "keyword")]`  
3. **Fallback**: Try `$.content.heading("keyword")` or `$.code.query("keyword")`

Example for "Find DocQLLexer class":
```json
{"query": "$.code.class(\"DocQLLexer\")"}
```

Example for "List all Lexer classes":
```json
{"query": "$.code.classes[?(@.name ~= \"Lexer\")]"}
```

---

## Answer Format

1. **Synthesize** findings from all query iterations
2. **Cite sources** with Markdown links: `[file.md](file://path/to/file.md)`
3. **Show code** when found - include relevant code snippets
4. **Acknowledge gaps** if information incomplete after max iterations

---

## Constraints
- ❌ Don't use incomplete queries like `$.code.class(` 
- ❌ Don't give up after 1 failed query
- ✅ Always use complete query syntax with all parameters
- ✅ For code questions: Try $.code.* queries first
        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
