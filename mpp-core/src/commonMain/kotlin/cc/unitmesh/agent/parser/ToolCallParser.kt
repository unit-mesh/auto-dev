package cc.unitmesh.agent.parser

import cc.unitmesh.agent.state.ToolCall

/**
 * Parser for extracting tool calls from LLM responses
 * Handles both DevIn blocks and direct tool call formats
 */
class ToolCallParser {
    private val devinParser = DevinBlockParser()
    private val escapeProcessor = EscapeSequenceProcessor
    
    /**
     * Parse all tool calls from LLM response
     */
    fun parseToolCalls(llmResponse: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        
        // First try to extract from DevIn blocks
        val devinBlocks = devinParser.extractDevinBlocks(llmResponse)
        
        if (devinBlocks.isEmpty()) {
            // No DevIn blocks, try direct parsing
            val directCall = parseDirectToolCall(llmResponse)
            if (directCall != null) {
                toolCalls.add(directCall)
            }
        } else {
            // Parse from first DevIn block only (as per original logic)
            val firstBlock = devinBlocks.firstOrNull()
            if (firstBlock != null) {
                val toolCall = parseToolCallFromDevinBlock(firstBlock)
                if (toolCall != null) {
                    toolCalls.add(toolCall)
                }
            }
        }
        
        return toolCalls
    }
    
    /**
     * Parse DevIn blocks from content
     */
    fun parseDevinBlocks(content: String): List<DevinBlock> {
        return devinParser.extractDevinBlocks(content)
    }
    
    /**
     * Parse a tool call from a DevIn block
     */
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
        
        // Parse key="value" parameters (including multiline values)
        if (rest.contains("=\"")) {
            parseKeyValueParameters(rest, params)
        } else if (rest.isNotEmpty()) {
            // Format 2: /shell\ncommand or /tool\ncontent
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
        } else {
            // Other tools: try to extract first line as main parameter
            val firstLine = rest.lines().firstOrNull()?.trim()
            if (firstLine != null && firstLine.isNotEmpty()) {
                val defaultParamName = when (toolName) {
                    "read-file", "write-file" -> "path"
                    "glob", "grep" -> "pattern"
                    else -> "content"
                }
                params[defaultParamName] = escapeProcessor.processEscapeSequences(firstLine)
            }
        }
    }
}
