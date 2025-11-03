package cc.unitmesh.agent.parser

import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.tool.ToolType

/**
 * Parser for extracting tool calls from LLM responses
 * Handles both DevIn blocks and direct tool call formats
 */
class ToolCallParser {
    private val devinParser = DevinBlockParser()
    private val escapeProcessor = EscapeSequenceProcessor

    fun parseToolCalls(llmResponse: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()

        val devinBlocks = devinParser.extractDevinBlocks(llmResponse)

        if (devinBlocks.isEmpty()) {
            val directCall = parseDirectToolCall(llmResponse)
            if (directCall != null) {
                toolCalls.add(directCall)
            }
        } else {
            val firstBlock = devinBlocks.firstOrNull()
            if (firstBlock != null) {
                val toolCall = parseToolCallFromDevinBlock(firstBlock)
                if (toolCall != null) {
                    // For write-file tools, try to extract content from the surrounding context
                    if (toolCall.toolName == "write-file" && !toolCall.params.containsKey("content")) {
                        val contentFromContext = extractContentFromContext(llmResponse, firstBlock)
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
        }

        return toolCalls
    }

    fun parseDevinBlocks(content: String): List<DevinBlock> {
        return devinParser.extractDevinBlocks(content)
    }

    private fun parseToolCallFromDevinBlock(block: DevinBlock): ToolCall? {
        val lines = block.content.lines()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            // Check if this line starts with a tool call
            if (trimmed.startsWith("/")) {
                return parseToolCallFromLine(trimmed)
            }
        }
        
        return null
    }
    
    /**
     * Parse a direct tool call (without DevIn blocks)
     */
    private fun parseDirectToolCall(response: String): ToolCall? {
        val toolPattern = Regex("""/(\w+(?:-\w+)*)(.*)""", RegexOption.MULTILINE)
        val match = toolPattern.find(response) ?: return null
        
        val toolName = match.groups[1]?.value ?: return null
        val rest = match.groups[2]?.value?.trim() ?: ""
        
        return parseToolCallFromLine("/$toolName $rest")
    }
    
    /**
     * Parse a tool call from a single line
     */
    private fun parseToolCallFromLine(line: String): ToolCall? {
        val toolPattern = Regex("""/(\w+(?:-\w+)*)(.*)""")
        val match = toolPattern.find(line) ?: return null
        
        val toolName = match.groups[1]?.value ?: return null
        val rest = match.groups[2]?.value?.trim() ?: ""
        
        val params = parseParameters(toolName, rest)
        
        return ToolCall.create(toolName, params)
    }
    
    /**
     * Parse parameters from the rest of the tool call line
     */
    private fun parseParameters(toolName: String, rest: String): Map<String, Any> {
        val params = mutableMapOf<String, Any>()
        
        if (rest.contains("=\"")) {
            parseKeyValueParameters(rest, params)
        } else if (rest.isNotEmpty()) {
            parseSimpleParameter(toolName, rest, params)
        }
        
        return params
    }
    
    /**
     * Parse key="value" style parameters
     */
    private fun parseKeyValueParameters(rest: String, params: MutableMap<String, Any>) {
        val remaining = rest.toCharArray().toList()
        var i = 0
        
        while (i < remaining.size) {
            // Find key
            val keyStart = i
            while (i < remaining.size && remaining[i] != '=') i++
            if (i >= remaining.size) break
            
            val key = remaining.subList(keyStart, i).joinToString("").trim()
            i++ // skip '='
            
            if (i >= remaining.size || remaining[i] != '"') {
                i++
                continue
            }
            
            i++ // skip opening quote
            val valueStart = i
            
            // Find closing quote (handle escaped quotes)
            var escaped = false
            while (i < remaining.size) {
                when {
                    escaped -> escaped = false
                    remaining[i] == '\\' -> escaped = true
                    remaining[i] == '"' -> break
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
        if (toolName == "shell") {
            params["command"] = escapeProcessor.processEscapeSequences(rest.trim())
        } else if (toolName == "write-file") {
            // For write-file, if only one parameter is provided, it's the path
            // The content should be provided in a separate parameter or in the LLM context
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
        // Look for code blocks after the devin block
        val afterBlock = llmResponse.substring(devinBlock.endOffset)

        // Try to find code blocks with ```
        val codeBlockRegex = Regex("```(?:\\w+)?\\s*\\n([\\s\\S]*?)\\n```", RegexOption.MULTILINE)
        val codeMatch = codeBlockRegex.find(afterBlock)
        if (codeMatch != null) {
            return codeMatch.groupValues[1].trim()
        }

        // Look for content in the same devin block after the tool call
        val blockContent = devinBlock.content
        val lines = blockContent.lines()
        val toolCallLineIndex = lines.indexOfFirst { it.trim().startsWith("/write-file") }

        if (toolCallLineIndex >= 0 && toolCallLineIndex < lines.size - 1) {
            // Get content after the tool call line
            val contentLines = lines.subList(toolCallLineIndex + 1, lines.size)
            val content = contentLines.joinToString("\n").trim()
            if (content.isNotEmpty()) {
                return content
            }
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
