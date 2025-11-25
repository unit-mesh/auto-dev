package cc.unitmesh.agent.tool.gitignore

/**
 * Cross-platform GitIgnore filter interface
 * Determines if files should be ignored based on .gitignore rules
 */
interface GitIgnoreFilter {
    /**
     * Check if a file path should be ignored
     * @param filePath Relative file path from project root
     * @return true if the file should be ignored
     */
    fun isIgnored(filePath: String): Boolean
    
    /**
     * Add a gitignore pattern
     * @param pattern The gitignore pattern to add
     */
    fun addPattern(pattern: String)
    
    /**
     * Clear all patterns
     */
    fun clearPatterns()
    
    /**
     * Get all current patterns
     */
    fun getPatterns(): List<String>
}

/**
 * GitIgnore pattern matcher
 * Implements gitignore pattern matching rules
 */
class GitIgnorePatternMatcher {
    
    companion object {
        /**
         * Convert a gitignore pattern to a regex pattern
         * Based on gitignore specification:
         * - * matches anything except /
         * - ** matches zero or more directories
         * - ? matches any one character except /
         * - [abc] matches one character in the set
         * - ! negates the pattern
         * - / at start anchors to base directory
         * - / at end matches only directories
         */
        fun patternToRegex(pattern: String): Pair<Regex, Boolean> {
            var p = pattern.trim()
            
            // Empty or comment lines
            if (p.isEmpty() || p.startsWith("#")) {
                return Regex("^$") to false
            }
            
            // Check for negation
            val isNegated = p.startsWith("!")
            if (isNegated) {
                p = p.substring(1).trim()
            }
            
            // Check if pattern is anchored to root
            val isAnchored = p.startsWith("/")
            if (isAnchored) {
                p = p.substring(1)
            }
            
            // Check if pattern matches only directories
            val dirOnly = p.endsWith("/")
            if (dirOnly) {
                p = p.substring(0, p.length - 1)
            }
            
            // Build regex pattern
            val regexBuilder = StringBuilder()
            
            if (isAnchored) {
                regexBuilder.append("^")
            } else {
                // If not anchored, can match at any level
                regexBuilder.append("(^|.*/)")
            }
            
            var i = 0
            while (i < p.length) {
                when {
                    // Handle **
                    i + 1 < p.length && p[i] == '*' && p[i + 1] == '*' -> {
                        // Check if it's **/ or **/
                        if (i + 2 < p.length && p[i + 2] == '/') {
                            regexBuilder.append("(.*/)??")
                            i += 3
                        } else if (i == 0 || p[i - 1] == '/') {
                            regexBuilder.append(".*")
                            i += 2
                        } else {
                            regexBuilder.append("[^/]*")
                            i++
                        }
                    }
                    // Handle single *
                    p[i] == '*' -> {
                        regexBuilder.append("[^/]*")
                        i++
                    }
                    // Handle ?
                    p[i] == '?' -> {
                        regexBuilder.append("[^/]")
                        i++
                    }
                    // Handle character class [abc]
                    p[i] == '[' -> {
                        val endBracket = p.indexOf(']', i)
                        if (endBracket != -1) {
                            regexBuilder.append(p.substring(i, endBracket + 1))
                            i = endBracket + 1
                        } else {
                            regexBuilder.append("\\[")
                            i++
                        }
                    }
                    // Escape special regex characters
                    p[i] in ".+(){}|^$\\" -> {
                        regexBuilder.append("\\").append(p[i])
                        i++
                    }
                    else -> {
                        regexBuilder.append(p[i])
                        i++
                    }
                }
            }
            
            if (dirOnly) {
                // For directory-only patterns, require trailing slash or subdirectory
                regexBuilder.append("(/|/.*)$")
            } else {
                // Match the pattern itself OR anything inside it (for directories)
                // This ensures ".intellijPlatform" matches both:
                // - ".intellijPlatform" (the directory itself)
                // - ".intellijPlatform/file.xml" (files inside the directory)
                regexBuilder.append("(/.*)?$")
            }
            
            return Regex(regexBuilder.toString()) to isNegated
        }
        
        /**
         * Normalize file path for matching
         * - Convert backslashes to forward slashes
         * - Remove leading ./
         * - Keep trailing / to indicate directories
         */
        fun normalizePath(path: String): String {
            var normalized = path.replace('\\', '/')

            // Remove leading ./
            if (normalized.startsWith("./")) {
                normalized = normalized.substring(2)
            }

            // Remove leading /
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1)
            }

            return normalized
        }
    }
}

/**
 * Default GitIgnore filter implementation
 * Uses pattern matching without file system access
 */
class DefaultGitIgnoreFilter : GitIgnoreFilter {
    private val patterns = mutableListOf<Pair<Regex, Boolean>>()
    private val rawPatterns = mutableListOf<String>()
    
    override fun isIgnored(filePath: String): Boolean {
        val normalizedPath = GitIgnorePatternMatcher.normalizePath(filePath)
        
        if (normalizedPath.isEmpty()) {
            return false
        }
        
        var ignored = false
        
        // Process patterns in order - later patterns can override earlier ones
        for ((regex, isNegated) in patterns) {
            if (regex.matches(normalizedPath)) {
                ignored = !isNegated
            }
        }
        
        return ignored
    }
    
    override fun addPattern(pattern: String) {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return
        }
        
        rawPatterns.add(trimmed)
        val (regex, isNegated) = GitIgnorePatternMatcher.patternToRegex(trimmed)
        patterns.add(regex to isNegated)
    }
    
    override fun clearPatterns() {
        patterns.clear()
        rawPatterns.clear()
    }
    
    override fun getPatterns(): List<String> {
        return rawPatterns.toList()
    }
}

/**
 * Parse gitignore file content and create a filter
 */
fun parseGitIgnoreContent(content: String): GitIgnoreFilter {
    val filter = DefaultGitIgnoreFilter()
    
    content.lines().forEach { line ->
        filter.addPattern(line)
    }
    
    return filter
}

