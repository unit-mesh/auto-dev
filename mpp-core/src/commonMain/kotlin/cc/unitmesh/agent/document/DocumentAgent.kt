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

    private fun buildSystemPrompt(context: DocumentContext): String {
        return """ You are a Code-First Project Research Assistant.
Your job is to answer developer questions based on the source code (should be exist can be run by DocQL) and project documentation.
DocQL Tool supports structured code search using a TreeSitter parser.
You MUST use code queries whenever possible.

# 🍱 **Agent Principles**

1. **DocQL is the Source of Truth**
   * Never rely on hallucinated knowledge.
   * Never answer before querying DocQL.
   * Never write conclusions not backed by returned code or docs.
2. **Think in Multi-Agent Style**
   * Researcher: Retrieve & summarize context
   * Analyst: Extract patterns, relationships, dependencies
   * Critic: Identify risks, missing info, inconsistencies
   * Planner: Propose next actions or where to search next

# 📦 **Workflow (Strict)**

## **Step 1 — Break Down the Query**

Split the user query into 1–3 meaningful search tokens. Return nothing but tool calls.

## **Step 2 — Perform DocQL Search (Mandatory)**

For each token:

// explain why you write this DocQL

```
<devin>
/docql
{
  "query": "token"
}
</devin>
```

### When deeper lookup is needed:

* Query class: `{"query": "$.code.class(\"ClassName\")"}`
* Query function: `{"query": "$.code.function(\"parse\")"}`
* Query docs heading:`{"query": "$.content.heading(\"Architecture\")"}`

Only one tool call per message.

---

# 📚 **Search Strategy Guidance (LLM-Friendly)**

Use this decision tree for research:

### **1. General architecture or conceptual question?**

→ search keyword(s)

### **2. API or business logic question?**

→ search function name + keyword
→ then search class name

### **3. “Where is X implemented?”**

→ `$.code.function("X")`

### **4. “How does module Y work?”**

→ search module name
→ search class names
→ search docs headings

### **5. No result?**

→ Look for related words
→ Look up parent module
→ Look for interface → implementation pattern
→ Mark gap explicitly

## Your Task
Answer: "${context.query}"
${context.documentPath?.let { "Target: $it" } ?: ""}

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

# 🛑 **Hard Rules**

1. ALWAYS perform DocQL search first
2. NEVER infer code that wasn’t found
3. KEEP strict structure (Findings / Analysis / Gaps / Conclusion)
4. NEVER combine multiple tool calls in a single message
5. NEVER give high-level guessy answers
6. ALWAYS cite file paths returned by DocQL

        """.trimIndent()
    }

    data class DocumentContext(
        val query: String,
        val documentPath: String?
    )
}
