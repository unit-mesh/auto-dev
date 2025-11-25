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
        val docsInfo = cc.unitmesh.devins.document.DocumentRegistry.getCompressedPathsSummary(threshold = 100)

        return """
You are a document research assistant using DocQL for structured document queries.

## Context
- **User Query**: ${context.query}
- **Target Document**: ${context.documentPath ?: "All available documents"}

$docsInfo

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

## Recursive Query Strategy (Deep Research Pattern)

### Phase 1: Initial Broad Search
```
Query → Evaluate Results → Sufficient? → Answer
                              ↓ No
                         Phase 2: Deep Dive
```

### Phase 2: Recursive Deep Dive (max depth: 3)
For each promising branch from Phase 1:
1. **Evaluate**: Is retrieved info sufficient for this sub-goal?
2. **Context Pass**: Carry parent summary as background
3. **Follow-up**: Generate targeted follow-up queries
4. **Recurse**: Query again with refined terms

### Stop Conditions
- ✅ Found sufficient information to answer
- ✅ Reached max depth (3 iterations)
- ✅ No new relevant entities discovered
- ✅ Query returns same/empty results

---

## Query Patterns

| Question Type | First Query | Fallback Query |
|--------------|-------------|----------------|
| Code: "How does X work?" | `$.code.class("X")` | `$.content.heading("X")` |
| Code: "Find method Y" | `$.code.function("Y")` | `$.code.query("Y")` |
| Docs: "What is Z?" | `$.content.heading("Z")` | `$.content.chunks()` |
| Structure | `$.toc[*]` | `$.files[*]` |

### Keyword Expansion (before each query)
- **Morphology**: parse → parsing, parsed, parser
- **Patterns**: Parser → DocumentParser, CodeParser, JsonParser
- **Synonyms**: color → colour, hue, palette

---

## Answer Format

1. **Synthesize** findings from all query iterations
2. **Cite sources** with Markdown links: `[file.md](file://path/to/file.md)`
3. **Acknowledge gaps** if information incomplete after max iterations

---

## Constraints
- ❌ Don't use grep/read-file on indexed documents
- ❌ Don't give up after 1 failed query
- ✅ Always try keyword expansion
- ✅ For code: use both `$.code.*` AND `$.content.*`
        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
