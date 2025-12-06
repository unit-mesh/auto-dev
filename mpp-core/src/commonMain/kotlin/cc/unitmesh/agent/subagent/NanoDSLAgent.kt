package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.boolean
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable

/**
 * NanoDSL Agent - Generates NanoDSL UI code from natural language descriptions.
 *
 * NanoDSL is a Python-style indentation-based DSL for AI-generated UI,
 * prioritizing token efficiency and human readability.
 *
 * Features:
 * - Converts natural language UI descriptions to NanoDSL code
 * - Supports component generation (Card, VStack, HStack, Button, etc.)
 * - Supports state management and event handling
 * - Supports HTTP requests with Fetch action
 *
 * Cross-platform support:
 * - JVM: Full support with prompt templates
 * - JS/WASM: Available via JsNanoDSLAgent wrapper
 */
class NanoDSLAgent(
    private val llmService: KoogLLMService,
    private val promptTemplate: String = DEFAULT_PROMPT
) : SubAgent<NanoDSLContext, ToolResult.AgentResult>(
    definition = createDefinition()
) {
    private val logger = getLogger("NanoDSLAgent")

    override val priority: Int = 50 // Higher priority for UI generation tasks

    override fun validateInput(input: Map<String, Any>): NanoDSLContext {
        val description = input["description"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: description")
        val componentType = input["componentType"] as? String
        val includeState = input["includeState"] as? Boolean ?: true
        val includeHttp = input["includeHttp"] as? Boolean ?: false

        return NanoDSLContext(
            description = description,
            componentType = componentType,
            includeState = includeState,
            includeHttp = includeHttp
        )
    }

    override suspend fun execute(
        input: NanoDSLContext,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        onProgress("🎨 NanoDSL Agent: Generating UI from description")
        onProgress("Description: ${input.description.take(80)}...")

        return try {
            val prompt = buildPrompt(input)
            onProgress("Calling LLM for code generation...")

            val responseBuilder = StringBuilder()
            llmService.streamPrompt(
                userPrompt = prompt,
                compileDevIns = false
            ).toList().forEach { chunk ->
                responseBuilder.append(chunk)
            }

            val llmResponse = responseBuilder.toString()

            // Extract NanoDSL code from markdown code fence
            val codeFence = CodeFence.parse(llmResponse)
            val generatedCode = if (codeFence.text.isNotBlank()) {
                codeFence.text.trim()
            } else {
                llmResponse.trim()
            }

            onProgress("✅ Generated ${generatedCode.lines().size} lines of NanoDSL code")

            ToolResult.AgentResult(
                success = true,
                content = generatedCode,
                metadata = mapOf(
                    "description" to input.description,
                    "componentType" to (input.componentType ?: "auto"),
                    "linesOfCode" to generatedCode.lines().size.toString(),
                    "includesState" to input.includeState.toString(),
                    "includesHttp" to input.includeHttp.toString()
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "NanoDSL generation failed" }
            onProgress("❌ Generation failed: ${e.message}")

            ToolResult.AgentResult(
                success = false,
                content = "Failed to generate NanoDSL: ${e.message}",
                metadata = mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    private fun buildPrompt(input: NanoDSLContext): String {
        val featureHints = buildList {
            if (input.includeState) add("Include state management if needed")
            if (input.includeHttp) add("Include HTTP request actions (Fetch) if applicable")
            input.componentType?.let { add("Focus on creating a $it component") }
        }.joinToString("\n- ")

        return """
$promptTemplate

${if (featureHints.isNotEmpty()) "## Additional Requirements:\n- $featureHints\n" else ""}
## User Request:
${input.description}
""".trim()
    }

    override fun formatOutput(output: ToolResult.AgentResult): String {
        return if (output.success) {
            "Generated NanoDSL:\n```nanodsl\n${output.content}\n```"
        } else {
            "Error: ${output.content}"
        }
    }

    override fun getParameterClass(): String = "NanoDSLContext"

    override fun shouldTrigger(context: Map<String, Any>): Boolean {
        val content = context["content"] as? String ?: return false
        val keywords = listOf("ui", "interface", "form", "card", "button", "component", "layout")
        return keywords.any { content.lowercase().contains(it) }
    }

    override suspend fun handleQuestion(
        question: String,
        context: Map<String, Any>
    ): ToolResult.AgentResult {
        // Treat questions as UI generation requests
        return execute(
            NanoDSLContext(description = question),
            onProgress = {}
        )
    }

    override fun getStateSummary(): Map<String, Any> = mapOf(
        "name" to name,
        "description" to description,
        "priority" to priority,
        "supportedFeatures" to listOf("components", "state", "actions", "http-requests")
    )

    companion object {
        private fun createDefinition() = AgentDefinition(
            name = "nanodsl-agent",
            displayName = "NanoDSL Agent",
            description = "Generates NanoDSL UI code from natural language descriptions",
            promptConfig = PromptConfig(
                systemPrompt = "You are a NanoDSL expert. Generate token-efficient UI code."
            ),
            modelConfig = ModelConfig.default(),
            runConfig = RunConfig(maxTurns = 3, maxTimeMinutes = 2)
        )

        const val DEFAULT_PROMPT = """You are a NanoDSL expert. Generate UI code using NanoDSL syntax.

## NanoDSL Syntax

NanoDSL uses Python-style indentation (4 spaces) to represent hierarchy.

### Components
- `component Name:` - Define a component
- `VStack(spacing="sm"):` - Vertical stack layout
- `HStack(align="center", justify="between"):` - Horizontal stack layout
- `Card:` - Container with padding/shadow
- `Text("content", style="h1|h2|h3|body|caption")` - Text display
- `Button("label", intent="primary|secondary")` - Clickable button
- `Image(src=path, aspect=16/9, radius="md")` - Image display
- `Input(value=binding, placeholder="...")` - Text input
- `Badge("text", color="green|red|blue")` - Status badge

### Properties
- `padding: "sm|md|lg"` - Padding size
- `shadow: "sm|md|lg"` - Shadow depth
- `spacing: "sm|md|lg"` - Gap between items

### State (for interactive components)
```
state:
    count: int = 0
    name: str = ""
```

### Bindings
- `<<` - One-way binding (subscribe)
- `:=` - Two-way binding

### Actions
- `on_click: state.var += 1` - State mutation
- `Navigate(to="/path")` - Navigation
- `ShowToast("message")` - Show notification
- `Fetch(url="/api/...", method="POST", body={...})` - HTTP request

### HTTP Requests
```
Button("Submit"):
    on_click:
        Fetch(
            url="/api/login",
            method="POST",
            body={"email": state.email, "password": state.password},
            on_success: Navigate(to="/dashboard"),
            on_error: ShowToast("Failed")
        )
```

## Output Rules
1. Output ONLY NanoDSL code, no explanations
2. Use 4 spaces for indentation
3. Keep it minimal - no redundant components
4. Wrap output in ```nanodsl code fence"""
    }
}

/**
 * NanoDSL generation context
 */
@Serializable
data class NanoDSLContext(
    val description: String,
    val componentType: String? = null,
    val includeState: Boolean = true,
    val includeHttp: Boolean = false
)

/**
 * Schema for NanoDSL Agent tool
 */
object NanoDSLAgentSchema : DeclarativeToolSchema(
    description = "Generate NanoDSL UI code from natural language description",
    properties = mapOf(
        "description" to string(
            description = "Natural language description of the UI to generate",
            required = true
        ),
        "componentType" to string(
            description = "Optional: Specific component type to focus on (card, form, list, etc.)",
            required = false
        ),
        "includeState" to boolean(
            description = "Whether to include state management (default: true)",
            required = false
        ),
        "includeHttp" to boolean(
            description = "Whether to include HTTP request actions (default: false)",
            required = false
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return """/$toolName description="Create a contact form with name, email, message fields and a submit button that sends to /api/contact" includeHttp=true"""
    }
}

