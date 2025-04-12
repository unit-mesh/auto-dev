package cc.unitmesh.devti.mcp.ui.model

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Based on https://github.com/jujumilk3/leaked-system-prompts/blob/main/anthropic-claude-api-tool-use_20250119.md
 */
data class McpLlmConfig(
    var temperature: Double = 0.0,
    var enabledTools: MutableList<Tool> = mutableListOf(),
    var systemPrompt: String = ""
) {
    fun createSystemPrompt(): String {
        val systemPrompt = """
In this environment you have access to a set of tools you can use to answer the user's question.
You can invoke functions by writing a "<devins:function_calls>" inside markdown code-block like the following as part of your reply to the user:

```xml
<devins:function_calls>
<devins:invoke name="${'$'}FUNCTION_NAME">
<devins:parameter name="${'$'}PARAMETER_NAME">${'$'}PARAMETER_VALUE</devins:parameter>
...
</devins:invoke>
<devins:invoke name="${'$'}FUNCTION_NAME2">
...
</devins:invoke>
</devins:function_calls>
```

String and scalar parameters should be specified as is, while lists and objects should use JSON format.

Here are the functions available in JSONSchema format:
<functions>
${enabledTools.joinToString("\n") { tool -> "<function>" + Json.Default.encodeToString(tool) } + "</function>"} }
</functions>

Answer the user's request using the relevant tool(s), if they are available. Check that all the required parameters for each tool call are provided or can reasonably be inferred from context. IF there are no relevant tools or there are missing values for required parameters, ask the user to supply these values; otherwise proceed with the tool calls. If the user provides a specific value for a parameter (for example provided in quotes), make sure to use that value EXACTLY. DO NOT make up values for or ask about optional parameters. Carefully analyze descriptive terms in the request as they may indicate required parameter values that should be included even if not explicitly quoted.

If you intend to call multiple tools and there are no dependencies between the calls, make all of the independent calls in the same <devins:function_calls></devins:function_calls> block.
""".trimIndent()
        return systemPrompt
    }
}