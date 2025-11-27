package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*

/**
 * Code block extracted from document
 */
data class CodeBlock(
    val language: String?,
    val code: String,
    val location: Location
)

/**
 * Table block extracted from document
 */
data class TableBlock(
    val headers: List<String>,
    val rows: List<List<String>>,
    val location: Location
)

/**
 * DocQL Executor for Markdown and documentation files
 * Handles content queries, heading queries, and code block extraction from markdown
 */
class MarkdownDocQLExecutor(
    documentFile: DocumentFile?,
    parserService: DocumentParserService?
) : BaseDocQLExecutor(documentFile, parserService) {

    /**
     * Execute content query ($.content.heading(...), $.content.h1(...), etc.)
     *
     * Supports both function call syntax and property + array access syntax:
     * - $.content.heading("keyword") - function call
     * - $.content.codeblock[*] - property with array access
     */
    override suspend fun executeContentQuery(nodes: List<DocQLNode>): DocQLResult {
        if (nodes.isEmpty()) {
            return DocQLResult.Error("Content query requires function call (e.g., $.content.chunks() or $.content.heading(\"keyword\"))")
        }

        // First check if we have a property node (for codeblock[*], table[*] syntax)
        val firstNode = nodes.firstOrNull()
        if (firstNode is DocQLNode.Property) {
            when (firstNode.name) {
                "codeblock" -> return executeCodeBlockQuery(nodes.drop(1))
                "table" -> return executeTableQuery(nodes.drop(1))
            }
        }

        // Otherwise look for function calls
        val functionNode = nodes.firstOrNull { it is DocQLNode.FunctionCall } as? DocQLNode.FunctionCall
            ?: return DocQLResult.Error("Content query requires function call (e.g., $.content.chunks() or $.content.heading(\"keyword\")) or property (e.g., $.content.codeblock[*])")

        return when (functionNode.name) {
            "heading" -> executeHeadingQuery(functionNode.argument)
            "chapter" -> executeChapterQuery(functionNode.argument)
            "h1", "h2", "h3", "h4", "h5", "h6" -> executeHeadingLevelQuery(functionNode.name, functionNode.argument)
            "grep" -> executeGrepQuery(functionNode.argument)
            "codeblock" -> executeCodeBlockQuery(nodes.drop(1))
            "table" -> executeTableQuery(nodes.drop(1))
            "chunks" -> executeAllChunksQuery()
            "all" -> executeAllChunksQuery()
            else -> DocQLResult.Error("Unknown content function '${functionNode.name}'")
        }
    }

    /**
     * Execute code query - not applicable for markdown files
     * Returns Empty since markdown files don't have source code structure
     */
    override suspend fun executeCodeQuery(nodes: List<DocQLNode>): DocQLResult {
        // Markdown files don't have code structure, return empty
        return DocQLResult.Empty
    }

    /**
     * Execute all chunks query: $.content.chunks() or $.content.all()
     */
    private suspend fun executeAllChunksQuery(): DocQLResult {
        if (parserService == null || documentFile == null) {
            return DocQLResult.Error("No parser service available")
        }

        // Query all headings with empty keyword to get all content chunks
        // This is a workaround since there's no direct "get all chunks" method
        val allHeadings = documentFile.toc?.let { flattenToc(it) } ?: emptyList()

        if (allHeadings.isEmpty()) {
            return DocQLResult.Empty
        }

        // Query each heading to get its content
        val chunks = mutableListOf<DocumentChunk>()
        for (heading in allHeadings) {
            val chunkContent = parserService.queryChapter(heading.anchor.removePrefix("#"))
            if (chunkContent != null) {
                chunks.add(chunkContent)
            }
        }

        return if (chunks.isEmpty()) {
            DocQLResult.Empty
        } else {
            DocQLResult.Chunks(mapOf(documentFile.path to chunks))
        }
    }

    /**
     * Execute heading query: $.content.heading("keyword")
     */
    private suspend fun executeHeadingQuery(keyword: String): DocQLResult {
        if (parserService == null || documentFile == null) {
            return DocQLResult.Error("No parser service available")
        }

        val chunks = parserService.queryHeading(keyword)
        return if (chunks.isEmpty()) {
            // Try partial matching with more flexible search by querying TOC
            val allHeadings = documentFile.toc.let { flattenToc(it) }
            val partialMatches = mutableListOf<DocumentChunk>()

            for (heading in allHeadings) {
                if (heading.title.contains(keyword, ignoreCase = true)) {
                    val chunk = parserService.queryChapter(heading.anchor.removePrefix("#"))
                    if (chunk != null) {
                        partialMatches.add(chunk)
                    }
                }
            }

            if (partialMatches.isNotEmpty()) {
                DocQLResult.Chunks(mapOf(documentFile.path to partialMatches))
            } else {
                DocQLResult.Empty
            }
        } else {
            DocQLResult.Chunks(mapOf(documentFile.path to chunks))
        }
    }

    /**
     * Execute chapter query: $.content.chapter("id")
     */
    private suspend fun executeChapterQuery(id: String): DocQLResult {
        if (parserService == null || documentFile == null) {
            return DocQLResult.Error("No parser service available")
        }

        val chunk = parserService.queryChapter(id)
        return if (chunk != null) {
            DocQLResult.Chunks(mapOf(documentFile.path to listOf(chunk)))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute heading level query: $.content.h1("keyword")
     */
    private fun executeHeadingLevelQuery(level: String, keyword: String): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        val levelNum = level.substring(1).toIntOrNull() ?: return DocQLResult.Error("Invalid heading level")

        val matchingTocs = flattenToc(documentFile.toc).filter {
            it.level == levelNum && it.title.contains(keyword, ignoreCase = true)
        }

        return if (matchingTocs.isNotEmpty()) {
            DocQLResult.TocItems(mapOf(documentFile.path to matchingTocs))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute grep query: $.content.grep("pattern")
     */
    private suspend fun executeGrepQuery(pattern: String): DocQLResult {
        if (parserService == null || documentFile == null) {
            return DocQLResult.Error("No parser service available")
        }

        // Use heading query for flexible search
        val chunks = parserService.queryHeading(pattern)

        return if (chunks.isNotEmpty()) {
            DocQLResult.Chunks(mapOf(documentFile.path to chunks))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute code block query: $.content.codeblock[*]
     * This is for extracting code blocks from markdown/documentation, NOT for querying source code structure.
     *
     * Supports:
     * - $.content.codeblock[*] - All code blocks
     * - $.content.codeblock[0] - First code block
     * - $.content.codeblock[?(@.language=="kotlin")] - Filter by language
     * - $.content.codeblock[?(@.language~="java")] - Language contains "java"
     */
    private fun executeCodeBlockQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null || parserService == null) {
            return DocQLResult.Error("No document loaded")
        }

        val content = parserService.getDocumentContent()
        if (content.isNullOrEmpty()) {
            return DocQLResult.Empty
        }

        // Extract code blocks from content using regex pattern for fenced code blocks
        var codeBlocks = extractCodeBlocks(content)

        // Apply filters from nodes
        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all code blocks - no filtering needed
                }

                is DocQLNode.ArrayAccess.Index -> {
                    codeBlocks = if (node.index < codeBlocks.size) {
                        listOf(codeBlocks[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    codeBlocks = filterCodeBlocks(codeBlocks, node.condition)
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for codeblock query")
                }
            }
        }

        return if (codeBlocks.isNotEmpty()) {
            DocQLResult.CodeBlocks(mapOf(documentFile.path to codeBlocks))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Extract code blocks from markdown content.
     * Matches fenced code blocks: ```language\ncode\n```
     */
    private fun extractCodeBlocks(content: String): List<CodeBlock> {
        val codeBlocks = mutableListOf<CodeBlock>()
        val lines = content.lines()
        var i = 0
        var lineNumber = 1

        while (i < lines.size) {
            val line = lines[i]
            // Check for code block start: ``` or ```language
            if (line.trimStart().startsWith("```")) {
                val startLineNumber = lineNumber
                val trimmedLine = line.trimStart()
                val language = if (trimmedLine.length > 3) {
                    trimmedLine.substring(3).trim().takeIf { it.isNotEmpty() }
                } else null

                // Find the end of the code block
                val codeLines = mutableListOf<String>()
                i++
                lineNumber++

                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                    lineNumber++
                }

                codeBlocks.add(
                    CodeBlock(
                        language = language,
                        code = codeLines.joinToString("\n"),
                        location = Location(
                            anchor = "#codeblock-$startLineNumber",
                            line = startLineNumber
                        )
                    )
                )

                // Skip the closing ```
                if (i < lines.size) {
                    i++
                    lineNumber++
                }
            } else {
                i++
                lineNumber++
            }
        }

        return codeBlocks
    }

    /**
     * Filter code blocks by condition.
     */
    private fun filterCodeBlocks(blocks: List<CodeBlock>, condition: FilterCondition): List<CodeBlock> {
        return blocks.filter { block ->
            when (condition) {
                is FilterCondition.Equals -> {
                    when (condition.property) {
                        "language" -> block.language == condition.value
                        "code" -> block.code == condition.value
                        else -> false
                    }
                }

                is FilterCondition.NotEquals -> {
                    when (condition.property) {
                        "language" -> block.language != condition.value
                        "code" -> block.code != condition.value
                        else -> true
                    }
                }

                is FilterCondition.Contains -> {
                    when (condition.property) {
                        "language" -> block.language?.contains(condition.value, ignoreCase = true) == true
                        "code" -> block.code.contains(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.RegexMatch -> {
                    when (condition.property) {
                        "language" -> block.language?.let { matchesRegex(it, condition.pattern, condition.flags) }
                            ?: false

                        "code" -> matchesRegex(block.code, condition.pattern, condition.flags)
                        else -> false
                    }
                }

                is FilterCondition.StartsWith -> {
                    when (condition.property) {
                        "language" -> block.language?.startsWith(condition.value, ignoreCase = true) == true
                        "code" -> block.code.startsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.EndsWith -> {
                    when (condition.property) {
                        "language" -> block.language?.endsWith(condition.value, ignoreCase = true) == true
                        "code" -> block.code.endsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.GreaterThan, is FilterCondition.GreaterThanOrEquals,
                is FilterCondition.LessThan, is FilterCondition.LessThanOrEquals -> false
            }
        }
    }

    /**
     * Execute table query: $.content.table[*]
     */
    private fun executeTableQuery(nodes: List<DocQLNode>): DocQLResult {
        // TODO: Implement table extraction
        return DocQLResult.Tables(emptyMap())
    }
}
