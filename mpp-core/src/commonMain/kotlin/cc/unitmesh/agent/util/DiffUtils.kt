package cc.unitmesh.agent.util

/**
 * Line-based diff algorithm using LCS (Longest Common Subsequence)
 * Ported from AutoDev IDEA's StreamDiff implementation
 */
object DiffUtils {
    
    /**
     * Result of a diff operation containing statistics
     */
    data class DiffStats(
        val addedLines: Int,
        val deletedLines: Int,
        val contextLines: Int
    ) {
        val totalChanges: Int get() = addedLines + deletedLines
    }
    
    /**
     * Represents a single line in a diff
     */
    sealed class DiffLine {
        data class Same(val line: String) : DiffLine()
        data class Added(val line: String) : DiffLine()
        data class Deleted(val line: String) : DiffLine()
    }
    
    /**
     * Calculate diff statistics between old and new content
     */
    fun calculateDiffStats(oldContent: String?, newContent: String?): DiffStats {
        if (oldContent == null && newContent == null) {
            return DiffStats(0, 0, 0)
        }
        
        if (oldContent == null) {
            // New file - all lines are added
            val lines = newContent?.lines()?.size ?: 0
            return DiffStats(addedLines = lines, deletedLines = 0, contextLines = 0)
        }
        
        if (newContent == null) {
            // Deleted file - all lines are deleted
            val lines = oldContent.lines().size
            return DiffStats(addedLines = 0, deletedLines = lines, contextLines = 0)
        }
        
        val oldLines = oldContent.lines()
        val newLines = newContent.lines()
        
        val diffs = diff(oldLines, newLines)
        
        var added = 0
        var deleted = 0
        var context = 0
        
        diffs.forEach { diffLine ->
            when (diffLine) {
                is DiffLine.Added -> added++
                is DiffLine.Deleted -> deleted++
                is DiffLine.Same -> context++
            }
        }
        
        return DiffStats(added, deleted, context)
    }
    
    /**
     * Generate a unified diff format string between old and new content
     */
    fun generateUnifiedDiff(
        oldContent: String?,
        newContent: String?,
        filePath: String,
        contextLines: Int = 3
    ): String {
        if (oldContent == null && newContent == null) {
            return ""
        }
        
        val oldLines = oldContent?.lines() ?: emptyList()
        val newLines = newContent?.lines() ?: emptyList()
        
        val diffs = diff(oldLines, newLines)
        
        val result = StringBuilder()
        
        // File header
        result.appendLine("--- a/$filePath")
        result.appendLine("+++ b/$filePath")
        
        // Group diffs into hunks
        val hunks = groupIntoHunks(diffs, oldLines.size, newLines.size, contextLines)
        
        hunks.forEach { hunk ->
            result.appendLine(hunk.header)
            hunk.lines.forEach { line ->
                result.appendLine(line)
            }
        }
        
        return result.toString()
    }
    
    /**
     * Compute diff between two lists of lines
     */
    private fun diff(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        // Compute LCS table
        val lcs = computeLcsTable(oldLines, newLines)
        
        // Backtrack to build diff
        val diffs = mutableListOf<DiffLine>()
        backtrackDiff(lcs, oldLines, newLines, oldLines.size, newLines.size, diffs)
        
        return diffs
    }
    
    /**
     * Compute the LCS (Longest Common Subsequence) table using dynamic programming
     */
    private fun computeLcsTable(oldLines: List<String>, newLines: List<String>): Array<IntArray> {
        val m = oldLines.size
        val n = newLines.size
        val table = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0 until m) {
            for (j in 0 until n) {
                if (oldLines[i] == newLines[j]) {
                    table[i + 1][j + 1] = table[i][j] + 1
                } else {
                    table[i + 1][j + 1] = maxOf(table[i + 1][j], table[i][j + 1])
                }
            }
        }
        
        return table
    }
    
    /**
     * Backtrack through the LCS table to construct the diff
     */
    private fun backtrackDiff(
        lcs: Array<IntArray>,
        oldLines: List<String>,
        newLines: List<String>,
        i: Int,
        j: Int,
        diffs: MutableList<DiffLine>
    ) {
        if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
            backtrackDiff(lcs, oldLines, newLines, i - 1, j - 1, diffs)
            diffs.add(DiffLine.Same(oldLines[i - 1]))
        } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
            backtrackDiff(lcs, oldLines, newLines, i, j - 1, diffs)
            diffs.add(DiffLine.Added(newLines[j - 1]))
        } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
            backtrackDiff(lcs, oldLines, newLines, i - 1, j, diffs)
            diffs.add(DiffLine.Deleted(oldLines[i - 1]))
        }
    }
    
    /**
     * Represents a hunk in a unified diff
     */
    private data class Hunk(
        val oldStart: Int,
        val oldCount: Int,
        val newStart: Int,
        val newCount: Int,
        val lines: List<String>
    ) {
        val header: String
            get() = "@@ -$oldStart,$oldCount +$newStart,$newCount @@"
    }
    
    /**
     * Group diff lines into hunks with context
     */
    private fun groupIntoHunks(
        diffs: List<DiffLine>,
        oldTotalLines: Int,
        newTotalLines: Int,
        contextLines: Int
    ): List<Hunk> {
        if (diffs.isEmpty()) {
            return emptyList()
        }
        
        val hunks = mutableListOf<Hunk>()
        val currentHunkLines = mutableListOf<String>()
        
        var oldLineNum = 1
        var newLineNum = 1
        var hunkOldStart = 1
        var hunkNewStart = 1
        var hunkOldCount = 0
        var hunkNewCount = 0
        
        diffs.forEach { diffLine ->
            when (diffLine) {
                is DiffLine.Same -> {
                    currentHunkLines.add(" ${diffLine.line}")
                    oldLineNum++
                    newLineNum++
                    hunkOldCount++
                    hunkNewCount++
                }
                is DiffLine.Added -> {
                    currentHunkLines.add("+${diffLine.line}")
                    newLineNum++
                    hunkNewCount++
                }
                is DiffLine.Deleted -> {
                    currentHunkLines.add("-${diffLine.line}")
                    oldLineNum++
                    hunkOldCount++
                }
            }
        }
        
        if (currentHunkLines.isNotEmpty()) {
            hunks.add(
                Hunk(
                    oldStart = hunkOldStart,
                    oldCount = hunkOldCount,
                    newStart = hunkNewStart,
                    newCount = hunkNewCount,
                    lines = currentHunkLines
                )
            )
        }
        
        return hunks
    }
}

