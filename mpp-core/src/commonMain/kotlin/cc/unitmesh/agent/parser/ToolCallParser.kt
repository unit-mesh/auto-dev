package cc.unitmesh.agent.parser

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.tool.ToolType

/**
 * Parser for extracting tool calls from LLM responses
 * Handles both DevIn blocks and direct tool call formats
 */
class ToolCallParser {
    private val logger = getLogger("ToolCallParser")
    private val devinParser = DevinBlockParser()
    private val escapeProcessor = EscapeSequenceProcessor

    /**
     * Parse all tool calls from LLM response
     * Now supports multiple tool calls for parallel execution
     * 
     * IMPORTANT: Only parses tool calls within <devin> blocks to avoid false positives
     * from natural language text (e.g., "/blog/" API paths, "/Hibernate" in sentences)
     */
    fun parseToolCalls(llmResponse: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()

        // Only extract from devin blocks - do NOT parse direct tool calls from plain text
        val devinBlocks = devinParser.extractDevinBlocks(llmResponse)

        // Parse all devin blocks (not just the first one)
        for (block in devinBlocks) {
            val toolCall = parseToolCallFromDevinBlock(block)
            if (toolCall != null) {
                if (toolCall.toolName == ToolType.WriteFile.name && !toolCall.params.containsKey("content")) {
                    val contentFromContext = extractContentFromContext(llmResponse, block)
                    if (contentFromContext != null) {
                        val updatedParams = toolCall.params.toMutableMap()
                        updatedParams["content"] = contentFromContext
                        toolCalls.add(ToolCall.create(toolCall.toolName, updatedParams))
                    } else {
                        toolCalls.add(toolCall)
                    }
                } else {
                    toolCalls.add(toolCall)
                }
            }
        }

        logger.debug { "Parsed ${toolCalls.size} tool call(s) from LLM response (from ${devinBlocks.size} devin block(s))" }
        return toolCalls
    }

    private fun parseToolCallFromDevinBlock(block: DevinBlock): ToolCall? {
        val lines = block.content.lines()
        var toolCallLine: String? = null
        var jsonStartIndex = -1

        // Find the tool call line
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check if this line starts with a tool call
            if (trimmed.startsWith("/")) {
                toolCallLine = trimmed
                jsonStartIndex = index + 1
                break
            }
        }

        if (toolCallLine == null) return null

        // Check if there's JSON content after the tool call
        val jsonLines = mutableListOf<String>()
        var inJsonBlock = false

        for (i in jsonStartIndex until lines.size) {
            val line = lines[i].trim()
            // Handle both "```json" on its own line and "```json{...}" on single line
            if (line == "```json") {
                inJsonBlock = true
                continue
            } else if (line.startsWith("```json") && line.length > 7) {
                // Handle format like: ```json{"pattern": "..."}
                val jsonContent = line.removePrefix("```json").removeSuffix("```")
                if (jsonContent.isNotEmpty()) {
                    jsonLines.add(jsonContent)
                    // If the line also ends with ```, we're done
                    if (line.endsWith("```")) {
                        break
                    }
                    inJsonBlock = true
                }
                continue
            } else if (line == "```") {
                break
            } else if (inJsonBlock) {
                // Handle content that might end with ```
                if (line.endsWith("```")) {
                    jsonLines.add(line.removeSuffix("```"))
                    break
                }
                jsonLines.add(lines[i]) // Keep original indentation for JSON
            }
        }

