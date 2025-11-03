package cc.unitmesh.agent.render

/**
 * Base abstract renderer providing common functionality
 * All specific renderer implementations should extend this class
 */
abstract class BaseRenderer : CodingAgentRenderer {
    protected val reasoningBuffer = StringBuilder()
    protected var isInDevinBlock = false
    protected var lastIterationReasoning = ""
    protected var consecutiveRepeats = 0
    
    /**
     * Common devin block filtering logic
     */
    protected fun filterDevinBlocks(content: String): String {
        var filtered = content
        
        // Remove complete devin blocks
        filtered = filtered.replace(Regex("<devin[^>]*>[\\s\\S]*?</devin>"), "")
        
        // Handle incomplete devin blocks at the end
        val openDevinIndex = filtered.lastIndexOf("<devin")
        if (openDevinIndex != -1) {
            val closeDevinIndex = filtered.indexOf("</devin>", openDevinIndex)
            if (closeDevinIndex == -1) {
                // Incomplete devin block, remove it
                filtered = filtered.substring(0, openDevinIndex)
            }
        }
        
        // Remove partial devin tags
        filtered = filtered.replace(Regex("<de(?:v(?:i(?:n)?)?)?$|<$"), "")
        
        return filtered
    }
    
    /**
     * Check for incomplete devin blocks
     */
    protected fun hasIncompleteDevinBlock(content: String): Boolean {
        val lastOpenDevin = content.lastIndexOf("<devin")
        val lastCloseDevin = content.lastIndexOf("</devin>")
        
        // Check for partial opening tags
        val partialDevinPattern = Regex("<de(?:v(?:i(?:n)?)?)?$|<$")
        val hasPartialTag = partialDevinPattern.containsMatchIn(content)
        
        return lastOpenDevin > lastCloseDevin || hasPartialTag
    }
    
    /**
     * Calculate similarity between two strings for repeat detection
     */
    protected fun calculateSimilarity(str1: String, str2: String): Double {
        if (str1.isEmpty() || str2.isEmpty()) return 0.0
        
        val words1 = str1.lowercase().split(Regex("\\s+"))
        val words2 = str2.lowercase().split(Regex("\\s+"))
        
        val commonWords = words1.intersect(words2.toSet())
        val totalWords = maxOf(words1.size, words2.size)
        
        return if (totalWords > 0) commonWords.size.toDouble() / totalWords else 0.0
    }
    
    /**
     * Clean up excessive newlines in content
     */
    protected fun cleanNewlines(content: String): String {
        return content.replace(Regex("\n{3,}"), "\n\n")
    }
    
    /**
     * Default implementation for LLM response start
     */
    override fun renderLLMResponseStart() {
        reasoningBuffer.clear()
        isInDevinBlock = false
    }
    
    /**
     * Default implementation for LLM response end with similarity checking
     */
    override fun renderLLMResponseEnd() {
        val currentReasoning = reasoningBuffer.toString().trim()
        val similarity = calculateSimilarity(currentReasoning, lastIterationReasoning)
        
        if (similarity > 0.8 && lastIterationReasoning.isNotEmpty()) {
            consecutiveRepeats++
            if (consecutiveRepeats >= 2) {
                renderRepeatAnalysisWarning()
            }
        } else {
            consecutiveRepeats = 0
        }
        
        lastIterationReasoning = currentReasoning
    }
    
    /**
     * Render warning for repetitive analysis - can be overridden by subclasses
     */
    protected open fun renderRepeatAnalysisWarning() {
        renderError("Agent appears to be repeating similar analysis...")
    }
}
