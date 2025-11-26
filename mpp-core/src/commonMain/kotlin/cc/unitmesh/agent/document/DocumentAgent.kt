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
            // Register DocQLTool with LLM service for LLM-based reranking
            registerTool(DocQLTool(llmService))
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
You are a document research assistant. **ALWAYS use DocQL tool FIRST** to search the project.

## Your Task
Answer: "${context.query}"
${context.documentPath?.let { "Target: $it" } ?: ""}

$docsInfo

## ⚡ REQUIRED: Use DocQL First!

**Step 1: Search with DocQL using keyword**
```
<devin>
/docql
```json
{"query": "your_keyword"}
```
</devin>
```

Example for "How does MCP work?":
```json
{"query": "MCP"}
```

**Step 2: If needed, use specific queries**
- Code: `{"query": "$.code.class(\"ClassName\")"}`
- Docs: `{"query": "$.content.heading(\"Title\")"}`

## Quick Reference
| Goal | Query |
|------|-------|
| Find by keyword | `{"query": "MCP"}` |
| Find class | `{"query": "$.code.class(\"AuthService\")"}` |
| Find function | `{"query": "$.code.function(\"parse\")"}` |
| Find heading | `{"query": "$.content.heading(\"Architecture\")"}` |
| List all TOC | `{"query": "$.toc[*]"}` |

## Other Tools
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

## Rules
1. **ALWAYS start with DocQL** - search first, then analyze
2. Use simple keyword search for general questions
3. Use $.code.* for code questions, $.content.* for docs
4. Execute ONE tool per response
5. Cite sources with file paths in your answer
        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