        return if (jsonLines.isNotEmpty()) {
            parseToolCallWithJson(toolCallLine, jsonLines.joinToString("\n"))
        } else {
            parseToolCallFromLine(toolCallLine)
        }
    }
    
    private fun parseToolCallFromLine(line: String): ToolCall? {
        val toolPattern = Regex("""/(\w+(?:-\w+)*)(.*)""")
        val match = toolPattern.find(line) ?: return null
        
        val toolName = match.groups[1]?.value ?: return null
        val rest = match.groups[2]?.value?.trim() ?: ""
        
        val params = parseParameters(toolName, rest)
        
        return ToolCall.create(toolName, params)
    }

    /**
     * Parse tool call with JSON parameters
     */
    private fun parseToolCallWithJson(toolCallLine: String, jsonContent: String): ToolCall? {
        val toolPattern = Regex("""/(\w+(?:-\w+)*)(.*)""")
        val match = toolPattern.find(toolCallLine) ?: return null

        val toolName = match.groups[1]?.value ?: return null

        return try {
            // Parse JSON content
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(jsonContent)
            val params = mutableMapOf<String, Any>()

            if (jsonElement is kotlinx.serialization.json.JsonObject) {
                for ((key, value) in jsonElement) {
                    params[key] = when (value) {
                        is kotlinx.serialization.json.JsonPrimitive -> {
                            when {
                                value.isString -> value.content
                                value.content == "true" -> true
                                value.content == "false" -> false
                                value.content.toIntOrNull() != null -> value.content.toInt()
                                value.content.toDoubleOrNull() != null -> value.content.toDouble()
                                else -> value.content
                            }
                        }
                        is kotlinx.serialization.json.JsonArray -> value.toString()
                        is kotlinx.serialization.json.JsonObject -> value.toString()
                        else -> value.toString()
                    }
                }
            }

            ToolCall.create(toolName, params)
        } catch (e: Exception) {
            logger.warn(e) { "⚠️ Failed to parse JSON parameters for tool $toolName: ${e.message}" }
            // Fallback to line parsing
            parseToolCallFromLine(toolCallLine)
        }
    }

    private fun parseParameters(toolName: String, rest: String): Map<String, Any> {
        val params = mutableMapOf<String, Any>()

        if (rest.contains("=\"")) {
            parseKeyValueParameters(rest, params)
        } else if (rest.isNotEmpty()) {
            parseSimpleParameter(toolName, rest, params)
        }

        return params
    }

    private fun parseKeyValueParameters(rest: String, params: MutableMap<String, Any>) {
        // Use regex-based parsing for better handling of complex content
        val paramPattern = Regex("""(\w+)="([^"\\]*(?:\\.[^"\\]*)*)"(?:\s|$)""")
        val matches = paramPattern.findAll(rest)

        for (match in matches) {
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            params[key] = escapeProcessor.processEscapeSequences(value)
        }

        // Fallback to character-by-character parsing for edge cases
        if (params.isEmpty()) {
            parseKeyValueParametersCharByChar(rest, params)
        }
    }

    private fun parseKeyValueParametersCharByChar(rest: String, params: MutableMap<String, Any>) {
        val remaining = rest.toCharArray().toList()
        var i = 0

        while (i < remaining.size) {
            // Skip whitespace
            while (i < remaining.size && remaining[i].isWhitespace()) i++
            if (i >= remaining.size) break

            // Find key
            val keyStart = i
            while (i < remaining.size && remaining[i] != '=') i++
            if (i >= remaining.size) break

            val key = remaining.subList(keyStart, i).joinToString("").trim()
            i++ // skip '='

            // Skip whitespace after =
            while (i < remaining.size && remaining[i].isWhitespace()) i++
            if (i >= remaining.size || remaining[i] != '"') {
                i++
                continue
            }

            i++ // skip opening quote
            val valueStart = i

            // Find closing quote (handle escaped quotes and newlines)
            var escaped = false
            var depth = 0
            while (i < remaining.size) {
                when {
                    escaped -> escaped = false
                    remaining[i] == '\\' -> escaped = true
                    remaining[i] == '"' -> {
                        // Check if this is really the end quote
                        if (depth == 0) break
                    }
                }
                i++
            }

            if (i > valueStart && key.isNotEmpty()) {
                val value = remaining.subList(valueStart, i).joinToString("")
                params[key] = escapeProcessor.processEscapeSequences(value)
            }

            i++ // skip closing quote
        }
    }
    
    /**
     * Parse simple parameter (single value without key)
     */
    private fun parseSimpleParameter(toolName: String, rest: String, params: MutableMap<String, Any>) {
        if (toolName == ToolType.Shell.name) {
            params["command"] = escapeProcessor.processEscapeSequences(rest.trim())
        } else if (toolName == ToolType.WriteFile.name) {
            val firstLine = rest.lines().firstOrNull()?.trim()
            if (firstLine != null && firstLine.isNotEmpty()) {
                params["path"] = escapeProcessor.processEscapeSequences(firstLine)
                // For write-file without explicit content, we'll need to handle this in the orchestrator
                // by prompting the LLM to provide the content
            }
        } else {
            // Other tools: try to extract first line as main parameter
            val firstLine = rest.lines().firstOrNull()?.trim()
            if (firstLine != null && firstLine.isNotEmpty()) {
                val defaultParamName = when (toolName) {
                    ToolType.ReadFile.name -> "path"
                    ToolType.Glob.name, ToolType.Grep.name -> "pattern"
                    ToolType.WebFetch.name -> "prompt"
                    else -> "content"
                }
                params[defaultParamName] = escapeProcessor.processEscapeSequences(firstLine)
            }
        }
    }

    /**
     * Extract content from the LLM response context for write-file operations
     * This looks for code blocks or content that appears to be intended for the file
     */
    private fun extractContentFromContext(llmResponse: String, devinBlock: DevinBlock): String? {
        // First, try to extract content from within the devin block itself
        val blockContent = devinBlock.content
        val lines = blockContent.lines()
        val toolCallLineIndex = lines.indexOfFirst { it.trim().startsWith("/write-file") }

        if (toolCallLineIndex >= 0) {
            // Check if the write-file command already has content parameter
            val toolCallLine = lines[toolCallLineIndex].trim()
            val contentMatch = Regex("""content="([^"\\]*(?:\\.[^"\\]*)*)"(?:\s|$)""").find(toolCallLine)
            if (contentMatch != null) {
                // Content is already in the command line, don't override it
                return null
            }

            // Look for content after the tool call line within the same devin block
            if (toolCallLineIndex < lines.size - 1) {
                val contentLines = lines.subList(toolCallLineIndex + 1, lines.size)
                val content = contentLines.joinToString("\n").trim()
                if (content.isNotEmpty()) {
                    return content
                }
            }
        }

        // Look for code blocks after the devin block
        val afterBlock = llmResponse.substring(devinBlock.endOffset)
        val codeBlockRegex = Regex("```(?:\\w+)?\\s*\\n([\\s\\S]*?)\\n```", RegexOption.MULTILINE)
        val codeMatch = codeBlockRegex.find(afterBlock)
        if (codeMatch != null) {
            return codeMatch.groupValues[1].trim()
        }

        // Look for content in the LLM response before the devin block
        val beforeBlock = llmResponse.substring(0, devinBlock.startOffset)
        val beforeCodeMatch = codeBlockRegex.find(beforeBlock)
        if (beforeCodeMatch != null) {
            return beforeCodeMatch.groupValues[1].trim()
        }

        return null
    }
}
