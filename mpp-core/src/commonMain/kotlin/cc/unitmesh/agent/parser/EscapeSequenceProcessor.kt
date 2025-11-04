package cc.unitmesh.agent.parser

/**
 * Utility class for processing escape sequences in tool parameters
 * Handles common escape sequences like \n, \t, \", etc.
 */
object EscapeSequenceProcessor {
    
    /**
     * Process escape sequences in a string
     * Order matters: process \\\\ first to avoid double processing
     */
    fun processEscapeSequences(content: String): String {
        var result = content

        // Process double backslash first to avoid conflicts
        result = result.replace("\\\\", "\u0001") // Temporary placeholder

        // Process other escape sequences
        result = result.replace("\\n", "\n")
        result = result.replace("\\r", "\r")
        result = result.replace("\\t", "\t")
        result = result.replace("\\\"", "\"")
        result = result.replace("\\'", "'")

        // Restore single backslashes
        result = result.replace("\u0001", "\\")

        return result
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
