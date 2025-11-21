package cc.unitmesh.agent.vcs.context

import cc.unitmesh.agent.diff.DiffLineType
import cc.unitmesh.agent.diff.DiffParser
import cc.unitmesh.agent.logging.getLogger

/**
 * Represents a code hunk (changed block) with context
 */
data class CodeHunk(
    val oldStartLine: Int,
    val newStartLine: Int,
    val oldLineCount: Int,
    val newLineCount: Int,
    val contextBefore: List<String>,
    val addedLines: List<String>,
    val deletedLines: List<String>,
    val contextAfter: List<String>,
    val header: String
) {
    /**
     * Format this hunk as a unified diff block
     */
    fun toUnifiedDiff(): String = buildString {
        appendLine(header)
        contextBefore.forEach { appendLine(" $it") }
        deletedLines.forEach { appendLine("-$it") }
        addedLines.forEach { appendLine("+$it") }
        contextAfter.forEach { appendLine(" $it") }
    }

    /**
     * Get total line count (for display)
     */
    val totalLines: Int
        get() = contextBefore.size + addedLines.size + deletedLines.size + contextAfter.size
}

/**
 * Extracts changed code hunks from git diff patches
 * 
 * This utility parses git diffs and extracts only the changed code blocks
 * with configurable context lines, which is useful for:
 * - Reducing LLM context size by sending only changed code
 * - Focusing AI analysis on actual modifications
 * - Providing sufficient context for understanding changes
 */
class ChangedCodeExtractor {
    private val logger = getLogger("ChangedCodeExtractor")

    /**
     * Extract changed code hunks from a git diff patch
     * 
     * @param patch The raw git diff patch string
     * @param contextLines Number of context lines to include before/after changes (default: 3)
     * @return Map of file path to list of changed hunks
     */
    fun extractChangedHunks(
        patch: String,
        contextLines: Int = 3
    ): Map<String, List<CodeHunk>> {
        if (patch.isBlank()) {
            logger.warn { "Empty patch provided to extractChangedHunks" }
            return emptyMap()
        }

        val fileDiffs = try {
            DiffParser.parse(patch)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse diff patch: ${e.message}" }
            return emptyMap()
        }

        val result = mutableMapOf<String, List<CodeHunk>>()

        for (fileDiff in fileDiffs) {
            // Skip binary files
            if (fileDiff.isBinaryFile) {
                logger.debug { "Skipping binary file: ${fileDiff.newPath ?: fileDiff.oldPath}" }
                continue
            }

            // Get the file path (prefer newPath for renamed files)
            val filePath = fileDiff.newPath ?: fileDiff.oldPath ?: continue

            // Extract hunks from this file
            val hunks = fileDiff.hunks.map { diffHunk ->
                extractCodeHunk(diffHunk, contextLines)
            }

            if (hunks.isNotEmpty()) {
                result[filePath] = hunks
                logger.debug { "Extracted ${hunks.size} hunks from $filePath" }
            }
        }

        logger.info { "Extracted changes from ${result.size} files, ${result.values.sumOf { it.size }} total hunks" }
        return result
    }

