package cc.unitmesh.agent.tool.impl.docql

import cc.unitmesh.devins.document.DocumentChunk
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import cc.unitmesh.devins.document.docql.DocQLResult
import cc.unitmesh.devins.document.docql.initialMaxResults
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.round

object DocQLResultFormatter {

    /**
     * Format a smart summary for header display.
     * Shows a concise one-line summary of the search results.
     *
     * Example outputs:
     * - "Found 12 results: AuthService (class), authenticate (function), ..."
     * - "No results found"
     */
    fun formatSmartSummary(
        results: List<ScoredResult>,
        totalCount: Int,
        truncated: Boolean
    ): String {
        // Filter out "unknown" entities (failed parsing results)
        val filteredResults = results.filter { result ->
            !isUnknownEntity(result)
        }

        if (filteredResults.isEmpty()) {
            return "No results found"
        }

        val countInfo = if (truncated) "$totalCount+" else "${filteredResults.size}"

        val topItems = filteredResults.take(initialMaxResults).mapNotNull { result ->
            val (itemType, itemName) = extractItemInfo(result)
            if (itemName.isNotBlank()) "$itemName ($itemType)" else null
        }

        val preview = topItems.joinToString(", ")
        val moreCount = filteredResults.size - initialMaxResults
        val moreInfo = if (moreCount > 0) ", +$moreCount more" else ""

        return "Found $countInfo: $preview$moreInfo"
    }

    /**
     * Check if the result represents an unknown/unparseable entity
     */
    private fun isUnknownEntity(result: ScoredResult): Boolean {
        return when (val item = result.item) {
            is Entity.ClassEntity -> item.name == "unknown" || item.name.isBlank()
            is Entity.FunctionEntity -> item.name == "unknown" || item.name.isBlank()
            else -> false
        }
    }

    /**
     * Extract item type and name from a ScoredResult for compact display.
     * Format: "ClassName:123" or "functionName:45" with inline line numbers
     */
    fun extractItemInfo(result: ScoredResult): Pair<String, String> {
        return when (val item = result.item) {
            is Entity.ClassEntity -> {
                val lineInfo = item.location.line?.let { ":$it" } ?: ""
                "class" to "${item.name}$lineInfo"
            }

            is Entity.FunctionEntity -> {
                val lineInfo = item.location.line?.let { ":$it" } ?: ""
                "function" to "${item.name}$lineInfo"
            }

            is TOCItem -> {
                val lineInfo = item.lineNumber?.let { ":$it" } ?: ""
                "heading" to "${item.title}$lineInfo"
            }

            is DocumentChunk -> {
                "content" to item.content.take(50)
            }

            is TextSegment -> {
                item.type to (item.name.ifEmpty { result.preview.take(50) })
            }

            else -> "item" to result.preview.take(50)
        }
    }

