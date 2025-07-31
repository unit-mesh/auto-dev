package cc.unitmesh.devti.vcs.gitignore

import java.util.concurrent.CopyOnWriteArrayList

/**
 * High-performance custom implementation of IgnoreEngine.
 * This engine uses pre-compiled regex patterns with concurrent caching for optimal performance.
 */
class HomeSpunIgnoreEngine : IgnoreEngine {
    private val rules = CopyOnWriteArrayList<HomeSpunIgnoreRule>()
    private val patternCache = IgnorePatternCache()
    
    override fun isIgnored(filePath: String): Boolean {
        if (rules.isEmpty()) {
            return false
        }
        
        val normalizedPath = normalizeFilePath(filePath)
        var ignored = false
        
        // Process rules in order - later rules can override earlier ones
        for (rule in rules) {
            if (rule.matches(normalizedPath)) {
                ignored = !rule.isNegated()
            }
        }
        
        return ignored
    }
    
    override fun addRule(pattern: String) {
        try {
            val rule = HomeSpunIgnoreRule.fromPattern(pattern, patternCache)
            rules.add(rule)
        } catch (e: InvalidGitIgnorePatternException) {
            // Log the error but don't fail - just skip the invalid pattern
            // In a real implementation, you might want to use a proper logger
            System.err.println("Warning: Skipping invalid gitignore pattern: ${e.message}")
        }
    }
    
    override fun removeRule(pattern: String) {
        rules.removeIf { it.getPattern() == pattern }
        // Also remove from cache to free memory
        patternCache.remove(pattern)
    }
    
    override fun getRules(): List<String> {
        return rules.map { it.getPattern() }
    }
    
    override fun clearRules() {
        rules.clear()
        patternCache.clear()
    }
    
    override fun loadFromContent(gitIgnoreContent: String) {
        clearRules()
        
        val lines = gitIgnoreContent.lines()
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                addRule(trimmedLine)
            }
        }
    }
    
    /**
     * Gets the number of cached patterns for monitoring purposes.
     *
     * @return the cache size
     */
    fun getCacheSize(): Int = patternCache.size()
    
    /**
     * Gets the number of active rules.
     *
     * @return the number of rules
     */
    fun getRuleCount(): Int = rules.size
    
    /**
     * Normalizes a file path for consistent matching.
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
        
        return normalized
    }
    
    /**
     * Gets detailed statistics about the engine for debugging/monitoring.
     *
     * @return a map of statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "ruleCount" to getRuleCount(),
            "cacheSize" to getCacheSize(),
            "cachedPatterns" to patternCache.getCachedPatterns()
        )
    }
}
