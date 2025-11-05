package cc.unitmesh.indexer.utils

/**
 * Simple token counter for estimating token usage in LLM context.
 * Uses a simplified heuristic approach that works across platforms.
 */
class TokenCounter {
    
    /**
     * Estimate token count for a given text.
     * Uses a simple heuristic: roughly 4 characters per token for English text.
     * This is a conservative estimate that works reasonably well for code identifiers.
     */
    fun countTokens(text: String): Int {
        if (text.isEmpty()) return 0
        
        // For code identifiers, we use a more conservative estimate
        // since they tend to be more dense than natural language
        val baseCount = (text.length / 3.5).toInt().coerceAtLeast(1)
        
        // Adjust for special patterns in code
        var adjustment = 0
        
        // CamelCase words tend to be more token-dense
        val uppercaseCount = text.count { it.isUpperCase() }
        if (uppercaseCount > 1) {
            adjustment += (uppercaseCount * 0.3).toInt()
        }
        
        // Numbers and special characters
        val digitCount = text.count { it.isDigit() }
        val specialCharCount = text.count { !it.isLetterOrDigit() }
        adjustment += (digitCount * 0.2).toInt()
        adjustment += (specialCharCount * 0.5).toInt()
        
        return baseCount + adjustment
    }
    
    /**
     * Estimate token count for a list of strings
     */
    fun countTokens(texts: List<String>): Int {
        return texts.sumOf { countTokens(it) }
    }
    
    /**
     * Check if adding more text would exceed the token limit
     */
    fun wouldExceedLimit(currentTokens: Int, additionalText: String, limit: Int): Boolean {
        return currentTokens + countTokens(additionalText) > limit
    }
    
    companion object {
        val DEFAULT = TokenCounter()
        
        /**
         * Quick estimate for very short identifiers (single words)
         */
        fun quickEstimate(text: String): Int {
            return when {
                text.isEmpty() -> 0
                text.length <= 3 -> 1
                text.length <= 8 -> 2
                text.length <= 15 -> 3
                else -> (text.length / 4).coerceAtLeast(1)
            }
        }
    }
}
