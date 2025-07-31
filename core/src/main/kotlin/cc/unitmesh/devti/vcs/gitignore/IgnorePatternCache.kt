package cc.unitmesh.devti.vcs.gitignore

import java.util.concurrent.ConcurrentHashMap

/**
 * A thread-safe cache for compiled gitignore patterns.
 * This cache improves performance by avoiding redundant pattern compilation.
 */
class IgnorePatternCache {
    private val patternCache = ConcurrentHashMap<String, ThreadSafeMatcher>()
    
    /**
     * Gets a compiled pattern from cache or compiles and caches it if not present.
     *
     * @param pattern the gitignore pattern to compile
     * @return a ThreadSafeMatcher for the pattern
     * @throws InvalidGitIgnorePatternException if the pattern cannot be compiled
     */
    fun getOrCompile(pattern: String): ThreadSafeMatcher {
        return patternCache.computeIfAbsent(pattern) { compilePattern(it) }
    }
    
    /**
     * Removes a pattern from the cache.
     *
     * @param pattern the pattern to remove
     */
    fun remove(pattern: String) {
        patternCache.remove(pattern)
    }
    
    /**
     * Clears all cached patterns.
     */
    fun clear() {
        patternCache.clear()
    }
    
    /**
     * Gets the current cache size.
     *
     * @return number of cached patterns
     */
    fun size(): Int = patternCache.size
    
    /**
     * Checks if a pattern is cached.
     *
     * @param pattern the pattern to check
     * @return true if the pattern is cached, false otherwise
     */
    fun contains(pattern: String): Boolean = patternCache.containsKey(pattern)
    
    /**
     * Gets all cached pattern strings.
     *
     * @return set of all cached pattern strings
     */
    fun getCachedPatterns(): Set<String> = patternCache.keys.toSet()
    
    private fun compilePattern(pattern: String): ThreadSafeMatcher {
        val compiledPattern = PatternConverter.compilePattern(pattern)
        return ThreadSafeMatcher(compiledPattern)
    }
}
