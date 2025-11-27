package cc.unitmesh.devins.document

import cc.unitmesh.devins.document.docql.*
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Document Registry - manages document parsers and parsed documents
 *
 * This registry provides a centralized way to:
 * 1. Register and retrieve document parsers for different formats
 * 2. Cache parsed documents for efficient DocQL queries
 * 3. Support platform-specific parser initialization
 *
 * Usage:
 * ```kotlin
 * // Register a parser (done automatically by platform initialization)
 * DocumentRegistry.registerParser(DocumentFormatType.PDF, TikaDocumentParser())
 *
 * // Parse and register a document
 * val parser = DocumentRegistry.getParser(DocumentFormatType.PDF)
 * val parsedDoc = parser?.parse(file, content)
 * DocumentRegistry.registerDocument(file.path, parsedDoc, parser)
 *
 * // Query via DocQL
 * val result = DocumentRegistry.queryDocument(filePath, "$.content.heading('Introduction')")
 * ```
 */
object DocumentRegistry {
    /**
     * 获取所有 root 级别（没有 / 的一级文件名）的已注册文档路径
     * 例如："README.md"，而不是 "docs/readme.md"
     */
    fun getRootRegisteredPaths(): List<String> {
        return documentCache.keys.filter { !it.contains("/") }
    }

    /**
     * Cache of parsed documents with their parsers
     * Key: document path, Value: Pair(DocumentFile, Parser)
     */
    private val documentCache = mutableMapOf<String, Pair<DocumentTreeNode, DocumentParserService>>()

    /**
     * Flag to track if platform-specific parsers have been initialized
     */
    private var initialized = false

    /**
     * Optional provider for accessing indexed documents from persistent storage
     * This allows querying documents that were indexed but not in memory cache
     */
    private var indexProvider: DocumentIndexProvider? = null

    init {
        // Register Markdown parser (available on all platforms)
        DocumentParserFactory.registerParser(DocumentFormatType.MARKDOWN) { MarkdownDocumentParser() }
    }

    /**
     * Initialize platform-specific document parsers
     * This should be called automatically by platform-specific code
     */
    fun initializePlatformParsers() {
        if (!initialized) {
            initialized = true
            logger.debug { "Initializing platform-specific document parsers" }
            platformInitialize()
        }
    }

    /**
     * Register a document with its parser for future queries
     *
     * @param path Document path (unique identifier)
     * @param document Parsed document tree node
     * @param parser Parser service used for queries
     */
    fun registerDocument(
        path: String,
        document: DocumentTreeNode,
        parser: DocumentParserService
    ) {
        documentCache[path] = document to parser
        logger.debug { "Registered document: $path" }
    }

    /**
     * Get a registered document and its parser
     *
     * @param path Document path
     * @return Pair of (DocumentTreeNode, Parser) or null if not found
     */
    fun getDocument(path: String): Pair<DocumentTreeNode, DocumentParserService>? {
        return documentCache[path]
    }

    /**
     * Get parser for a specific format
     * Ensures platform parsers are initialized
     *
     * @param formatType Document format type
     * @return Parser service or null if not supported
     */
    fun getParser(formatType: DocumentFormatType): DocumentParserService? {
        initializePlatformParsers()
        return DocumentParserFactory.createParser(formatType)
    }

    /**
     * Get parser for a file path (auto-detect format)
     *
     * @param filePath File path
     * @return Parser service or null if format not supported
     */
    fun getParserForFile(filePath: String): DocumentParserService? {
        initializePlatformParsers()
        return DocumentParserFactory.createParserForFile(filePath)
    }

