package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.DocumentReranker
import cc.unitmesh.agent.scoring.DocumentRerankerConfig
import cc.unitmesh.agent.scoring.ScoredItem
import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.devins.document.DocumentRegistry
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.docql.DocQLResult
import cc.unitmesh.devins.document.DocumentChunk
import cc.unitmesh.devins.document.TOCItem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

@Serializable
data class DocQLParams(
    val query: String,
    val documentPath: String? = null,
    val maxResults: Int? = 20
)

object DocQLSchema : DeclarativeToolSchema(
    description = """
        Executes a DocQL query against available documents (both in-memory and indexed).
        
        ## SMART SEARCH (Recommended)
        Simply provide a keyword or phrase, and the tool will automatically:
        1. Search for Classes and Functions matching the keyword (High Priority)
        2. Search for Headings and Content matching the keyword (Medium Priority)
        3. Rerank results to show the most relevant code and documentation first.
        
        **Example:** `{"query": "Auth"}` -> Finds `AuthService` class, `authenticate` function, and "Authentication" sections.
        
        ## ADVANCED: DIRECT DOCQL QUERIES
        For precise control, use standard DocQL syntax (starts with `$.`):
        
        ### 1. Document Queries ($.content.*, $.toc[*])
        **For:** Markdown, text files, documentation (.md, .txt, README)
        **Examples:**
        - $.content.heading("keyword") - Find sections by heading
        - $.content.chunks() - Get all content chunks
        - $.toc[*] - Get table of contents
        
        ### 2. Code Queries ($.code.*)
        **For:** Source code files (.kt, .java, .py, .js, .ts, .go, .rs, .cs)
        **Parser:** TreeSitter-based with full code structure
        **Examples:**
        - $.code.class("ClassName") - Find class with full source code
        - $.code.function("functionName") - Find function/method with implementation
        - $.code.classes[*] - List all classes
        - $.code.functions[*] - List all functions/methods
        
        ## Parameters
        - **query** (required): The keyword (Smart Search) or DocQL query string (Advanced)
        - **documentPath** (optional): Target specific document by path
        - **maxResults** (optional): Limit results (default: 20)
    """.trimIndent(),
    properties = mapOf(
        "query" to string(
            description = "The keyword to search for (Smart Search) or a specific DocQL query (e.g., '$.content.heading(\"Introduction\")').",
            required = true
        ),
        "documentPath" to string(
            description = """
                The path of the document to query (e.g., 'design-system-color.md').
                Use this to target specific documents when their names match your keywords.
                Check the available documents list and match keywords before querying.
                If omitted, searches all registered documents.
            """.trimIndent(),
            required = false
        ),
        "maxResults" to integer(
            description = """
                Maximum number of results to return. Default is 20.
                Use lower values for quick overview, higher values for comprehensive search.
                Note: Very high values may exceed context limits for large result sets.
            """.trimIndent(),
            required = false
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return """
            /$toolName
            ```json
            {"query": "$.content.heading(\"Introduction\")"}
            ```
            
            Or specific DocQL:
            /$toolName
            ```json
            {"query": "$.content.heading(\"Introduction\")", "documentPath": "path/to/doc.md"}
            ```
        """.trimIndent()
    }
}

class DocQLInvocation(
    params: DocQLParams,
    tool: DocQLTool
) : BaseToolInvocation<DocQLParams, ToolResult>(params, tool) {

    override fun getDescription(): String = if (params.documentPath != null) {
        "Executing DocQL query: ${params.query} on ${params.documentPath}"
    } else {
        "Executing DocQL query: ${params.query} on all available documents"
    }

    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return ToolErrorUtils.safeExecute(ToolErrorType.COMMAND_FAILED) {
            // Check if it's a raw keyword (Smart Search) or a DocQL query
            if (!params.query.trim().startsWith("$.")) {
                executeSmartSearch(params.query, params.documentPath, params.maxResults ?: 20)
            } else {
                // Standard DocQL execution
                if (params.documentPath != null) {
                    querySingleDocument(params.documentPath, params.query)
                } else {
                    queryAllDocuments(params.query)
                }
            }
        }
    }

    private suspend fun executeSmartSearch(keyword: String, documentPath: String?, maxResults: Int): ToolResult {
        logger.info { "Executing Smart Search for keyword: '$keyword'" }
        
        // Create reranker with appropriate config
        val reranker = DocumentReranker(DocumentRerankerConfig(
            maxResults = maxResults,
            minScoreThreshold = 5.0  // Filter out very low relevance items
        ))
        
        // 1. Define query sources for multi-channel retrieval
        val queryChannels = mapOf(
            "class" to "$.code.class(\"$keyword\")",
            "function" to "$.code.function(\"$keyword\")",
            "heading" to "$.content.heading(\"$keyword\")",
            "toc" to "$.toc[?(@.title contains \"$keyword\")]"
        )

        // 2. Execute all channels in parallel
        val channelResults = coroutineScope {
            queryChannels.map { (channel, query) ->
                async {
                    try {
                        val result: DocQLResult? = if (documentPath != null) {
                            DocumentRegistry.queryDocument(documentPath, query)
                        } else {
                            DocumentRegistry.queryDocuments(query)
                        }
                        if (result != null) channel to result else null
                    } catch (e: Exception) {
                        logger.warn { "Smart search channel '$channel' failed: ${e.message}" }
                        null
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }

        // 3. Convert results to ranked lists for RRF fusion
        val rankedLists = mutableMapOf<String, List<SearchItem>>()
        val itemMetadata = mutableMapOf<SearchItem, Pair<Any, String?>>() // item -> (original, filePath)
        
        channelResults.forEach { (channel, result: DocQLResult) ->
            val items = mutableListOf<SearchItem>()
            collectSearchItems(result, items, itemMetadata)
            if (items.isNotEmpty()) {
                rankedLists[channel] = items
            }
        }

        // 4. Use Reranker for RRF fusion + BM25 scoring
        if (rankedLists.isEmpty()) {
            return executeFallbackSearch(keyword, documentPath, maxResults, reranker)
        }

        val rerankResult = reranker.rerank(
            rankedLists = rankedLists,
            query = keyword,
            segmentExtractor = { it.segment }
        )

        if (rerankResult.items.isEmpty()) {
            return executeFallbackSearch(keyword, documentPath, maxResults, reranker)
        }

        // 5. Convert to ScoredResult for formatting
        val scoredResults = rerankResult.items.map { scoredItem ->
            val (originalItem, filePath) = itemMetadata[scoredItem.item] ?: (scoredItem.item to null)
            ScoredResult(
                item = originalItem,
                score = scoredItem.score,
                uniqueId = scoredItem.item.segment.id ?: scoredItem.item.hashCode().toString(),
                preview = scoredItem.item.segment.text.take(100).replace("\n", " "),
                filePath = filePath
            )
        }

        return ToolResult.Success(formatSmartResult(scoredResults, keyword, rerankResult.truncated, rerankResult.totalCount))
    }

    /**
     * Wrapper for search items to enable RRF fusion
     */
    private data class SearchItem(
        val segment: TextSegment
    )

    private fun collectSearchItems(
        result: DocQLResult,
        items: MutableList<SearchItem>,
        itemMetadata: MutableMap<SearchItem, Pair<Any, String?>>
    ) {
        when (result) {
            is DocQLResult.Entities -> {
                result.itemsByFile.forEach { (file, entities) ->
                    entities.forEach { entity ->
                        val type = when (entity) {
                            is Entity.ClassEntity -> "class"
                            is Entity.FunctionEntity -> "function"
                            else -> "entity"
                        }
                        val segment = TextSegment(
                            text = entity.name,
                            metadata = mapOf(
                                "type" to type,
                                "name" to entity.name,
                                "id" to "${file}:${entity.name}:${entity.location.line}",
                                "filePath" to file
                            )
                        )
                        val item = SearchItem(segment)
                        items.add(item)
                        itemMetadata[item] = entity to file
                    }
                }
            }
            is DocQLResult.TocItems -> {
                result.itemsByFile.forEach { (file, tocItems) ->
                    tocItems.forEach { tocItem ->
                        val segment = TextSegment(
                            text = tocItem.title,
                            metadata = mapOf(
                                "type" to "toc",
                                "name" to tocItem.title,
                                "id" to "${file}:${tocItem.title}:${tocItem.level}",
                                "filePath" to file
                            )
                        )
                        val item = SearchItem(segment)
                        items.add(item)
                        itemMetadata[item] = tocItem to file
                    }
                }
            }
            is DocQLResult.Chunks -> {
                result.itemsByFile.forEach { (file, chunks) ->
                    chunks.forEach { chunk ->
                        val segment = TextSegment(
                            text = chunk.content,
                            metadata = mapOf(
                                "type" to "chunk",
                                "id" to "${file}:${chunk.content.hashCode()}",
                                "filePath" to file
                            )
                        )
                        val item = SearchItem(segment)
                        items.add(item)
                        itemMetadata[item] = chunk to file
                    }
                }
            }
            else -> {} // Ignore other types
        }
    }

    private suspend fun executeFallbackSearch(
        keyword: String,
        documentPath: String?,
        maxResults: Int,
        reranker: DocumentReranker
    ): ToolResult {
        logger.info { "Smart search yielded no results, trying broader content search" }
        val fallbackQuery = "$.content.chunks()"
        val allChunksResult = if (documentPath != null) {
            DocumentRegistry.queryDocument(documentPath, fallbackQuery)
        } else {
            DocumentRegistry.queryDocuments(fallbackQuery)
        }
        
        if (allChunksResult is DocQLResult.Chunks) {
            // Pre-filter chunks that contain the keyword
            val relevantChunks = allChunksResult.itemsByFile.flatMap { (file, chunks) ->
                chunks.filter { it.content.contains(keyword, ignoreCase = true) }
                    .map { chunk ->
                        TextSegment(
                            text = chunk.content,
                            metadata = mapOf(
                                "type" to "chunk",
                                "id" to "${file}:${chunk.hashCode()}",
                                "filePath" to file
                            )
                        ) to chunk
                    }
            }
            
            if (relevantChunks.isNotEmpty()) {
                val segments = relevantChunks.map { it.first }
                val rerankResult = reranker.rerankSegments(segments, keyword)
                
                val scoredResults = rerankResult.items.mapIndexed { index, scoredItem: ScoredItem<TextSegment> ->
                    val chunk = relevantChunks.find { it.first == scoredItem.item }?.second
                    ScoredResult(
                        item = chunk ?: scoredItem.item,
                        score = scoredItem.score,
                        uniqueId = scoredItem.item.id ?: index.toString(),
                        preview = scoredItem.item.text.take(100).replace("\n", " "),
                        filePath = scoredItem.item.filePath
                    )
                }
                return ToolResult.Success(formatSmartResult(scoredResults, keyword, rerankResult.truncated, rerankResult.totalCount))
            }
        }

        return ToolResult.Success("No results found for '$keyword'.\n\n${buildQuerySuggestion(keyword, DocumentRegistry.getRegisteredPaths())}")
    }

    private data class ScoredResult(
        val item: Any,
        val score: Double,
        val uniqueId: String,
        val preview: String,
        val filePath: String? = null
    )

    private fun formatSmartResult(
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
                    val scoreInfo = if (result.score > 0) " (score: %.2f)".format(result.score) else ""
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

    private suspend fun querySingleDocument(documentPath: String?, query: String): ToolResult {
        // If documentPath is null or "null", delegate to global search
        if (documentPath == null || documentPath == "null") {
            return queryAllDocuments(query)
        }
        
        val result = DocumentRegistry.queryDocument(documentPath, query)
        
        // If document not found, fall back to global search
        if (result == null) {
            logger.info { "Document '$documentPath' not found, falling back to global search" }
            return queryAllDocuments(query)
        }
        
        return ToolResult.Success(formatDocQLResult(result, documentPath, params.maxResults ?: 20))
    }

    private suspend fun queryAllDocuments(query: String): ToolResult {
        // Use the new multi-file query API
        val result = DocumentRegistry.queryDocuments(query)
        
        return if (!isEmptyResult(result)) {
            ToolResult.Success(formatDocQLResult(result, null, params.maxResults ?: 20))
        } else {
            // Provide helpful suggestions when no results found
            val availablePaths = DocumentRegistry.getAllAvailablePaths()
            if (availablePaths.isEmpty()) {
                return ToolResult.Error(
                    "No documents available. Please register or index documents first.", 
                    ToolErrorType.FILE_NOT_FOUND.code
                )
            }
            val suggestion = buildQuerySuggestion(query, availablePaths)
            ToolResult.Success("No results found for query: $query\n\n$suggestion")
        }
    }

    private fun buildQuerySuggestion(query: String, registeredPaths: List<String>): String {
        val suggestions = mutableListOf<String>()
        
        suggestions.add("💡 **Suggestions to find the information:**")
        
        // Suggest checking TOC if not already a TOC query
        if (!query.contains("toc")) {
            suggestions.add("1. Try `$.toc[*]` to see all available sections in the documents")
        }
        
        // Suggest broader search if query looks specific
        if (query.contains("heading") || query.contains("h1") || query.contains("h2")) {
            suggestions.add("2. Try a broader heading search with fewer keywords")
            suggestions.add("3. Try `$.content.chunks()` to get all content and search manually")
        } else if (!query.contains("chunks")) {
            suggestions.add("2. Try `$.content.chunks()` to retrieve all document content")
        }
        
        // List available documents
        if (registeredPaths.isNotEmpty()) {
            suggestions.add("\n📚 **Available documents:**")
            registeredPaths.forEach { path ->
                suggestions.add("   - $path")
            }
        }
        
        return suggestions.joinToString("\n")
    }

    private fun formatDocQLResult(
        result: DocQLResult, 
        documentPath: String?,
        maxResults: Int = 20
    ): String {
        return when (result) {
            is DocQLResult.TocItems -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults
                    
                    appendLine("Found $totalItems TOC items across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                    }
                    appendLine()
                    
                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break
                        
                        appendLine("## 📄 $filePath")
                        for (item in items) {
                            if (count >= maxResults) break
                            appendLine("  ${"  ".repeat(item.level - 1)}${item.level}. ${item.title}")
                            count++
                        }
                        appendLine()
                    }
                    
                    if (truncated) {
                        appendLine("💡 Tip: Query specific directories to get more focused results:")
                        appendLine("   \$.toc[?(@.title contains \"keyword\")]")
                    }
                }
            }
            is DocQLResult.Entities -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults
                    
                    appendLine("Found $totalItems entities across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                    }
                    appendLine()
                    
                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break
                        
                        appendLine("## 📄 $filePath")
                        for (entity in items) {
                            if (count >= maxResults) break
                            when (entity) {
                                is Entity.ClassEntity -> {
                                    val pkg = if (!entity.packageName.isNullOrEmpty()) " (${entity.packageName})" else ""
                                    appendLine("  📘 class ${entity.name}$pkg")
                                    if (entity.location.line != null) {
                                        appendLine("     └─ Line ${entity.location.line}")
                                    }
                                }
                                is Entity.FunctionEntity -> {
                                    val sig = entity.signature ?: entity.name
                                    appendLine("  ⚡ $sig")
                                    if (entity.location.line != null) {
                                        appendLine("     └─ Line ${entity.location.line}")
                                    }
                                }
                                is Entity.Term -> {
                                    appendLine("  📝 ${entity.name}: ${entity.definition ?: ""}")
                                }
                                is Entity.API -> {
                                    appendLine("  🔌 ${entity.name}: ${entity.signature ?: ""}")
                                }
                            }
                            count++
                        }
                        appendLine()
                    }
                    
                    if (truncated) {
                        appendLine("💡 Tip: Use $.code.class(\"ClassName\") or $.code.function(\"functionName\") to get full source code")
                    }
                }
            }
            is DocQLResult.Chunks -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults
                    
                    appendLine("Found $totalItems content chunks across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                        appendLine("💡 Tip: Narrow down your search to specific files or directories")
                        appendLine("   Example: Query documents in a specific directory only")
                    }
                    appendLine()
                    
                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break
                        
                        // Filter out empty or whitespace-only chunks
                        val nonEmptyItems = items.filter { it.content.trim().isNotEmpty() }
                        if (nonEmptyItems.isEmpty()) continue
                        
                        appendLine("## 📄 $filePath")
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
                        appendLine("## 📄 $filePath")
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
                        appendLine("## 📄 $filePath")
                        appendLine("  ${items.size} table(s)")
                    }
                }
            }
            is DocQLResult.Files -> {
                buildString {
                    appendLine("Found ${result.items.size} files:")
                    appendLine()
                    result.items.forEach { file ->
                        appendLine("📄 ${file.path}")
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
                        appendLine("💡 Too many results! Consider filtering by directory:")
                        appendLine("   \$.files[?(@.path contains \"your-directory\")]")
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

    private fun isEmptyResult(result: DocQLResult): Boolean {
        return when (result) {
            is DocQLResult.Empty -> true
            is DocQLResult.TocItems -> result.totalCount == 0
            is DocQLResult.Entities -> result.totalCount == 0
            is DocQLResult.Chunks -> result.totalCount == 0
            is DocQLResult.CodeBlocks -> result.totalCount == 0
            is DocQLResult.Tables -> result.totalCount == 0
            is DocQLResult.Files -> result.items.isEmpty()
            is DocQLResult.Error -> true
        }
    }
}

class DocQLTool : BaseExecutableTool<DocQLParams, ToolResult>() {
    override val name: String = "DocQL"
    override val description: String = "Executes a DocQL query against available documents (both in-memory and indexed)."
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "DocQL Query",
        tuiEmoji = "📄",
        composeIcon = "description",
        category = ToolCategory.Utility,
        schema = DocQLSchema
    )

    override fun getParameterClass(): String = DocQLParams::class.simpleName ?: "DocQLParams"

    /**
     * Override createInvocation to handle Map<String, Any> parameters from ToolOrchestrator
     */
    override fun createInvocation(params: DocQLParams): ToolInvocation<DocQLParams, ToolResult> {
        // Handle both direct DocQLParams and Map<String, Any> from ToolOrchestrator
        val actualParams = when (params) {
            is DocQLParams -> params
            else -> {
                // If params is actually a Map (cast from Any), convert it to DocQLParams
                @Suppress("UNCHECKED_CAST")
                val paramsMap = params as? Map<String, Any> ?: throw ToolException(
                    "Invalid parameters type: expected DocQLParams or Map<String, Any>, got ${params::class.simpleName}",
                    ToolErrorType.INVALID_PARAMETERS
                )
                convertMapToDocQLParams(paramsMap)
            }
        }
        return createToolInvocation(actualParams)
    }

    override fun createToolInvocation(params: DocQLParams): ToolInvocation<DocQLParams, ToolResult> {
        return DocQLInvocation(params, this)
    }
    
    private fun convertMapToDocQLParams(map: Map<String, Any>): DocQLParams {
        val query = map["query"] as? String
            ?: throw ToolException("Missing required parameter 'query'", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        
        val documentPath = map["documentPath"] as? String
        val maxResults = when (val maxRes = map["maxResults"]) {
            is Int -> maxRes
            is Long -> maxRes.toInt()
            is Double -> maxRes.toInt()
            is String -> maxRes.toIntOrNull()
            null -> null
            else -> null
        }
        
        return DocQLParams(
            query = query,
            documentPath = documentPath,
            maxResults = maxResults ?: 20
        )
    }
}