    /**
     * Extract a single code hunk from a DiffHunk
     * 
     * Strategy:
     * 1. Find the first actual change (add/delete)
     * 2. Everything before first change = contextBefore (keep last N lines)
     3. Collect all adds/deletes
     * 4. Everything after last change = contextAfter (keep first N lines)
     */
    private fun extractCodeHunk(
        diffHunk: cc.unitmesh.agent.diff.DiffHunk,
        maxContextLines: Int
    ): CodeHunk {
        val allLines = diffHunk.lines
        
        // Find first and last change index
        var firstChangeIndex = -1
        var lastChangeIndex = -1
        
        for (i in allLines.indices) {
            val line = allLines[i]
            if (line.type == DiffLineType.ADDED || line.type == DiffLineType.DELETED) {
                if (firstChangeIndex == -1) {
                    firstChangeIndex = i
                }
                lastChangeIndex = i
            }
        }
        
        // If no changes found (shouldn't happen in a valid diff), return empty hunk
        if (firstChangeIndex == -1) {
            return CodeHunk(
                oldStartLine = diffHunk.oldStartLine,
                newStartLine = diffHunk.newStartLine,
                oldLineCount = diffHunk.oldLineCount,
                newLineCount = diffHunk.newLineCount,
                contextBefore = emptyList(),
                addedLines = emptyList(),
                deletedLines = emptyList(),
                contextAfter = emptyList(),
                header = diffHunk.header
            )
        }
        
        // Extract context before (lines before first change)
        val allContextBefore = allLines.subList(0, firstChangeIndex)
            .filter { it.type == DiffLineType.CONTEXT }
            .map { it.content }
        
        // Keep only last N context lines
        val contextBefore = if (allContextBefore.size > maxContextLines) {
            allContextBefore.takeLast(maxContextLines)
        } else {
            allContextBefore
        }
        
        // Extract changes (between first and last change, inclusive)
        val addedLines = mutableListOf<String>()
        val deletedLines = mutableListOf<String>()
        
        for (i in firstChangeIndex..lastChangeIndex) {
            when (allLines[i].type) {
                DiffLineType.ADDED -> addedLines.add(allLines[i].content)
                DiffLineType.DELETED -> deletedLines.add(allLines[i].content)
                DiffLineType.CONTEXT -> {
                    // Context lines in the middle of changes stay in the change block
                    // We could add them to both contextBefore certain deletes and contextAfter certain adds
                    // For simplicity, we'll keep them as implicit context in the changed region
                }
                DiffLineType.HEADER -> {} // Skip
            }
        }
        
        // Extract context after (lines after last change)
        val allContextAfter = if (lastChangeIndex + 1 < allLines.size) {
            allLines.subList(lastChangeIndex + 1, allLines.size)
                .filter { it.type == DiffLineType.CONTEXT }
                .map { it.content }
        } else {
            emptyList()
        }
        
        // Keep only first N context lines
        val contextAfter = if (allContextAfter.size > maxContextLines) {
            allContextAfter.take(maxContextLines)
        } else {
            allContextAfter
        }

        return CodeHunk(
            oldStartLine = diffHunk.oldStartLine,
            newStartLine = diffHunk.newStartLine,
            oldLineCount = diffHunk.oldLineCount,
            newLineCount = diffHunk.newLineCount,
            contextBefore = contextBefore,
            addedLines = addedLines,
            deletedLines = deletedLines,
            contextAfter = contextAfter,
            header = diffHunk.header
        )
    }

    /**
     * Format changed hunks as a human-readable summary
     * Useful for debugging and logging
     */
    fun formatHunksSummary(hunks: Map<String, List<CodeHunk>>): String = buildString {
        appendLine("Changed Code Summary:")
        appendLine("=" * 50)
        
        for ((filePath, fileHunks) in hunks) {
            appendLine()
            appendLine("File: $filePath")
            appendLine("-" * 50)
            
            fileHunks.forEachIndexed { index, hunk ->
                appendLine("  Hunk #${index + 1}:")
                appendLine("    Lines: ${hunk.newStartLine}-${hunk.newStartLine + hunk.newLineCount - 1}")
                appendLine("    Added: ${hunk.addedLines.size}, Deleted: ${hunk.deletedLines.size}")
                appendLine("    Context: ${hunk.contextBefore.size} before, ${hunk.contextAfter.size} after")
            }
        }
        
        appendLine()
        appendLine("=" * 50)
        appendLine("Total: ${hunks.size} files, ${hunks.values.sumOf { it.size }} hunks")
    }

    /**
     * Operator function for string multiplication (for formatting)
     */
    private operator fun String.times(n: Int): String = repeat(n)
}
