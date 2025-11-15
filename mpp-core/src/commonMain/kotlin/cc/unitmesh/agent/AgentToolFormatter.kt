package cc.unitmesh.agent

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import kotlinx.serialization.json.*

/**
 * Unified tool formatter for agent contexts
 * 
 * This class provides consistent tool list formatting across all agent types
 * (Coding, CodeReview, etc.). It handles both built-in tools and MCP tools.
 */
object AgentToolFormatter {
    private val logger = getLogger("AgentToolFormatter")

    /**
     * Format tool list with enhanced schema information for AI understanding
     * 
     * @param toolList List of executable tools to format
     * @return Formatted string containing tool descriptions and schemas
     */
    fun formatToolListForAI(toolList: List<ExecutableTool<*, *>>): String {
        logger.debug { "🔍 [AgentToolFormatter] Formatting tool list with ${toolList.size} tools:" }
        toolList.forEach { tool ->
            logger.debug { "  - ${tool.name} (${tool::class.simpleName}): ${tool.getParameterClass()}" }
        }

        if (toolList.isEmpty()) {
            logger.warn { "❌ [AgentToolFormatter] Tool list is empty" }
            return "No tools available."
        }

        return toolList.joinToString("\n\n") { tool ->
            buildString {
                // Tool header with name and description
                appendLine("## ${tool.name}")

                // Check for empty description and provide warning
                val description = tool.description.takeIf { it.isNotBlank() }
                    ?: "Tool description not available"
                appendLine("**Description:** $description")
                appendLine()

                // Get ToolType for schema information
                val toolType = tool.name.toToolType()

                if (toolType != null) {
                    // Use JSON Schema for built-in tools
                    appendLine("**Parameters JSON Schema:**")
                    appendLine("```json")

                    val jsonSchema = toolType.schema.toJsonSchema()
                    // Pretty print the JSON schema
                    val prettyJson = formatJsonSchema(jsonSchema)
                    appendLine(prettyJson)

                    appendLine("```")
                } else {
                    // Fallback for MCP tools or other tools
                    val paramClass = tool.getParameterClass()
                    when {
                        paramClass.isBlank() || paramClass == "Unit" -> {
                            appendLine("**Parameters:** None")
                        }
                        paramClass == "AgentInput" -> {
                            // Generic agent input - provide more specific info for SubAgents
                            appendLine("**Parameters JSON Schema:**")
                            appendLine("```json")
                            appendLine("""{
  "${'$'}schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Generic agent input parameters",
  "additionalProperties": true
}""")
                            appendLine("```")
                        }
                        tool.name.contains("_") -> {
                            // Likely an MCP tool
                            appendLine("**Parameters JSON Schema:**")
                            appendLine("```json")
                            appendLine("""{
  "${'$'}schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "MCP tool parameters",
  "additionalProperties": true
}""")
                            appendLine("```")
                        }
                        else -> {
                            // Valid parameter class
                            appendLine("**Parameters:** $paramClass")
                        }
                    }
                }

                // Add example if available
                val example = generateToolExample(tool, toolType)
                if (example.isNotEmpty()) {
                    appendLine()
                    appendLine("**Example:**")
                    appendLine(example)
                }
            }
        }
    }

    /**
     * Format tool list as simple bullet points (for display purposes)
     * 
     * @param toolList List of executable tools
     * @return Simple formatted string with tool names and descriptions
     */
    fun formatToolListSimple(toolList: List<ExecutableTool<*, *>>): String {
        return buildString {
            toolList.forEach { tool ->
                appendLine("- ${tool.name}: ${tool.description}")
            }
        }
    }

    /**
     * Format JSON schema as compact single line with $schema field
     */
    private fun formatJsonSchema(jsonElement: JsonElement): String {
        val jsonObject = jsonElement.jsonObject.toMutableMap()

        // Add $schema field if not present
        if (!jsonObject.containsKey("\$schema")) {
            jsonObject["\$schema"] = JsonPrimitive("http://json-schema.org/draft-07/schema#")
        }

        // Create a new JsonObject with $schema first
        val orderedJson = buildJsonObject {
            put("\$schema", jsonObject["\$schema"]!!)
            jsonObject.forEach { (key, value) ->
                if (key != "\$schema") {
                    put(key, value)
                }
            }
        }

        // Return compact JSON string
        return orderedJson.toString()
    }

    /**
     * Generate example usage for a tool with DevIns-style format (/command + JSON block)
     */
    private fun generateToolExample(tool: ExecutableTool<*, *>, toolType: ToolType?): String {
        return if (toolType != null) {
            // Generate DevIns-style example with JSON parameters
            generateDevInsExample(tool.name, toolType)
        } else {
            // Fallback for MCP tools or other tools
            when (tool.name) {
                "read-file" -> """/${tool.name}
```json
{"path": "src/main.kt", "startLine": 1, "endLine": 50}
```"""
                "write-file" -> """/${tool.name}
```json
{"path": "output.txt", "content": "Hello, World!"}
```"""
                "grep" -> """/${tool.name}
```json
{"pattern": "function.*main", "path": "src", "include": "*.kt"}
```"""
                "glob" -> """/${tool.name}
```json
{"pattern": "*.kt", "path": "src"}
```"""
                "shell" -> """/${tool.name}
```json
{"command": "ls -la"}
```"""
                else -> {
                    // For MCP tools or other tools, provide a generic example
                    if (tool.name.contains("_")) {
                        // Likely an MCP tool with server_toolname format
                        """/${tool.name}
```json
{"arguments": {"path": "/tmp"}}
```"""
                    } else {
                        """/${tool.name}
```json
{"parameter": "value"}
```"""
                    }
                }
            }
        }
    }

    /**
     * Generate DevIns-style example with JSON parameters based on schema
     */
    private fun generateDevInsExample(toolName: String, toolType: ToolType): String {
        val jsonSchema = toolType.schema.toJsonSchema()
        val properties = jsonSchema.jsonObject["properties"]?.jsonObject

        if (properties == null || properties.isEmpty()) {
            return """/$toolName
```json
{}
```"""
        }

        // Generate example JSON based on schema properties
        val exampleJson = buildJsonObject {
            properties.forEach { (paramName, paramSchema) ->
                val paramObj = paramSchema.jsonObject
                val type = paramObj["type"]?.jsonPrimitive?.content
                val defaultValue = paramObj["default"]

                when {
                    defaultValue != null -> put(paramName, defaultValue)
                    type == "string" -> {
                        val example = when (paramName) {
                            "path" -> "src/main.kt"
                            "content" -> "Hello, World!"
                            "pattern" -> "*.kt"
                            "command" -> "ls -la"
                            "message" -> "Example message"
                            else -> "example_value"
                        }
                        put(paramName, example)
                    }
                    type == "integer" -> {
                        val example = when (paramName) {
                            "startLine", "endLine" -> 1
                            "maxLines" -> 100
                            "port" -> 8080
                            else -> 42
                        }
                        put(paramName, example)
                    }
                    type == "boolean" -> put(paramName, false)
                    type == "array" -> put(paramName, buildJsonArray { add("example") })
                    else -> put(paramName, JsonPrimitive("example"))
                }
            }
        }

        return """/$toolName
```json
${exampleJson.toString()}
```"""
    }
}

