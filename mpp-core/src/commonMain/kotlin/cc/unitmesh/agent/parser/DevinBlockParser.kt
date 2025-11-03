package cc.unitmesh.agent.parser

/**
 * Parser for DevIn blocks in LLM responses
 * Handles extraction and processing of <devin>...</devin> blocks
 */
class DevinBlockParser {
    
    /**
     * Extract all DevIn blocks from text
     */
    fun extractDevinBlocks(text: String): List<DevinBlock> {
        val blocks = mutableListOf<DevinBlock>()
        val devinRegex = Regex("<devin>([\\s\\S]*?)</devin>", RegexOption.MULTILINE)
        val matches = devinRegex.findAll(text)
        
        for ((index, match) in matches.withIndex()) {
            val content = match.groupValues[1].trim()
            val startOffset = match.range.first
            val endOffset = match.range.last
            
            blocks.add(DevinBlock(
                id = "devin_block_$index",
                content = content,
                startOffset = startOffset,
                endOffset = endOffset,
                rawMatch = match.value
            ))
        }
        
        return blocks
    }
    
    /**
     * Check if text contains incomplete DevIn blocks
     */
    fun hasIncompleteDevinBlock(text: String): Boolean {
        val openTags = text.count { it == '<' && text.indexOf("<devin>", text.indexOf(it)) == text.indexOf(it) }
        val closeTags = text.count { it == '<' && text.indexOf("</devin>", text.indexOf(it)) == text.indexOf(it) }
        return openTags > closeTags
    }
    
    /**
     * Remove DevIn blocks from text, leaving only the reasoning content
     */
    fun filterDevinBlocks(text: String): String {
        val devinRegex = Regex("<devin>([\\s\\S]*?)</devin>", RegexOption.MULTILINE)
        return devinRegex.replace(text, "")
    }
    
    /**
     * Extract the first tool call from DevIn blocks
     */
    fun extractFirstToolCall(blocks: List<DevinBlock>): ToolCallInfo? {
        for (block in blocks) {
            val toolCall = parseToolCallFromBlock(block)
            if (toolCall != null) {
                return toolCall
            }
        }
        return null
    }
    
    /**
     * Parse a tool call from a DevIn block
     */
    private fun parseToolCallFromBlock(block: DevinBlock): ToolCallInfo? {
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
     * Parse a tool call from a single line
     */
    private fun parseToolCallFromLine(line: String): ToolCallInfo? {
        val toolPattern = Regex("""/(\w+(?:-\w+)*)(.*)""")
        val match = toolPattern.find(line) ?: return null
        
        val toolName = match.groups[1]?.value ?: return null
        val rest = match.groups[2]?.value?.trim() ?: ""
        
        return ToolCallInfo(
            toolName = toolName,
            rawParams = rest,
            originalLine = line
        )
    }
}

/**
 * Represents a DevIn block extracted from text
 */
data class DevinBlock(
    val id: String,
    val content: String,
    val startOffset: Int,
    val endOffset: Int,
    val rawMatch: String
)

/**
 * Represents a tool call extracted from a DevIn block
 */
data class ToolCallInfo(
    val toolName: String,
    val rawParams: String,
    val originalLine: String
)
