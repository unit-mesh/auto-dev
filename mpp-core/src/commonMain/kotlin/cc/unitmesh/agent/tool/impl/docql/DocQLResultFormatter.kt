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
     * Files that should be deprioritized in search results.
     * These files often contain a lot of noise (version history, temporary notes, etc.)
     */
    private val LOW_PRIORITY_FILES = setOf(
        "CHANGELOG.md", "CHANGELOG", "HISTORY.md", "RELEASES.md",
        "package-lock.json", "yarn.lock", "pnpm-lock.yaml"
    )

    /**
     * Directory patterns that should be deprioritized.
     * Matches any path containing these directory names.
     */
    private val LOW_PRIORITY_DIRS = setOf(
        ".augment", ".cursor", ".vscode", ".idea",
        "node_modules", "__pycache__", ".git"
    )

    /**
     * Check if a file path is low priority (noisy content).
     */
    private fun isLowPriorityPath(path: String): Boolean {
        val fileName = path.substringAfterLast('/')
        if (LOW_PRIORITY_FILES.contains(fileName)) return true
        return LOW_PRIORITY_DIRS.any { dir -> path.contains("/$dir/") || path.startsWith("$dir/") }
    }

    /**
     * Get file priority for sorting (lower = higher priority).
     * Source code files get higher priority than documentation.
     */
    private fun getFilePriority(path: String): Int {
        if (isLowPriorityPath(path)) return 100

        val ext = path.substringAfterLast('.').lowercase()
        return when (ext) {
            // Source code (highest priority)
            "kt", "java", "ts", "tsx", "js", "jsx", "py", "go", "rs", "cs" -> 0
            // Test files (still important but lower than main code)
            else -> if (path.contains("test", ignoreCase = true) ||
                path.contains("Test.")
            ) 5 else {
                when (ext) {
                    // Configuration files
                    "gradle", "kts", "toml", "yaml", "yml", "json", "xml" -> 10
                    // Documentation (lower priority)
                    "md", "txt", "rst", "adoc" -> 20
                    // Other
                    else -> 15
                }
            }
        }
    }

    /**
     * Sort files by priority, putting source code first and low-priority files last.
     */
    private fun <T> sortByFilePriority(items: Map<String, T>): List<Pair<String, T>> {
        return items.entries
            .sortedBy { getFilePriority(it.key) }
            .map { it.key to it.value }
    }

    /**
     * Format file path with icon for display.
     * Shows a cleaner, more readable path.
     */
    private fun formatFilePath(path: String): String {
        val displayPath = if (path.length > 80) {
            val parts = path.split("/")
            if (parts.size > 4) {
                val start = parts.take(2).joinToString("/")
                val end = parts.takeLast(2).joinToString("/")
                "$start/.../$end"
            } else path
        } else path
        return "$displayPath"
    }

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
        // Filter out "unknown" entities and low-priority files
        val filteredResults = results.filter { result ->
            !isUnknownEntity(result) && !isLowPriorityPath(result.filePath ?: "")
        }

        if (filteredResults.isEmpty()) {
            return "No results found"
        }

        val countInfo = if (truncated) "$totalCount+" else "${filteredResults.size}"

        // Generate concise preview with top items
        val topItems = filteredResults.take(3).mapNotNull { result ->
            val (itemType, itemName) = extractItemInfo(result)
            if (itemName.isNotBlank()) "$itemName ($itemType)" else null
        }

        val preview = topItems.joinToString(", ")

        // Add file type distribution for large result sets
        val fileTypeSummary = if (filteredResults.size > 5) {
            val byType = filteredResults.groupBy { result ->
                when (val item = result.item) {
                    is Entity.ClassEntity -> "class"
                    is Entity.FunctionEntity -> "function"
                    is Entity.ConstructorEntity -> "constructor"
                    is TOCItem -> "heading"
                    is DocumentChunk -> "content"
                    else -> "item"
                }
            }
            val typeCounts = byType.entries
                .sortedByDescending { it.value.size }
                .take(3)
                .joinToString(", ") { "${it.value.size} ${it.key}s" }
            " ($typeCounts)"
        } else ""

        return "Found $countInfo: $preview$fileTypeSummary"
    }

    /**
     * Check if the result represents an unknown/unparseable entity
     */
    private fun isUnknownEntity(result: ScoredResult): Boolean {
        return when (val item = result.item) {
            is Entity.ClassEntity -> item.name == "unknown" || item.name.isBlank()
            is Entity.FunctionEntity -> item.name == "unknown" || item.name.isBlank()
            is Entity.ConstructorEntity -> item.name == "unknown" || item.name.isBlank()
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

            is Entity.ConstructorEntity -> {
                val lineInfo = item.location.line?.let { ":$it" } ?: ""
                "constructor" to "${item.className}$lineInfo"
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

    /**
     * Maximum lines to show for function/method code blocks in search results.
     */
    private const val MAX_CODE_PREVIEW_LINES = 20

    fun formatFallbackResult(
        results: List<ScoredResult>,
        keyword: String,
        truncated: Boolean = false,
        totalCount: Int = results.size
    ): String {
        // Filter out "unknown" entities and low-priority files
        val filteredResults = results.filter { result ->
            val isValidEntity = when (val item = result.item) {
                is Entity.ClassEntity -> item.name != "unknown" && item.name.isNotBlank()
                is Entity.FunctionEntity -> item.name != "unknown" && item.name.isNotBlank()
                is Entity.ConstructorEntity -> item.name != "unknown" && item.name.isNotBlank()
                else -> true
            }
            isValidEntity
        }

        // Separate high-priority and low-priority results
        val (highPriority, lowPriority) = filteredResults.partition { result ->
            val path = result.filePath ?: ""
            !isLowPriorityPath(path)
        }

        val unknownCount = results.size - filteredResults.size
        val lowPriorityCount = lowPriority.size

        return buildString {
            appendLine("## Search Results for '$keyword'")
            if (truncated) {
                appendLine("Showing ${highPriority.size} of $totalCount results (sorted by relevance)")
            } else {
                appendLine("Found ${highPriority.size} relevant items (sorted by relevance)")
            }
            if (unknownCount > 0) {
                appendLine("($unknownCount unparseable items filtered)")
            }
            if (lowPriorityCount > 0) {
                appendLine("($lowPriorityCount low-priority results from CHANGELOG/generated files hidden)")
            }
            appendLine()

            // Group by file and sort by priority (source code first)
            val byFile = highPriority.groupBy { it.filePath ?: "Unknown File" }
            val sortedByFile = sortByFilePriority(byFile)

            sortedByFile.forEach { (file, items) ->
                appendLine("### ${formatFilePath(file)}")
                appendLine()

                items.forEach { result ->
                    val scoreInfo = if (result.score > 0) " (score: ${formatScore(result.score)})" else ""
                    when (val item = result.item) {
                        is Entity.ClassEntity -> {
                            val lineInfo = item.location.line?.let { ":$it" } ?: ""
                            appendLine("#### Class: `${item.name}$lineInfo`$scoreInfo")
                            appendLine()
                            // Show class summary with functions from preview
                            val preview = formatCodePreview(result.preview)
                            if (preview.isNotBlank()) {
                                appendLine("```kotlin")
                                appendLine(preview)
                                appendLine("```")
                            }
                            appendLine()
                        }

                        is Entity.FunctionEntity -> {
                            val lineInfo = item.location.line?.let { ":$it" } ?: ""
                            val sig = item.signature ?: item.name
                            appendLine("#### Function: `$sig$lineInfo`$scoreInfo")
                            appendLine()
                            // Show function code (up to MAX_CODE_PREVIEW_LINES lines)
                            val preview = formatCodePreview(result.preview)
                            if (preview.isNotBlank() && preview != sig) {
                                appendLine("```kotlin")
                                appendLine(preview)
                                appendLine("```")
                            }
                            appendLine()
                        }

                        is Entity.ConstructorEntity -> {
                            val lineInfo = item.location.line?.let { ":$it" } ?: ""
                            val sig = item.signature ?: "${item.className}()"
                            appendLine("#### Constructor: `$sig$lineInfo`$scoreInfo")
                            appendLine()
                            // Show constructor code
                            val preview = formatCodePreview(result.preview)
                            if (preview.isNotBlank() && preview != sig) {
                                appendLine("```kotlin")
                                appendLine(preview)
                                appendLine("```")
                            }
                            appendLine()
                        }

                        is TOCItem -> {
                            appendLine("#### Section: ${item.title}$scoreInfo")
                            appendLine("- Level: H${item.level}")
                            if (!item.content.isNullOrBlank()) {
                                val contentPreview = item.content!!.lines()
                                    .take(MAX_CODE_PREVIEW_LINES)
                                    .joinToString("\n")
                                appendLine()
                                appendLine("> ${contentPreview.replace("\n", "\n> ")}")
                            }
                            appendLine()
                        }

                        is DocumentChunk -> {
                            val title = item.chapterTitle?.let { "#### $it$scoreInfo" } ?: "#### Content$scoreInfo"
                            appendLine(title)
                            appendLine()
                            // Show content (up to MAX_CODE_PREVIEW_LINES lines)
                            val preview = formatCodePreview(result.preview)
                            if (preview.isNotBlank()) {
                                // Detect if it looks like code
                                if (looksLikeCode(preview)) {
                                    appendLine("```")
                                    appendLine(preview)
                                    appendLine("```")
                                } else {
                                    appendLine(preview)
                                }
                            }
                            appendLine()
                        }

                        is TextSegment -> {
                            val type = item.type.replaceFirstChar { it.uppercase() }
                            val name = item.name.ifEmpty { extractNameFromPreview(result.preview) }
                            appendLine("#### $type: `$name`$scoreInfo")
                            appendLine()
                            // Show code content (up to MAX_CODE_PREVIEW_LINES lines)
                            val preview = formatCodePreview(result.preview)
                            if (preview.isNotBlank() && preview != name) {
                                if (looksLikeCode(preview)) {
                                    appendLine("```kotlin")
                                    appendLine(preview)
                                    appendLine("```")
                                } else {
                                    appendLine(preview)
                                }
                            }
                            appendLine()
                        }
                    }
                }
            }
        }
    }

    /**
     * Format code preview, limiting to MAX_CODE_PREVIEW_LINES lines.
     */
    private fun formatCodePreview(preview: String): String {
        val lines = preview.lines()
        return if (lines.size > MAX_CODE_PREVIEW_LINES) {
            val truncatedLines = lines.take(MAX_CODE_PREVIEW_LINES)
            val remaining = lines.size - MAX_CODE_PREVIEW_LINES
            truncatedLines.joinToString("\n") + "\n// ... ($remaining more lines)"
        } else {
            preview.trim()
        }
    }

    /**
     * Check if content looks like code (has typical code patterns).
     */
    private fun looksLikeCode(content: String): Boolean {
        val codePatterns = listOf(
            "fun ", "class ", "interface ", "object ", "val ", "var ", // Kotlin
            "public ", "private ", "protected ", "void ", "static ", // Java
            "def ", "import ", "from ", // Python
            "function ", "const ", "let ", "=>", // JavaScript/TypeScript
            "{", "}", "()", "->", "::"
        )
        return codePatterns.any { content.contains(it) }
    }

    /**
     * Extract a name from preview content (first meaningful line).
     */
    private fun extractNameFromPreview(preview: String): String {
        val firstLine = preview.lines().firstOrNull { it.isNotBlank() } ?: preview
        return firstLine.take(60).let { if (firstLine.length > 60) "$it..." else it }
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
                    // Filter out low-priority files
                    val filteredByFile = result.itemsByFile.filterKeys { !isLowPriorityPath(it) }
                    val lowPriorityCount = result.totalCount - filteredByFile.values.sumOf { it.size }

                    val totalItems = filteredByFile.values.sumOf { it.size }
                    val truncated = totalItems > maxResults * 2

                    appendLine("Found $totalItems TOC items across ${filteredByFile.size} file(s):")
                    if (truncated) {
                        appendLine("Showing first $maxResults results (${totalItems - maxResults} more available)")
                    }
                    if (lowPriorityCount > 0) {
                        appendLine("($lowPriorityCount items from CHANGELOG/generated files hidden)")
                    }
                    appendLine()

                    var count = 0
                    // Sort by file priority (source code first)
                    val sortedFiles = sortByFilePriority(filteredByFile)

                    for ((filePath, items) in sortedFiles) {
                        if (count >= maxResults) break

                        appendLine("## ${formatFilePath(filePath)}")

                        // For very long TOC lists (like CHANGELOG), show summary instead
                        if (items.size > 50) {
                            appendLine("  (${items.size} sections - showing first 5)")
                            items.take(5).forEach { item ->
                                appendLine("  ${"  ".repeat(item.level - 1)}${item.level}. ${item.title}")
                            }
                            appendLine("  ...")
                            count += 5
                        } else {
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
                    // Filter out "unknown" entities and low-priority files
                    val filteredByFile = result.itemsByFile
                        .filterKeys { !isLowPriorityPath(it) }
                        .mapValues { (_, entities) ->
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
                    // Sort by file priority (source code first)
                    val sortedFiles = sortByFilePriority(filteredByFile)

                    for ((filePath, items) in sortedFiles) {
                        if (count >= maxResults) break

                        appendLine("## ${formatFilePath(filePath)}")

                        // Group entities by type for better readability
                        val classes = items.filterIsInstance<Entity.ClassEntity>()
                        val constructors = items.filterIsInstance<Entity.ConstructorEntity>()
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

                        // Format constructors - inline with line number
                        for (entity in constructors) {
                            if (count >= maxResults) break
                            val lineInfo = entity.location.line?.let { ":$it" } ?: ""
                            val sig = entity.signature ?: "${entity.className}()"
                            appendLine("  constructor $sig$lineInfo")
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
                    // Filter out low-priority files
                    val filteredByFile = result.itemsByFile.filterKeys { !isLowPriorityPath(it) }
                    val lowPriorityCount = result.totalCount - filteredByFile.values.sumOf { it.size }

                    val totalItems = filteredByFile.values.sumOf { it.size }
                    val truncated = totalItems > maxResults

                    appendLine("Found $totalItems content chunks across ${filteredByFile.size} file(s):")
                    if (truncated) {
                        appendLine("  Showing first $maxResults results (${totalItems - maxResults} more available)")
                        appendLine(" Tip: Narrow down your search to specific files or directories")
                        appendLine("   Example: Query documents in a specific directory only")
                    }
                    if (lowPriorityCount > 0) {
                        appendLine("($lowPriorityCount chunks from CHANGELOG/generated files hidden)")
                    }
                    appendLine()

                    var count = 0
                    // Sort by file priority (source code first)
                    val sortedFiles = sortByFilePriority(filteredByFile)

                    for ((filePath, items) in sortedFiles) {
                        if (count >= maxResults) break

                        // Filter out empty or whitespace-only chunks
                        val nonEmptyItems = items.filter { it.content.trim().isNotEmpty() }
                        if (nonEmptyItems.isEmpty()) continue

                        appendLine("## ${formatFilePath(filePath)}")
                        appendLine()

                        for (chunk in nonEmptyItems) {
                            if (count >= maxResults) break

                            val content = chunk.content.trim()
                            if (content.isNotEmpty()) {
                                // Limit content to MAX_CODE_PREVIEW_LINES for readability
                                val formattedContent = formatCodePreview(content)
                                appendLine(formattedContent)
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
                        appendLine("## ${formatFilePath(filePath)}")
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
                        appendLine("## ${formatFilePath(filePath)}")
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

            is DocQLResult.Frontmatter -> {
                buildString {
                    appendLine("Frontmatter:")
                    appendLine()
                    result.data.forEach { (key, value) ->
                        appendLine("  $key: $value")
                    }
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
