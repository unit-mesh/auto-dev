package cc.unitmesh.agent.tool.impl.docql

import cc.unitmesh.devins.document.DocumentChunk
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.devins.document.docql.DocQLResult
import cc.unitmesh.agent.scoring.TextSegment
import kotlin.math.round

/**
 * Responsible for formatting DocQL search results for display to the user/LLM.
 */
object DocQLResultFormatter {

    /**
     * Maximum number of items to show in the compact summary
     */
    private const val COMPACT_MAX_ITEMS = 12

    /**
     * Format a compact summary for display to user/LLM.
     * Shows key information without technical details like scores.
     *
     * Example output:
     * ```
     * Found 12 results for "Auth":
     * - AuthService (class) in auth/AuthService.kt
     * - authenticate (function) in auth/AuthService.kt
     * - Authorization (heading) in docs/security.md
     * ... and 9 more
     * ```
     */
    fun formatCompactSummary(
        results: List<ScoredResult>,
        keyword: String,
        truncated: Boolean = false,
        totalCount: Int = results.size
    ): String {
        if (results.isEmpty()) {
            return "No results found for '$keyword'."
        }

        return buildString {
            val countInfo = if (truncated) "$totalCount+" else "${results.size}"
            appendLine("Found $countInfo results for \"$keyword\":")
            appendLine()

            val itemsToShow = results.take(COMPACT_MAX_ITEMS)

            itemsToShow.forEach { result ->
                val (itemType, itemName) = extractItemInfo(result)
                val fileInfo = result.filePath
                appendLine("- $itemName ($itemType)$fileInfo")
            }

            val remainingCount = results.size - COMPACT_MAX_ITEMS
            if (remainingCount > 0) {
                appendLine()
                appendLine("... and $remainingCount more (click Details to view all)")
            }
        }
    }

    /**
     * Extract item type and name from a ScoredResult for compact display.
     */
    private fun extractItemInfo(result: ScoredResult): Pair<String, String> {
        return when (val item = result.item) {
            is Entity.ClassEntity -> {
                "class" to item.name
            }

            is Entity.FunctionEntity -> {
                "function" to item.name
            }

            is TOCItem -> {
                "heading" to item.title
            }

            is DocumentChunk -> {
                "content" to result.preview.take(40)
            }

            is TextSegment -> {
                item.type to (item.name.ifEmpty { result.preview.take(40) })
            }

            else -> "item" to result.preview.take(40)
        }
    }

    fun formatDetailedResult(
        results: List<ScoredResult>,
        keyword: String,
        truncated: Boolean = false,
        totalCount: Int = results.size
    ): String {
        return buildString {
            appendLine("## Search Results for '$keyword'")
            if (truncated) {
                appendLine("Showing ${results.size} of $totalCount results (sorted by relevance)")
            } else {
                appendLine("Found ${results.size} relevant items (sorted by relevance)")
            }
            appendLine()

            // Group by file for cleaner output
            val byFile = results.groupBy { it.filePath ?: "Unknown File" }

            byFile.forEach { (file, items) ->
                appendLine("### $file")
                items.forEach { result ->
                    val scoreInfo = if (result.score > 0) " (score: ${formatScore(result.score)})" else ""
                    when (val item = result.item) {
                        is Entity.ClassEntity -> {
                            appendLine("- **Class**: `${item.name}`$scoreInfo")
                            if (item.location.line != null) appendLine("  - Line: ${item.location.line}")
                        }

                        is Entity.FunctionEntity -> {
                            appendLine("- **Function**: `${item.name}`$scoreInfo")
                            if (item.signature != null) appendLine("  - Signature: `${item.signature}`")
                            if (item.location.line != null) appendLine("  - Line: ${item.location.line}")
                        }

                        is TOCItem -> {
                            appendLine("- **Section**: ${item.title}$scoreInfo")
                            appendLine("  - Level: H${item.level}")
                        }

                        is DocumentChunk -> {
                            appendLine("- **Content**:$scoreInfo")
                            appendLine("  > ${result.preview.take(200)}")
                        }

                        is TextSegment -> {
                            val type = item.type.replaceFirstChar { it.uppercase() }
                            appendLine("- **$type**: ${item.name.ifEmpty { result.preview.take(100) }}$scoreInfo")
                        }
                    }
                }
                appendLine()
            }
        }
    }

    fun buildQuerySuggestion(query: String): String {
        val suggestions = mutableListOf<String>()

        suggestions.add("💡 **Suggestions to find the information:**")
        if (!query.contains("toc")) {
            suggestions.add("1. Try `$.toc[*]` to see all available sections in the documents")
        }

        if (query.contains("heading") || query.contains("h1") || query.contains("h2")) {
            suggestions.add("2. Try a broader heading search with fewer keywords")
            suggestions.add("3. Try `$.content.chunks()` to get all content and search manually")
        } else if (!query.contains("chunks")) {
            suggestions.add("2. Try `$.content.chunks()` to retrieve all document content")
        }

        return suggestions.joinToString("\n")
    }

    /**
     * Format a score value to 2 decimal places (multiplatform compatible)
     */
    fun formatScore(value: Double): String {
        val rounded = round(value * 100) / 100
        val str = rounded.toString()
        val dotIndex = str.indexOf('.')
        return if (dotIndex == -1) {
            "$str.00"
        } else {
            val currentDecimals = str.length - dotIndex - 1
            when {
                currentDecimals >= 2 -> str.take(dotIndex + 3)
                currentDecimals == 1 -> str + "0"
                else -> str + "00"
            }
        }
    }
}