    fun formatFallbackResult(
        results: List<ScoredResult>,
        keyword: String,
        truncated: Boolean = false,
        totalCount: Int = results.size
    ): String {
        // Filter out "unknown" entities (failed parsing results)
        val filteredResults = results.filter { result ->
            when (val item = result.item) {
                is Entity.ClassEntity -> item.name != "unknown" && item.name.isNotBlank()
                is Entity.FunctionEntity -> item.name != "unknown" && item.name.isNotBlank()
                else -> true
            }
        }

        val unknownCount = results.size - filteredResults.size

        return buildString {
            appendLine("## Search Results for '$keyword'")
            if (truncated) {
                appendLine("Showing ${filteredResults.size} of $totalCount results (sorted by relevance)")
            } else {
                appendLine("Found ${filteredResults.size} relevant items (sorted by relevance)")
            }
            if (unknownCount > 0) {
                appendLine("($unknownCount unparseable items filtered)")
            }
            appendLine()

            // Group by file for cleaner output
            val byFile = filteredResults.groupBy { it.filePath ?: "Unknown File" }

            byFile.forEach { (file, items) ->
                appendLine("### $file")
                items.forEach { result ->
                    val scoreInfo = if (result.score > 0) " (${formatScore(result.score)})" else ""
                    when (val item = result.item) {
                        is Entity.ClassEntity -> {
                            val lineInfo = item.location.line?.let { ":$it" } ?: ""
                            appendLine("- **Class**: `${item.name}$lineInfo`$scoreInfo")
                        }

                        is Entity.FunctionEntity -> {
                            val lineInfo = item.location.line?.let { ":$it" } ?: ""
                            val sig = item.signature ?: item.name
                            appendLine("- **Function**: `$sig$lineInfo`$scoreInfo")
                        }

                        is TOCItem -> {
                            appendLine("- **Section**: ${item.title}$scoreInfo")
                            appendLine("  - Level: H${item.level}")
                            if (!item.content.isNullOrBlank()) {
                                appendLine("  > ${item.content.take(200).replace("\n", " ")}...")
                            }
                        }

                        is DocumentChunk -> {
                            appendLine("- **Content**:$scoreInfo")
                            appendLine("  > ${result.preview.take(200)}")
                        }

                        is TextSegment -> {
                            val type = item.type.replaceFirstChar { it.uppercase() }
                            appendLine("- **$type**: ${item.name.ifEmpty { result.preview }}$scoreInfo")
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


    fun formatDocQLResult(result: DocQLResult, maxResults: Int = initialMaxResults): String {
        return when (result) {
            is DocQLResult.TocItems -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults * 2

                    appendLine("Found $totalItems TOC items across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("Showing first $maxResults results (${totalItems - maxResults} more available)")
                    }
                    appendLine()

                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break

                        appendLine("## $filePath")
                        for (item in items) {
                            if (count >= maxResults) break
                            appendLine("  ${"  ".repeat(item.level - 1)}${item.level}. ${item.title}")
                            if (!item.content.isNullOrBlank()) {
                                val preview = item.content!!.lines().take(3).joinToString("\n") { "    > $it" }
                                appendLine(preview)
                                if (item.content!!.lines().size > 3) {
                                    appendLine("    > ...")
                                }
                            }
                            count++
                        }
                        appendLine()
                    }

                    if (truncated) {
                        appendLine(" Tip: Query specific directories to get more focused results:")
                        appendLine("   \$.content.h1()")
                        appendLine("   \$.toc[?(@.title contains \"keyword\")]")
                    }
                }
            }

            is DocQLResult.Entities -> {
                buildString {
                    // Filter out "unknown" entities (failed parsing results)
                    val filteredByFile = result.itemsByFile.mapValues { (_, entities) ->
                        entities.filter { entity ->
                            entity.name != "unknown" && entity.name.isNotBlank()
                        }
                    }.filter { it.value.isNotEmpty() }

                    val filteredCount = filteredByFile.values.sumOf { it.size }
                    val unknownCount = result.totalCount - filteredCount
                    val truncated = filteredCount > maxResults

                    appendLine("Found $filteredCount entities across ${filteredByFile.size} file(s):")
                    if (truncated) {
                        appendLine(" Showing first $maxResults results (${filteredCount - maxResults} more available)")
                    }
                    if (unknownCount > 0) {
                        appendLine(" ($unknownCount unparseable entities filtered)")
                    }
                    appendLine()

                    var count = 0
                    for ((filePath, items) in filteredByFile) {
                        if (count >= maxResults) break

                        appendLine("## $filePath")

                        // Group entities by type for better readability
                        val classes = items.filterIsInstance<Entity.ClassEntity>()
                        val functions = items.filterIsInstance<Entity.FunctionEntity>()
                        val terms = items.filterIsInstance<Entity.Term>()
                        val apis = items.filterIsInstance<Entity.API>()

                        // Format classes - inline with line number
                        for (entity in classes) {
                            if (count >= maxResults) break
                            val lineInfo = entity.location.line?.let { ":$it" } ?: ""
                            val pkg = if (!entity.packageName.isNullOrEmpty()) " (${entity.packageName})" else ""
                            appendLine("  class ${entity.name}$lineInfo$pkg")
                            count++
                        }

                        // Format functions - inline with line number
                        for (entity in functions) {
                            if (count >= maxResults) break
                            val lineInfo = entity.location.line?.let { ":$it" } ?: ""
                            val sig = entity.signature ?: entity.name
                            appendLine("  $sig$lineInfo")
                            count++
                        }

                        // Format terms
                        for (entity in terms) {
                            if (count >= maxResults) break
                            appendLine("  ${entity.name}: ${entity.definition ?: ""}")
                            count++
                        }

                        // Format APIs
                        for (entity in apis) {
                            if (count >= maxResults) break
                            appendLine("  ${entity.name}: ${entity.signature ?: ""}")
                            count++
                        }

                        appendLine()
                    }

                    if (truncated) {
                        appendLine(" Tip: Use $.code.class(\"ClassName\") or $.code.function(\"functionName\") to get full source code")
                    }
                }
            }

            is DocQLResult.Chunks -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults

                    appendLine("Found $totalItems content chunks across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("  Showing first $maxResults results (${totalItems - maxResults} more available)")
                        appendLine(" Tip: Narrow down your search to specific files or directories")
                        appendLine("   Example: Query documents in a specific directory only")
                    }
                    appendLine()

                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break

                        // Filter out empty or whitespace-only chunks
                        val nonEmptyItems = items.filter { it.content.trim().isNotEmpty() }
                        if (nonEmptyItems.isEmpty()) continue

                        appendLine("## $filePath")
                        appendLine()

                        for (chunk in nonEmptyItems) {
                            if (count >= maxResults) break

                            val content = chunk.content.trim()
                            if (content.isNotEmpty()) {
                                appendLine(content)
                                appendLine()
                                appendLine("---")
                                appendLine()
                                count++
                            }
                        }
                    }

                    if (count == 0) {
                        appendLine("No non-empty chunks found.")
                    }
                }
            }

            is DocQLResult.CodeBlocks -> {
                buildString {
                    appendLine("Found ${result.totalCount} code blocks across ${result.itemsByFile.size} file(s):")
                    appendLine()
                    for ((filePath, items) in result.itemsByFile) {
                        appendLine("## $filePath")
                        items.forEach { block ->
                            appendLine("```${block.language ?: ""}")
                            appendLine(block.code)
                            appendLine("```")
                            appendLine()
                        }
                    }
                }
            }

            is DocQLResult.Tables -> {
                buildString {
                    appendLine("Found ${result.totalCount} tables across ${result.itemsByFile.size} file(s):")
                    for ((filePath, items) in result.itemsByFile) {
                        appendLine("## $filePath")
                        appendLine("  ${items.size} table(s)")
                    }
                }
            }

            is DocQLResult.Files -> {
                buildString {
                    appendLine("Found ${result.items.size} files:")
                    appendLine()
                    result.items.forEach { file ->
                        appendLine("## ${file.path}")
                        if (file.directory.isNotEmpty()) {
                            appendLine("   Directory: ${file.directory}")
                        }
                        if (file.extension.isNotEmpty()) {
                            appendLine("   Type: ${file.extension}")
                        }
                        if (file.size > 0) {
                            appendLine("   Size: ${file.size} characters")
                        }
                        appendLine()
                    }
                    if (result.items.size > 50) {
                        appendLine(" Too many results! Consider filtering by directory:")
                        appendLine(" \$.files[?(@.path contains \"your-directory\")]")
                    }
                }
            }

            is DocQLResult.Structure -> {
                buildString {
                    appendLine("File Structure (${result.directoryCount} directories, ${result.fileCount} files):")
                    appendLine()
                    appendLine(result.tree)
                    appendLine()
                    appendLine(" Use \$.files[*] to get detailed file information")
                    appendLine(" \$.files[?(@.extension==\"kt\")] to filter by extension")
                }
            }

            is DocQLResult.Empty -> {
                "No results found."
            }

            is DocQLResult.Error -> {
                throw ToolException(result.message, ToolErrorType.COMMAND_FAILED)
            }
        }
    }

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