    /**
     * Query a registered document using DocQL
     * If document is not in memory but available in index, it will be loaded first
     * If documentPath is null or "null", falls back to querying all documents
     *
     * @param documentPath Document path (null or "null" for global search)
     * @param docqlQuery DocQL query string (e.g., "$.content.heading('title')")
     * @return Query result or null if document not found (for single document queries)
     */
    suspend fun queryDocument(documentPath: String?, docqlQuery: String): DocQLResult? {
        if (documentPath == null || documentPath == "null") {
            logger.info { "Document path is null, falling back to global search across all documents" }
            return queryDocuments(docqlQuery)
        }

        var docPair = getDocument(documentPath)
        if (docPair == null && indexProvider != null) {
            val loaded = loadFromIndex(documentPath)
            if (loaded) {
                docPair = getDocument(documentPath)
            }
        }

        if (docPair == null) {
            return null
        }

        val (document, parser) = docPair

        if (document !is DocumentFile) {
            logger.warn { "Document at $documentPath is not a file" }
            return null
        }

        return try {
            val query = parseDocQL(docqlQuery)
            val executor = DocQLExecutor(document, parser)
            executor.execute(query)
        } catch (e: Exception) {
            // Only log once per unique query error to avoid spam
            logger.debug { "Failed to execute DocQL query: $docqlQuery, Caused by: '${e.message}'" }
            DocQLResult.Error(e.message ?: "Query execution failed")
        }
    }

    /**
     * Query multiple documents using DocQL and merge results with source file information
     * This is the recommended method for querying across all documents
     *
     * @param docqlQuery DocQL query string (e.g., "$.content.heading('title')")
     * @param documentPaths Optional list of specific documents to query (queries all if null)
     * @return Merged query result with source file information for each item
     */
    suspend fun queryDocuments(docqlQuery: String, documentPaths: List<String>? = null): DocQLResult {
        val pathsToQuery = documentPaths ?: getAllAvailablePaths()

        if (pathsToQuery.isEmpty()) {
            return DocQLResult.Empty
        }

        // Special handling for $.files queries - these work across all files
        if (docqlQuery.trim().startsWith("\$.files")) {
            return try {
                val query = parseDocQL(docqlQuery)
                executeFilesQuery(query, pathsToQuery)
            } catch (e: Exception) {
                logger.debug { "Failed to execute files query: $docqlQuery, Caused by: '${e.message}'" }
                DocQLResult.Error(e.message ?: "Files query execution failed")
            }
        }
        
        // Special handling for $.structure queries - these work across all files
        if (docqlQuery.trim().startsWith("\$.structure")) {
            return try {
                executeStructureQuery(pathsToQuery)
            } catch (e: Exception) {
                logger.debug { "Failed to execute structure query: $docqlQuery, Caused by: '${e.message}'" }
                DocQLResult.Error(e.message ?: "Structure query execution failed")
            }
        }

        // Query each document and merge results
        val resultsByFile = mutableMapOf<String, DocQLResult>()

        for (path in pathsToQuery) {
            val result = queryDocument(path, docqlQuery)
            if (result != null && result !is DocQLResult.Empty && result !is DocQLResult.Error) {
                resultsByFile[path] = result
            }
        }

        // Merge results by type
        return mergeQueryResults(resultsByFile)
    }

    /**
     * Execute $.structure query across multiple documents
     * Returns a tree-like view of all file paths
     */
    private fun executeStructureQuery(paths: List<String>): DocQLResult {
        if (paths.isEmpty()) {
            return DocQLResult.Empty
        }
        
        val tree = buildStructureTree(paths)
        val directoryCount = countDirectories(paths)
        
        return DocQLResult.Structure(
            tree = tree,
            paths = paths,
            directoryCount = directoryCount,
            fileCount = paths.size
        )
    }

