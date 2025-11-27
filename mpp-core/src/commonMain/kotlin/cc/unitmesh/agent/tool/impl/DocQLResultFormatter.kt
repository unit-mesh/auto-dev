package cc.unitmesh.agent.tool.impl

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

    fun formatSmartResult(
        results: List<ScoredResult>,
        keyword: String,
        truncated: Boolean = false,
        totalCount: Int = results.size
    ): String {
        return buildString {
            appendLine("**Smart Search Results for '$keyword'**")
            if (truncated) {
                appendLine("Showing ${results.size} of $totalCount results (sorted by relevance)")
            } else {
                appendLine("Found ${results.size} relevant items (sorted by relevance)")
            }
            appendLine()

            // Group by file for cleaner output
            val byFile = results.groupBy { it.filePath ?: "Unknown File" }

            byFile.forEach { (file, items) ->
                appendLine("## $file")
                items.forEach { result ->
                    val scoreInfo = if (result.score > 0) " (score: ${formatScore(result.score)})" else ""
                    when (val item = result.item) {
                        is Entity.ClassEntity -> {
                            appendLine("  - **Class**: ${item.name}$scoreInfo")
                            if (item.location.line != null) appendLine("    Line: ${item.location.line}")
                        }

                        is Entity.FunctionEntity -> {
                            appendLine("  - **Function**: ${item.name}$scoreInfo")
                            if (item.signature != null) appendLine("    Sig: ${item.signature}")
                            if (item.location.line != null) appendLine("    Line: ${item.location.line}")
                        }

                        is TOCItem -> {
                            appendLine("  - **Section**: ${item.title}$scoreInfo")
                        }

                        is DocumentChunk -> {
                            appendLine("  - **Content**: ...${result.preview}...$scoreInfo")
                        }

                        is TextSegment -> {
                            val type = item.type
                            appendLine("  - **${type.replaceFirstChar { it.uppercase() }}**: ${item.name.ifEmpty { result.preview }}$scoreInfo")
                        }
                    }
                }
                appendLine()
            }

            if (truncated) {
                appendLine("Tip: Use a more specific query or increase `maxResults` to see more results.")
            } else {
                appendLine("Tip: Use `$.code.class(\"Name\")` or `$.content.heading(\"Title\")` for more specific queries.")
            }
        }
    }

    fun formatDocQLResult(result: DocQLResult, maxResults: Int): String {
        return result.formatDocQLResult(maxResults)
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
