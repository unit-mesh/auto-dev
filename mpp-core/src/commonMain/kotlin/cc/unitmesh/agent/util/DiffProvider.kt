package cc.unitmesh.agent.util

import cc.unitmesh.agent.platform.GitOperations

/**
 * DiffProvider interface - provides diff generation capabilities
 * with Git as primary method and LCS-based fallback
 */
interface DiffProvider {
    /**
     * Calculate diff statistics between old and new content
     */
    suspend fun calculateDiffStats(oldContent: String?, newContent: String?, filePath: String): DiffUtils.DiffStats
    
    /**
     * Generate unified diff format between old and new content
     */
    suspend fun generateUnifiedDiff(oldContent: String?, newContent: String?, filePath: String): String
    
    /**
     * Check if this provider is available on the current platform
     */
    fun isAvailable(): Boolean
}

/**
 * Git-based diff provider (preferred when Git is available)
 * Uses real git diff command for accurate, industry-standard diffs
 */
class GitDiffProvider(private val projectPath: String) : DiffProvider {
    private val gitOps = GitOperations(projectPath)
    
    override suspend fun calculateDiffStats(
        oldContent: String?,
        newContent: String?,
        filePath: String
    ): DiffUtils.DiffStats {
        if (!isAvailable()) {
            // Fallback to LCS-based calculation
            return DiffUtils.calculateDiffStats(oldContent, newContent)
        }
        
        // Try to get git diff
        val diff = gitOps.getFileDiff(filePath)
        if (diff != null) {
            return parseDiffStats(diff)
        }
        
        // Fallback to LCS-based calculation
        return DiffUtils.calculateDiffStats(oldContent, newContent)
    }
    
    override suspend fun generateUnifiedDiff(
        oldContent: String?,
        newContent: String?,
        filePath: String
    ): String {
        if (!isAvailable()) {
            // Fallback to LCS-based diff
            return DiffUtils.generateUnifiedDiff(oldContent, newContent, filePath)
        }
        
        // Try to get git diff
        val diff = gitOps.getFileDiff(filePath)
        if (diff != null && diff.isNotBlank()) {
            return diff
        }
        
        // Fallback to LCS-based diff
        return DiffUtils.generateUnifiedDiff(oldContent, newContent, filePath)
    }
    
    override fun isAvailable(): Boolean = gitOps.isSupported()
    
    /**
     * Parse diff statistics from git diff output
     */
    private fun parseDiffStats(diff: String): DiffUtils.DiffStats {
        var added = 0
        var deleted = 0
        var context = 0
        
        diff.lines().forEach { line ->
            when {
                line.startsWith("+") && !line.startsWith("+++") -> added++
                line.startsWith("-") && !line.startsWith("---") -> deleted++
                line.startsWith(" ") -> context++
            }
        }
        
        return DiffUtils.DiffStats(added, deleted, context)
    }
}

/**
 * Fallback diff provider using LCS algorithm
 * Used when Git is not available (e.g., Android, or when git is not configured)
 */
class FallbackDiffProvider : DiffProvider {
    override suspend fun calculateDiffStats(
        oldContent: String?,
        newContent: String?,
        filePath: String
    ): DiffUtils.DiffStats {
        return DiffUtils.calculateDiffStats(oldContent, newContent)
    }
    
    override suspend fun generateUnifiedDiff(
        oldContent: String?,
        newContent: String?,
        filePath: String
    ): String {
        return DiffUtils.generateUnifiedDiff(oldContent, newContent, filePath)
    }
    
    override fun isAvailable(): Boolean = true
}

/**
 * Factory for creating the appropriate DiffProvider
 */
object DiffProviderFactory {
    /**
     * Create a diff provider with Git as primary and LCS as fallback
     * 
     * @param projectPath Project root path for Git operations
     * @return GitDiffProvider if Git is available, otherwise FallbackDiffProvider
     */
    fun create(projectPath: String): DiffProvider {
        val gitProvider = GitDiffProvider(projectPath)
        return if (gitProvider.isAvailable()) {
            gitProvider
        } else {
            FallbackDiffProvider()
        }
    }
    
    /**
     * Create a fallback-only provider (useful for testing or when Git is explicitly disabled)
     */
    fun createFallback(): DiffProvider = FallbackDiffProvider()
}