    /**
     * Build a tree-like string representation of file paths.
     */
    fun buildStructureTree(paths: List<String>): String {
        if (paths.isEmpty()) return "No files found."

        // Build tree structure
        data class TreeNode(
            val name: String,
            val children: MutableMap<String, TreeNode> = mutableMapOf(),
            var isFile: Boolean = false
        )

        val root = TreeNode("")

        for (path in paths.sorted()) {
            val parts = path.split('/')
            var current = root

            for ((index, part) in parts.withIndex()) {
                if (part.isEmpty()) continue
                val isLast = index == parts.size - 1

                if (!current.children.containsKey(part)) {
                    current.children[part] = TreeNode(part)
                }
                current = current.children[part]!!
                if (isLast) current.isFile = true
            }
        }

        // Render tree to string
        fun renderNode(node: TreeNode, prefix: String, isLast: Boolean, isRoot: Boolean): String {
            val sb = StringBuilder()

            if (!isRoot) {
                val connector = if (isLast) "`-- " else "|-- "
                val icon = if (node.isFile) "" else "/"
                sb.appendLine("$prefix$connector${node.name}$icon")
            }

            val childPrefix = if (isRoot) "" else prefix + (if (isLast) "    " else "|   ")
            // Sort: directories first, then files, both alphabetically
            val sortedChildren = node.children.values.sortedWith(compareBy({ it.children.isEmpty() }, { it.name }))

            for ((index, child) in sortedChildren.withIndex()) {
                val childIsLast = index == sortedChildren.size - 1
                sb.append(renderNode(child, childPrefix, childIsLast, false))
            }

            return sb.toString()
        }

        return renderNode(root, "", true, true).trimEnd()
    }

    /**
     * Count unique directories from file paths.
     */
    fun countDirectories(paths: List<String>): Int {
        val directories = mutableSetOf<String>()
        for (path in paths) {
            val parts = path.split('/')
            for (i in 1 until parts.size) {
                directories.add(parts.subList(0, i).joinToString("/"))
            }
        }
        return directories.size
    }

