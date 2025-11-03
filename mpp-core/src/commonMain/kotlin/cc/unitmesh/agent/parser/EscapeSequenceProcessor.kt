package cc.unitmesh.agent.parser

/**
 * Utility class for processing escape sequences in tool parameters
 * Handles common escape sequences like \n, \t, \", etc.
 */
object EscapeSequenceProcessor {
    
    /**
     * Process escape sequences in a string
     */
    fun processEscapeSequences(content: String): String {
        return content
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
    }
    
    /**
     * Escape special characters for safe string representation
     */
    fun escapeString(content: String): String {
        return content
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    /**
     * Check if a string contains unescaped quotes
     */
    fun hasUnescapedQuotes(content: String): Boolean {
        var escaped = false
        for (char in content) {
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> return true
            }
        }
        return false
    }
    
    /**
     * Find the closing quote position, handling escaped quotes
     */
    fun findClosingQuote(content: CharArray, startIndex: Int): Int {
        var escaped = false
        for (i in startIndex until content.size) {
            when {
                escaped -> escaped = false
                content[i] == '\\' -> escaped = true
                content[i] == '"' -> return i
            }
        }
        return -1
    }
}
