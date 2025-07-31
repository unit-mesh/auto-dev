package cc.unitmesh.devti.vcs.gitignore

import java.util.regex.Pattern

/**
 * Converts gitignore patterns to regular expressions.
 * Handles all gitignore pattern features including wildcards, negation, and directory patterns.
 */
object PatternConverter {
    
    /**
     * Converts a gitignore pattern to a regular expression string.
     *
     * @param gitignorePattern the original gitignore pattern
     * @return the equivalent regular expression pattern
     * @throws InvalidGitIgnorePatternException if the pattern is malformed
     */
    fun convertToRegex(gitignorePattern: String): String {
        try {
            var pattern = gitignorePattern.trim()
            
            // Skip empty lines and comments
            if (pattern.isEmpty() || pattern.startsWith("#")) {
                return "^$" // Never matches anything
            }
            
            // Handle negated rules (remove the ! prefix, caller should handle negation logic)
            if (pattern.startsWith("!")) {
                pattern = pattern.substring(1)
            }
            
            // Escape special regex characters except for gitignore wildcards
            pattern = escapeSpecialCharacters(pattern)
            
            // Convert gitignore wildcards to regex wildcards
            pattern = handleWildcards(pattern)
            
            // Normalize path separators for cross-platform consistency
            pattern = normalizePathSeparators(pattern)
            
            // Handle directory patterns (ending with /)
            pattern = handleDirectoryPatterns(pattern)
            
            // Handle patterns that should match from root vs anywhere
            pattern = handleRootPatterns(pattern)
            
            return pattern
        } catch (e: Exception) {
            throw InvalidGitIgnorePatternException(gitignorePattern, "Failed to convert pattern to regex", e)
        }
    }
    
    /**
     * Compiles a gitignore pattern to a compiled Pattern object.
     *
     * @param gitignorePattern the gitignore pattern to compile
     * @return compiled Pattern object
     * @throws InvalidGitIgnorePatternException if the pattern cannot be compiled
     */
    fun compilePattern(gitignorePattern: String): Pattern {
        val regexPattern = convertToRegex(gitignorePattern)
        return try {
            Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            throw InvalidGitIgnorePatternException(gitignorePattern, "Failed to compile regex pattern: $regexPattern", e)
        }
    }
    
    private fun escapeSpecialCharacters(pattern: String): String {
        // Escape regex special characters but preserve gitignore wildcards
        return pattern
            .replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("+", "\\+")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("|", "\\|")
    }
    
    private fun handleWildcards(pattern: String): String {
        var result = pattern
        
        // Handle ** (matches zero or more directories)
        result = result.replace("**/", "(?:.*/)?")
        result = result.replace("/**", "(?:/.*)?")
        result = result.replace("**", ".*")
        
        // Handle * (matches any characters except path separator)
        result = result.replace("*", "[^/]*")
        
        // Handle ? (matches any single character except path separator)
        result = result.replace("?", "[^/]")
        
        return result
    }
    
    private fun normalizePathSeparators(pattern: String): String {
        // Convert backslashes to forward slashes for consistency
        return pattern.replace("\\\\", "/")
    }
    
    private fun handleDirectoryPatterns(pattern: String): String {
        return if (pattern.endsWith("/")) {
            // Directory pattern - match the directory and anything inside it
            pattern.dropLast(1) + "(?:/.*)?$"
        } else {
            // File pattern - exact match
            "$pattern$"
        }
    }
    
    private fun handleRootPatterns(pattern: String): String {
        return if (pattern.startsWith("/")) {
            // Pattern starting with / should match from root
            "^" + pattern.substring(1)
        } else {
            // Pattern not starting with / can match anywhere in the path
            "(?:^|.*/)$pattern"
        }
    }
}
