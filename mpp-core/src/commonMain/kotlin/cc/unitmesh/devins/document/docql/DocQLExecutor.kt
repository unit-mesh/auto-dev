package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*

/**
 * File information for $.files queries
 *
 * @property path Full path of the file
 * @property name File name only
 * @property directory Directory path
 * @property extension File extension
 * @property content File content (loaded from DocumentRegistry/IndexProvider)
 * @property size Content size in characters
 */
data class FileInfo(
    val path: String,
    val name: String = path.substringAfterLast('/'),
    val directory: String = if (path.contains('/')) path.substringBeforeLast('/') else "",
    val extension: String = if (path.contains('.')) path.substringAfterLast('.') else "",
    val content: String? = null,
    val size: Int = content?.length ?: 0
)

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
 * DocQL Executor - executes DocQL queries against document data
 */
class DocQLExecutor(
    private val documentFile: DocumentFile?,
    private val parserService: DocumentParserService?
) {

    /**
     * Execute a DocQL query
     */
    suspend fun execute(query: DocQLQuery): DocQLResult {
        try {
            if (query.nodes.isEmpty()) {
                return DocQLResult.Error("Empty query")
            }

            // First node must be Root
            if (query.nodes[0] !is DocQLNode.Root) {
                return DocQLResult.Error("Query must start with $")
            }

            // Second node determines the context
            if (query.nodes.size < 2) {
                return DocQLResult.Error("Incomplete query")
            }

            return when (val contextNode = query.nodes[1]) {
                is DocQLNode.Property -> {
                    when (contextNode.name) {
                        "toc" -> executeTocQuery(query.nodes.drop(2))
                        "entities" -> executeEntitiesQuery(query.nodes.drop(2))
                        "content" -> executeContentQuery(query.nodes.drop(2))
                        "code" -> executeCodeQuery(query.nodes.drop(2))
                        "files" -> DocQLResult.Error("$.files queries must be executed via DocumentRegistry.queryDocuments()")
                        "structure" -> executeStructureQuery(query.nodes.drop(2))
                        else -> DocQLResult.Error("Unknown context '${contextNode.name}'")
                    }
                }

                else -> DocQLResult.Error("Expected property after $")
            }
        } catch (e: Exception) {
            return DocQLResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Execute TOC query ($.toc[...])
     */
    private fun executeTocQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        var items = documentFile.toc

        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all items (flatten tree)
                    items = flattenToc(items)
                }

                is DocQLNode.ArrayAccess.Index -> {
                    // Return specific index
                    items = if (node.index < items.size) {
                        listOf(items[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    // Filter items
                    items = filterTocItems(items, node.condition)
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for TOC query")
                }
            }
        }

        // Return result with source file information
        return if (items.isNotEmpty()) {
            DocQLResult.TocItems(mapOf(documentFile.path to items))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute entities query ($.entities[...])
     */
    private fun executeEntitiesQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        var items = documentFile.entities

        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all entities
                }

                is DocQLNode.ArrayAccess.Index -> {
                    // Return specific index
                    items = if (node.index < items.size) {
                        listOf(items[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    // Filter entities
                    items = filterEntities(items, node.condition)
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for entities query")
                }
            }
        }

        // Return result with source file information
        return if (items.isNotEmpty()) {
            DocQLResult.Entities(mapOf(documentFile.path to items))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute content query ($.content.heading(...), $.content.h1(...), etc.)
     *
     * Supports both function call syntax and property + array access syntax:
     * - $.content.heading("keyword") - function call
     * - $.content.codeblock[*] - property with array access
     */
    private suspend fun executeContentQuery(nodes: List<DocQLNode>): DocQLResult {
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
     * Execute code query: $.code.classes[*], $.code.class("Name"), $.code.functions[*], etc.
     * This is for querying source code structure (classes, methods, functions) parsed by CodeDocumentParser.
     * 
     * NOTE: This query only works on SOURCE_CODE files. For other document types (markdown, pdf, etc.),
     * it returns Empty result to avoid returning irrelevant results.
     */
    private suspend fun executeCodeQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }
        
        // $.code.* queries should only work on source code files
        // Skip non-source-code files (markdown, pdf, etc.) to avoid returning irrelevant results
        if (documentFile.metadata.formatType != DocumentFormatType.SOURCE_CODE) {
            return DocQLResult.Empty
        }
        
        if (nodes.isEmpty()) {
            return DocQLResult.Error("Code query requires function call or property (e.g., $.code.classes[*], $.code.class(\"Name\"), $.code.functions[*])")
        }
        return when (val firstNode = nodes.firstOrNull()) {
            is DocQLNode.Property -> {
                when (firstNode.name) {
                    "classes" -> executeCodeClassesQuery(nodes.drop(1))
                    "functions" -> executeCodeFunctionsQuery(nodes.drop(1))
                    "methods" -> executeCodeMethodsQuery(nodes.drop(1))
                    else -> DocQLResult.Error("Unknown code property '${firstNode.name}'. Use: classes, functions, methods")
                }
            }

            is DocQLNode.FunctionCall -> {
                when (firstNode.name) {
                    "class" -> executeCodeClassQuery(firstNode.argument)
                    "function" -> executeCodeFunctionQuery(firstNode.argument)
                    "method" -> executeCodeMethodQuery(firstNode.argument)
                    "query" -> executeCodeCustomQuery(firstNode.argument)
                    else -> DocQLResult.Error("Unknown code function '${firstNode.name}'. Use: class(name), function(name), method(name), query(keyword)")
                }
            }

            else -> DocQLResult.Error("Expected property or function call after $.code")
        }
    }

    /**
     * Execute $.code.classes[*] or $.code.classes[filter]
     */
    private fun executeCodeClassesQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        // Get class entities
        var classes = documentFile.entities.filterIsInstance<Entity.ClassEntity>()

        // Apply filters if any
        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all classes
                }

                is DocQLNode.ArrayAccess.Index -> {
                    classes = if (node.index < classes.size) {
                        listOf(classes[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    classes = classes.filter { classEntity ->
                        when (node.condition) {
                            is FilterCondition.Equals -> {
                                when (node.condition.property) {
                                    "name" -> classEntity.name == node.condition.value
                                    "package" -> classEntity.packageName == node.condition.value
                                    else -> false
                                }
                            }

                            is FilterCondition.Contains -> {
                                when (node.condition.property) {
                                    "name" -> classEntity.name.contains(node.condition.value, ignoreCase = true)
                                    "package" -> (classEntity.packageName ?: "").contains(
                                        node.condition.value,
                                        ignoreCase = true
                                    )

                                    else -> false
                                }
                            }

                            else -> false
                        }
                    }
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for classes query")
                }
            }
        }

        return if (classes.isNotEmpty()) {
            DocQLResult.Entities(mapOf(documentFile.path to classes))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute $.code.functions[*] or $.code.functions[filter]
     */
    private fun executeCodeFunctionsQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        // Get function entities
        var functions = documentFile.entities.filterIsInstance<Entity.FunctionEntity>()

        // Apply filters if any
        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all functions
                }

                is DocQLNode.ArrayAccess.Index -> {
                    functions = if (node.index < functions.size) {
                        listOf(functions[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    functions = functions.filter { funcEntity ->
                        when (node.condition) {
                            is FilterCondition.Equals -> {
                                when (node.condition.property) {
                                    "name" -> funcEntity.name == node.condition.value
                                    "signature" -> (funcEntity.signature ?: "") == node.condition.value
                                    else -> false
                                }
                            }

                            is FilterCondition.Contains -> {
                                when (node.condition.property) {
                                    "name" -> funcEntity.name.contains(node.condition.value, ignoreCase = true)
                                    "signature" -> (funcEntity.signature ?: "").contains(
                                        node.condition.value,
                                        ignoreCase = true
                                    )

                                    else -> false
                                }
                            }

                            else -> false
                        }
                    }
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for functions query")
                }
            }
        }

        return if (functions.isNotEmpty()) {
            DocQLResult.Entities(mapOf(documentFile.path to functions))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute $.code.methods[*] - alias for functions query
     */
    private fun executeCodeMethodsQuery(nodes: List<DocQLNode>): DocQLResult {
        return executeCodeFunctionsQuery(nodes)
    }

    /**
     * Execute $.code.class("ClassName") - find specific class and its content
     * 
     * Supports wildcard: $.code.class("*") returns all classes (equivalent to $.code.classes[*])
     */
    private suspend fun executeCodeClassQuery(className: String): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        // Handle wildcard "*" - return all classes (equivalent to $.code.classes[*])
        if (className == "*" || className.isEmpty()) {
            return executeCodeClassesQuery(emptyList())
        }

        if (parserService == null) {
            return DocQLResult.Error("No parser service available")
        }

        // Use heading query to find the class - CodeDocumentParser supports this
        val chunks = parserService.queryHeading(className)

        // Filter to only class-level chunks (not methods)
        val classChunks = chunks.filter { chunk ->
            val title = chunk.chapterTitle ?: ""
            title.startsWith("class ") ||
                    title.startsWith("interface ") ||
                    title.startsWith("enum ")
        }

        return if (classChunks.isNotEmpty()) {
            DocQLResult.Chunks(mapOf(documentFile.path to classChunks))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute $.code.function("functionName") - find specific function/method
     * 
     * Supports wildcard: $.code.function("*") returns all functions (equivalent to $.code.functions[*])
     */
    private suspend fun executeCodeFunctionQuery(functionName: String): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        // Handle wildcard "*" - return all functions (equivalent to $.code.functions[*])
        if (functionName == "*" || functionName.isEmpty()) {
            return executeCodeFunctionsQuery(emptyList())
        }

        return executeCodeCustomQuery(functionName)
    }

    /**
     * Execute grep query: $.content.grep("pattern")
     */
    private suspend fun executeGrepQuery(pattern: String): DocQLResult {
        return executeCodeCustomQuery(pattern)
    }

    /**
     * Execute $.code.method("methodName") - alias for function query
     */
    private suspend fun executeCodeMethodQuery(methodName: String): DocQLResult {
        return executeCodeFunctionQuery(methodName)
    }

    /**
     * Execute $.code.query("keyword") - custom query for any code element
     * 
     * Supports wildcard: $.code.query("*") returns all code chunks
     */
    private suspend fun executeCodeCustomQuery(keyword: String): DocQLResult {
        if (parserService == null || documentFile == null) {
            return DocQLResult.Error("No parser service available")
        }

        // Handle wildcard "*" - return all code chunks
        if (keyword == "*") {
            return executeAllChunksQuery()
        }

        // Use heading query for flexible search
        val chunks = parserService.queryHeading(keyword)

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
     * Execute structure query: $.structure, $.structure.tree(), $.structure.flat()
     * Returns a file structure view from the current document context.
     */
    private fun executeStructureQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded. Use DocumentRegistry.queryDocuments() for $.structure queries across all documents.")
        }

        // For single document, return its path structure
        val paths = listOf(documentFile.path)
        val tree = buildFileTree(paths)

        return DocQLResult.Structure(
            tree = tree,
            paths = paths,
            directoryCount = countDirectories(paths),
            fileCount = paths.size
        )
    }

    /**
     * Build a tree-like string representation of file paths.
     */
    private fun buildFileTree(paths: List<String>): String {
        return DocumentRegistry.buildStructureTree(paths)
    }

    /**
     * Count unique directories from file paths.
     */
    private fun countDirectories(paths: List<String>): Int {
        return DocumentRegistry.countDirectories(paths)
    }

    private fun executeTableQuery(nodes: List<DocQLNode>): DocQLResult {
        // TODO: Implement table extraction
        return DocQLResult.Tables(emptyMap())
    }

    private fun flattenToc(items: List<TOCItem>): List<TOCItem> {
        val result = mutableListOf<TOCItem>()
        for (item in items) {
            result.add(item)
            result.addAll(flattenToc(item.children))
        }
        return result
    }

    /**
     * Filter TOC items by condition
     */
    private fun filterTocItems(items: List<TOCItem>, condition: FilterCondition): List<TOCItem> {
        val flattened = flattenToc(items)
        return flattened.filter { item ->
            when (condition) {
                is FilterCondition.Equals -> {
                    when (condition.property) {
                        "level" -> item.level.toString() == condition.value
                        "title" -> item.title == condition.value
                        else -> false
                    }
                }

                is FilterCondition.NotEquals -> {
                    when (condition.property) {
                        "level" -> item.level.toString() != condition.value
                        "title" -> item.title != condition.value
                        else -> true
                    }
                }

                is FilterCondition.Contains -> {
                    when (condition.property) {
                        "title" -> item.title.contains(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.RegexMatch -> {
                    when (condition.property) {
                        "title" -> matchesRegex(item.title, condition.pattern, condition.flags)
                        else -> false
                    }
                }

                is FilterCondition.GreaterThan -> {
                    when (condition.property) {
                        "level" -> item.level > condition.value
                        "page" -> (item.page ?: 0) > condition.value
                        else -> false
                    }
                }

                is FilterCondition.GreaterThanOrEquals -> {
                    when (condition.property) {
                        "level" -> item.level >= condition.value
                        "page" -> (item.page ?: 0) >= condition.value
                        else -> false
                    }
                }

                is FilterCondition.LessThan -> {
                    when (condition.property) {
                        "level" -> item.level < condition.value
                        "page" -> (item.page ?: 0) < condition.value
                        else -> false
                    }
                }

                is FilterCondition.LessThanOrEquals -> {
                    when (condition.property) {
                        "level" -> item.level <= condition.value
                        "page" -> (item.page ?: 0) <= condition.value
                        else -> false
                    }
                }

                is FilterCondition.StartsWith -> {
                    when (condition.property) {
                        "title" -> item.title.startsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.EndsWith -> {
                    when (condition.property) {
                        "title" -> item.title.endsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }
            }
        }
    }

    /**
     * Filter entities by condition
     */
    private fun filterEntities(items: List<Entity>, condition: FilterCondition): List<Entity> {
        return items.filter { entity ->
            when (condition) {
                is FilterCondition.Equals -> {
                    when (condition.property) {
                        "name" -> entity.name == condition.value
                        "type" -> {
                            val entityType = when (entity) {
                                is Entity.Term -> "Term"
                                is Entity.API -> "API"
                                is Entity.ClassEntity -> "ClassEntity"
                                is Entity.FunctionEntity -> "FunctionEntity"
                                is Entity.ConstructorEntity -> "ConstructorEntity"
                            }
                            entityType == condition.value
                        }

                        else -> false
                    }
                }

                is FilterCondition.NotEquals -> {
                    when (condition.property) {
                        "name" -> entity.name != condition.value
                        "type" -> {
                            val entityType = when (entity) {
                                is Entity.Term -> "Term"
                                is Entity.API -> "API"
                                is Entity.ClassEntity -> "ClassEntity"
                                is Entity.FunctionEntity -> "FunctionEntity"
                                is Entity.ConstructorEntity -> "ConstructorEntity"
                            }
                            entityType != condition.value
                        }

                        else -> true
                    }
                }

                is FilterCondition.Contains -> {
                    when (condition.property) {
                        "name" -> entity.name.contains(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.RegexMatch -> {
                    when (condition.property) {
                        "name" -> matchesRegex(entity.name, condition.pattern, condition.flags)
                        else -> false
                    }
                }

                is FilterCondition.StartsWith -> {
                    when (condition.property) {
                        "name" -> entity.name.startsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.EndsWith -> {
                    when (condition.property) {
                        "name" -> entity.name.endsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.GreaterThan, is FilterCondition.GreaterThanOrEquals,
                is FilterCondition.LessThan, is FilterCondition.LessThanOrEquals -> false
            }
        }
    }

    /**
     * Check if text matches regex pattern with flags
     * Note: Only IGNORE_CASE and MULTILINE are supported across all Kotlin platforms (JVM, JS, Native)
     * The 's' flag (DOT_MATCHES_ALL) is not available in Kotlin/JS, so we ignore it for cross-platform compatibility
     */
    private fun matchesRegex(text: String, pattern: String, flags: String): Boolean {
        return try {
            val options = mutableSetOf<RegexOption>()
            if (flags.contains('i')) options.add(RegexOption.IGNORE_CASE)
            if (flags.contains('m')) options.add(RegexOption.MULTILINE)
            // Note: RegexOption.DOT_MATCHES_ALL is not available in Kotlin/JS
            // For cross-platform compatibility, we skip the 's' flag

            val regex = Regex(pattern, options)
            regex.containsMatchIn(text)
        } catch (e: Exception) {
            // Invalid regex pattern
            false
        }
    }
}

/**
 * Convenience function to parse and execute DocQL query
 */
suspend fun executeDocQL(
    queryString: String,
    documentFile: DocumentFile?,
    parserService: DocumentParserService?
): DocQLResult {
    return try {
        val query = parseDocQL(queryString)
        val executor = DocQLExecutor(documentFile, parserService)
        executor.execute(query)
    } catch (e: DocQLException) {
        DocQLResult.Error(e.message ?: "Parse error")
    } catch (e: Exception) {
        DocQLResult.Error(e.message ?: "Execution error")
    }
}

