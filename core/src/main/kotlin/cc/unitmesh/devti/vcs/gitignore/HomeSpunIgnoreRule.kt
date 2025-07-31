package cc.unitmesh.devti.vcs.gitignore

/**
 * Implementation of IgnoreRule for the custom high-performance gitignore engine.
 * This class represents a single gitignore rule with its pattern and matching logic.
 */
class HomeSpunIgnoreRule(
    private val originalPattern: String,
    private val matcher: ThreadSafeMatcher,
    private val negated: Boolean = false
) : IgnoreRule {
    
    companion object {
        /**
         * Creates an IgnoreRule from a gitignore pattern string.
         *
         * @param pattern the gitignore pattern
         * @param cache the pattern cache to use for compilation
         * @return a new IgnoreRule instance
         * @throws InvalidGitIgnorePatternException if the pattern is invalid
         */
        fun fromPattern(pattern: String, cache: IgnorePatternCache): HomeSpunIgnoreRule {
            val trimmedPattern = pattern.trim()
            
            // Skip empty lines and comments
            if (trimmedPattern.isEmpty() || trimmedPattern.startsWith("#")) {
                // Return a rule that never matches
                return HomeSpunIgnoreRule(
                    originalPattern = trimmedPattern,
                    matcher = cache.getOrCompile("^$"), // Never matches
                    negated = false
                )
            }
            
            // Check if this is a negated rule
            val isNegated = trimmedPattern.startsWith("!")
            val patternToCompile = if (isNegated) trimmedPattern.substring(1) else trimmedPattern
            
            val compiledMatcher = cache.getOrCompile(patternToCompile)
            
            return HomeSpunIgnoreRule(
                originalPattern = trimmedPattern,
                matcher = compiledMatcher,
                negated = isNegated
            )
        }
    }
    
    override fun matches(filePath: String): Boolean {
        // Skip empty or comment patterns
        if (originalPattern.isEmpty() || originalPattern.startsWith("#")) {
            return false
        }
        
        // Normalize the file path for consistent matching
        val normalizedPath = normalizeFilePath(filePath)
        
        return matcher.matches(normalizedPath)
    }
    
    override fun getPattern(): String = originalPattern
    
    override fun isNegated(): Boolean = negated
    
    /**
     * Normalizes a file path for consistent matching across platforms.
     *
     * @param filePath the file path to normalize
     * @return the normalized file path
     */
    private fun normalizeFilePath(filePath: String): String {
        var normalized = filePath.trim()
        
        // Convert backslashes to forward slashes for consistency
        normalized = normalized.replace('\\', '/')
        
        // Remove leading slash if present (make relative)
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1)
        }
        
        // Remove trailing slash for files (keep for directories if needed)
        if (normalized.endsWith("/") && normalized.length > 1) {
            normalized = normalized.dropLast(1)
        }
        
        return normalized
    }
    
    override fun toString(): String {
        return "HomeSpunIgnoreRule(pattern='$originalPattern', negated=$negated)"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HomeSpunIgnoreRule) return false
        
        return originalPattern == other.originalPattern && negated == other.negated
    }
    
    override fun hashCode(): Int {
        var result = originalPattern.hashCode()
        result = 31 * result + negated.hashCode()
        return result
    }
}