    /**
     * Execute $.files query across multiple documents
     */
    private suspend fun executeFilesQuery(query: DocQLQuery, paths: List<String>): DocQLResult {
        val files = paths.map { path ->
            FileInfo(
                path = path,
                name = path.substringAfterLast('/'),
                directory = if (path.contains('/')) path.substringBeforeLast('/') else "",
                extension = if (path.contains('.')) path.substringAfterLast('.') else "",
                content = null, // Content not loaded by default for performance
                size = 0
            )
        }

        // Apply filters from query
        var filteredFiles = files
        for (node in query.nodes.drop(2)) { // Skip $ and .files
            when (node) {
                is DocQLNode.ArrayAccess.Filter -> {
                    filteredFiles = filterFiles(filteredFiles, node.condition)
                }

                is DocQLNode.ArrayAccess.Index -> {
                    filteredFiles = if (node.index < filteredFiles.size) {
                        listOf(filteredFiles[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.All -> {
                    // No filtering needed - return all files
                }

                else -> {
                    // Ignore other node types for files query
                }
            }
        }

        return if (filteredFiles.isNotEmpty()) {
            DocQLResult.Files(filteredFiles)
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Filter files by condition
     */
    private fun filterFiles(items: List<FileInfo>, condition: FilterCondition): List<FileInfo> {
        return items.filter { file ->
            when (condition) {
                is FilterCondition.Equals -> {
                    when (condition.property) {
                        "path" -> file.path == condition.value
                        "name" -> file.name == condition.value
                        "directory" -> file.directory == condition.value
                        "extension" -> file.extension == condition.value
                        else -> false
                    }
                }

                is FilterCondition.Contains -> {
                    when (condition.property) {
                        "path" -> file.path.contains(condition.value, ignoreCase = true)
                        "name" -> file.name.contains(condition.value, ignoreCase = true)
                        "directory" -> file.directory.contains(condition.value, ignoreCase = true)
                        "extension" -> file.extension.contains(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                else -> false
            }
        }
    }

    /**
     * Merge query results from multiple files into a single result with source information
     */
    private fun mergeQueryResults(resultsByFile: Map<String, DocQLResult>): DocQLResult {
        if (resultsByFile.isEmpty()) {
            return DocQLResult.Empty
        }

        // Group results by type
        val tocItemsByFile = mutableMapOf<String, List<TOCItem>>()
        val entitiesByFile = mutableMapOf<String, List<Entity>>()
        val chunksByFile = mutableMapOf<String, List<DocumentChunk>>()
        val codeBlocksByFile = mutableMapOf<String, List<CodeBlock>>()
        val tablesByFile = mutableMapOf<String, List<TableBlock>>()

        for ((path, result) in resultsByFile) {
            when (result) {
                is DocQLResult.TocItems -> {
                    // Old format: single file result
                    tocItemsByFile[path] = result.itemsByFile.values.flatten()
                }

                is DocQLResult.Entities -> {
                    entitiesByFile[path] = result.itemsByFile.values.flatten()
                }

                is DocQLResult.Chunks -> {
                    chunksByFile[path] = result.itemsByFile.values.flatten()
                }

                is DocQLResult.CodeBlocks -> {
                    codeBlocksByFile[path] = result.itemsByFile.values.flatten()
                }

                is DocQLResult.Tables -> {
                    tablesByFile[path] = result.itemsByFile.values.flatten()
                }

                else -> {} // Skip Empty, Error, Files
            }
        }

        // Return the first non-empty result type
        return when {
            tocItemsByFile.isNotEmpty() -> DocQLResult.TocItems(tocItemsByFile)
            entitiesByFile.isNotEmpty() -> DocQLResult.Entities(entitiesByFile)
            chunksByFile.isNotEmpty() -> DocQLResult.Chunks(chunksByFile)
            codeBlocksByFile.isNotEmpty() -> DocQLResult.CodeBlocks(codeBlocksByFile)
            tablesByFile.isNotEmpty() -> DocQLResult.Tables(tablesByFile)
            else -> DocQLResult.Empty
        }
    }

    /**
     * Clear document cache
     */
    fun clearCache() {
        documentCache.clear()
        logger.info { "Document cache cleared" }
    }

    /**
     * Set the document index provider
     * This allows access to documents stored in persistent storage
     *
     * @param provider Document index provider or null to clear
     */
    fun setIndexProvider(provider: DocumentIndexProvider?) {
        indexProvider = provider
        logger.info { "Document index provider ${if (provider != null) "set" else "cleared"}" }
    }

    /**
     * Get the current document index provider
     */
    fun getIndexProvider(): DocumentIndexProvider? {
        return indexProvider
    }

    /**
     * Get all registered document paths (in-memory only)
     * For all available paths including indexed documents, use getAllAvailablePaths()
     */
    fun getRegisteredPaths(): List<String> {
        return documentCache.keys.toList()
    }

    /**
     * Get all available document paths (both in-memory and indexed)
     *
     * @return Combined list of paths from memory cache and index provider
     */
    suspend fun getAllAvailablePaths(): List<String> {
        val memoryPaths = documentCache.keys.toSet()
        val indexedPaths = indexProvider?.getIndexedPaths()?.toSet() ?: emptySet()
        return (memoryPaths + indexedPaths).sorted()
    }

    /**
     * Get a compressed summary of available document paths
     * When there are many documents (>20), this returns a tree-like structure
     * to save prompt space.
     *
     * @param threshold Maximum number of files before compression kicks in (default: 20)
     * @return Formatted string summary of documents
     */
    suspend fun getCompressedPathsSummary(threshold: Int = 20): String {
        val allPaths = getAllAvailablePaths()

        if (allPaths.isEmpty()) {
            return "No documents available."
        }

        // When there are MANY files (>= threshold), use COMPRESSED format (simple list to save space)
        if (allPaths.size >= threshold) {
            return buildString {
                appendLine("Available documents (${allPaths.size}):")
                allPaths.forEach { path ->
                    appendLine("  - $path")
                }
            }.trimEnd()
        }

        // When there are FEW files (< threshold), use EXPANDED format (tree structure with more details)
        val pathTree = buildPathTree(allPaths)

        return buildString {
            appendLine("Available documents (${allPaths.size} total - showing directory structure):")
            appendLine()
            appendLine("Use DocQL `\\$.files[*]` to list all files, or `\\$.files[?(@.path contains \\\"pattern\\\")]` to filter.")
            appendLine()
            renderPathTree(this, pathTree, "", true)
            appendLine()
            appendLine("💡 Tip: Query specific directories to reduce context size, e.g.:")
            appendLine("   \\$.files[?(@.path contains \\\"docs\\\")]")
        }.trimEnd()
    }

    /**
     * Build a tree structure from flat paths
     */
    private fun buildPathTree(paths: List<String>): PathNode {
        val root = PathNode("/", mutableListOf(), 0)

        for (path in paths) {
            val parts = path.split('/')
            var current = root

            for ((index, part) in parts.withIndex()) {
                if (part.isEmpty()) continue

                val isFile = index == parts.size - 1
                var child = current.children.find { it.name == part }

                if (child == null) {
                    child = PathNode(part, mutableListOf(), if (isFile) 1 else 0)
                    current.children.add(child)
                } else if (isFile) {
                    child.fileCount++
                }

                current = child
            }
        }

        // Propagate file counts up the tree
        fun propagateCount(node: PathNode): Int {
            var total = node.fileCount
            for (child in node.children) {
                total += propagateCount(child)
            }
            node.totalCount = total
            return total
        }
        propagateCount(root)

        return root
    }

    /**
     * Render path tree in a compact format
     */
    private fun renderPathTree(sb: StringBuilder, node: PathNode, prefix: String, isRoot: Boolean) {
        if (isRoot) {
            // Render root children directly
            for ((index, child) in node.children.withIndex()) {
                val isLast = index == node.children.size - 1
                renderPathTreeNode(sb, child, "", isLast)
            }
        } else {
            renderPathTreeNode(sb, node, prefix, true)
        }
    }

    private fun renderPathTreeNode(sb: StringBuilder, node: PathNode, prefix: String, isLast: Boolean) {
        val connector = if (isLast) "└── " else "├── "
        val extension = if (isLast) "    " else "│   "

        // Show directory with file count or individual file
        if (node.children.isEmpty()) {
            // It's a file
            sb.appendLine("$prefix$connector${node.name}")
        } else {
            // It's a directory
            val countInfo = if (node.totalCount > 0) " (${node.totalCount} files)" else ""
            sb.appendLine("$prefix$connector${node.name}/$countInfo")

            // Render children
            for ((index, child) in node.children.withIndex()) {
                val childIsLast = index == node.children.size - 1
                renderPathTreeNode(sb, child, prefix + extension, childIsLast)
            }
        }
    }

    /**
     * Data class for path tree nodes
     */
    private data class PathNode(
        val name: String,
        val children: MutableList<PathNode>,
        var fileCount: Int,
        var totalCount: Int = 0
    )

    /**
     * Check if a document is registered in memory
     */
    fun isDocumentRegistered(path: String): Boolean {
        return documentCache.containsKey(path)
    }

    /**
     * Check if a document is available (either in memory or indexed)
     *
     * @param path Document path
     * @return true if document is in memory or indexed
     */
    suspend fun isDocumentAvailable(path: String): Boolean {
        if (documentCache.containsKey(path)) {
            return true
        }
        return indexProvider?.isIndexed(path) ?: false
    }

    /**
     * Load a document from index provider and register it in memory
     * This is automatically called by queryDocument when needed
     *
     * @param path Document path
     * @return true if successfully loaded and registered
     */
    suspend fun loadFromIndex(path: String): Boolean {
        if (documentCache.containsKey(path)) {
            return true // Already in memory
        }

        val provider = indexProvider ?: return false
        val (content, formatType) = provider.loadIndexedDocument(path)

        if (content == null || formatType == null) {
            return false
        }

        return try {
            val parser = DocumentParserFactory.createParser(formatType)
            if (parser == null) {
                logger.warn { "No parser available for format: $formatType" }
                return false
            }

            val docFile = DocumentFile(
                name = path.substringAfterLast('/'),
                path = path,
                metadata = DocumentMetadata(
                    lastModified = 0,
                    fileSize = content.length.toLong(),
                    formatType = formatType
                )
            )

            val parsedDoc = parser.parse(docFile, content)
            registerDocument(path, parsedDoc, parser)
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to load document from index: $path" }
            false
        }
    }
}

/**
 * Platform-specific initialization function
 * Implemented via expect/actual pattern
 */
expect fun platformInitialize()

