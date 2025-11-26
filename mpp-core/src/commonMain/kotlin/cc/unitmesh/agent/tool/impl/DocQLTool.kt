package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.DocumentReranker
import cc.unitmesh.agent.scoring.DocumentRerankerConfig
import cc.unitmesh.agent.scoring.ExpandedKeywords
import cc.unitmesh.agent.scoring.KeywordExpander
import cc.unitmesh.agent.scoring.KeywordExpanderConfig
import cc.unitmesh.agent.scoring.ScoredItem
import cc.unitmesh.agent.scoring.SearchStrategy
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
    val maxResults: Int? = 20,
    /**
     * Optional secondary keyword for filtering results when primary query returns too many.
     * Used in multi-level keyword expansion strategy.
     *
     * Example: query="base64", secondaryKeyword="encoding"
     *   → First searches for "base64", then filters by "encoding" if too many results
     */
    val secondaryKeyword: String? = null
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
        - **secondaryKeyword** (optional): Additional keyword for filtering when results are too many
        
        ## Multi-Level Keyword Strategy
        The tool automatically expands keywords when needed:
        - **Level 1**: Original query + phrase variations (e.g., "base64 encoding" → "base64 encoder")
        - **Level 2**: Component words (e.g., "base64", "encoding")
        - **Level 3**: Stem variants (e.g., "encode", "encoded", "encoder")
        
        If you provide a **secondaryKeyword**, it will be used to filter Level 1 results when too many.
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
        ),
        "secondaryKeyword" to string(
            description = """
                Optional secondary keyword for multi-level filtering.
                When the primary query returns too many results (>50), the secondary keyword
                is used to filter and prioritize the most relevant results.
                
                Example: query="Auth", secondaryKeyword="Service" 
                  → Finds AuthService, AuthenticationService with higher priority
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
                executeSmartSearch(
                    keyword = params.query,
                    documentPath = params.documentPath,
                    maxResults = params.maxResults ?: 20,
                    secondaryKeyword = params.secondaryKeyword
                )
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

    /**
     * Execute smart search with multi-level keyword expansion.
     *
     * Strategy:
     * 1. Start with Level 1 (primary) keywords
     * 2. If too few results → expand to Level 2 (secondary) keywords
     * 3. If too many results → filter using secondary keyword (if provided) or boost matching
     * 4. If still too few → expand to Level 3 (tertiary) keywords
     */
    private suspend fun executeSmartSearch(
        keyword: String,
        documentPath: String?,
        maxResults: Int,
        secondaryKeyword: String? = null
    ): ToolResult {
        logger.info {
            "Executing Smart Search for keyword: '$keyword'" +
                    (secondaryKeyword?.let { ", secondary: '$it'" } ?: "")
        }

        // Create reranker with appropriate config
        val rerankerConfig = DocumentRerankerConfig(
            maxResults = maxResults,
            minScoreThreshold = 5.0  // Filter out very low relevance items
        )
        val reranker = DocumentReranker(rerankerConfig)

        // Initialize keyword expander
        val expanderConfig = KeywordExpanderConfig(
            minResultsThreshold = 3,
            maxResultsThreshold = maxResults * 2,  // Scale with maxResults
            idealMinResults = 5,
            idealMaxResults = maxResults
        )
        val keywordExpander = KeywordExpander(expanderConfig)
        val expandedKeywords = keywordExpander.expandWithHint(keyword, secondaryKeyword)

        // Track keyword expansion stats
        var levelUsed = 1
        var level1ResultCount = 0
        var level2ResultCount = 0
        var strategyUsed = "KEEP"

        // === LEVEL 1: Try primary keywords first ===
        val level1Results = executeKeywordSearch(
            keywords = expandedKeywords.primary,
            documentPath = documentPath,
            reranker = reranker,
            rerankerConfig = rerankerConfig,
            queryForScoring = keyword
        )
        level1ResultCount = level1Results.totalCount

        // Determine strategy based on Level 1 results
        val strategy = keywordExpander.recommendStrategy(level1Results.totalCount, 1)

        when (strategy) {
            SearchStrategy.KEEP -> {
                // Level 1 results are ideal, use them
                strategyUsed = "KEEP"
                return buildSearchResult(
                    level1Results,
                    keyword,
                    rerankerConfig,
                    KeywordExpansionStats(
                        originalQuery = keyword,
                        primaryKeywords = expandedKeywords.primary,
                        secondaryKeywords = expandedKeywords.secondary,
                        levelUsed = levelUsed,
                        strategyUsed = strategyUsed,
                        level1ResultCount = level1ResultCount,
                        level2ResultCount = level2ResultCount
                    )
                )
            }

            SearchStrategy.FILTER -> {
                // Too many results, filter using secondary keywords
                strategyUsed = "FILTER"

                val filterKeywords = if (secondaryKeyword != null) {
                    listOf(secondaryKeyword) + expandedKeywords.secondary.take(3)
                } else {
                    expandedKeywords.secondary.take(4)
                }

                if (filterKeywords.isNotEmpty()) {
                    val filteredResults = filterResultsByKeywords(
                        level1Results.items,
                        filterKeywords,
                        level1Results.metadata
                    )
                    level2ResultCount = filteredResults.size

                    // If filtering produces reasonable results, use them
                    if (filteredResults.size >= expanderConfig.minResultsThreshold) {
                        levelUsed = 2
                        val reranked = reranker.rerank(
                            rankedLists = mapOf("filtered" to filteredResults),
                            query = "$keyword ${filterKeywords.joinToString(" ")}",
                            segmentExtractor = { it.segment }
                        )

                        return buildSearchResult(
                            SearchLevelResult(
                                items = filteredResults,
                                totalCount = reranked.totalCount,
                                truncated = reranked.truncated,
                                metadata = level1Results.metadata,
                                docsSearched = level1Results.docsSearched,
                                activeChannels = level1Results.activeChannels
                            ),
                            keyword,
                            rerankerConfig,
                            KeywordExpansionStats(
                                originalQuery = keyword,
                                primaryKeywords = expandedKeywords.primary,
                                secondaryKeywords = filterKeywords,
                                levelUsed = levelUsed,
                                strategyUsed = strategyUsed,
                                level1ResultCount = level1ResultCount,
                                level2ResultCount = level2ResultCount
                            )
                        )
                    }
                }

                // Filtering didn't help much, fall back to Level 1 with truncation
                return buildSearchResult(
                    level1Results,
                    keyword,
                    rerankerConfig,
                    KeywordExpansionStats(
                        originalQuery = keyword,
                        primaryKeywords = expandedKeywords.primary,
                        secondaryKeywords = expandedKeywords.secondary,
                        levelUsed = 1,
                        strategyUsed = "KEEP_TRUNCATED",
                        level1ResultCount = level1ResultCount,
                        level2ResultCount = level2ResultCount
                    )
                )
            }

            SearchStrategy.EXPAND -> {
                // Too few results, expand to Level 2
                strategyUsed = "EXPAND"
                levelUsed = 2

                val level2Results = executeKeywordSearch(
                    keywords = expandedKeywords.upToLevel(2),
                    documentPath = documentPath,
                    reranker = reranker,
                    rerankerConfig = rerankerConfig,
                    queryForScoring = keyword
                )
                level2ResultCount = level2Results.totalCount

                // Check if Level 2 is enough
                if (level2Results.totalCount >= expanderConfig.minResultsThreshold) {
                    return buildSearchResult(
                        level2Results,
                        keyword,
                        rerankerConfig,
                        KeywordExpansionStats(
                            originalQuery = keyword,
                            primaryKeywords = expandedKeywords.primary,
                            secondaryKeywords = expandedKeywords.secondary,
                            levelUsed = levelUsed,
                            strategyUsed = strategyUsed,
                            level1ResultCount = level1ResultCount,
                            level2ResultCount = level2ResultCount
                        )
                    )
                }

                // Still too few, expand to Level 3
                levelUsed = 3
                val level3Results = executeKeywordSearch(
                    keywords = expandedKeywords.upToLevel(3),
                    documentPath = documentPath,
                    reranker = reranker,
                    rerankerConfig = rerankerConfig,
                    queryForScoring = keyword
                )

                if (level3Results.totalCount > 0) {
                    return buildSearchResult(
                        level3Results,
                        keyword,
                        rerankerConfig,
                        KeywordExpansionStats(
                            originalQuery = keyword,
                            primaryKeywords = expandedKeywords.primary,
                            secondaryKeywords = expandedKeywords.secondary + expandedKeywords.tertiary,
                            levelUsed = levelUsed,
                            strategyUsed = "EXPAND_L3",
                            level1ResultCount = level1ResultCount,
                            level2ResultCount = level2ResultCount
                        )
                    )
                }

                return executeFallbackSearch(keyword, documentPath, maxResults, reranker, rerankerConfig)
            }
        }
    }

    /**
     * Result of a search at a specific keyword level.
     */
    private data class SearchLevelResult(
        val items: List<SearchItem>,
        val totalCount: Int,
        val truncated: Boolean,
        val metadata: MutableMap<SearchItem, Pair<Any, String?>>,
        val docsSearched: Int,
        val activeChannels: List<String>
    )

    /**
     * Execute search for a list of keywords and merge results.
     */
    private suspend fun executeKeywordSearch(
        keywords: List<String>,
        documentPath: String?,
        reranker: DocumentReranker,
        rerankerConfig: DocumentRerankerConfig,
        queryForScoring: String
    ): SearchLevelResult {
        if (keywords.isEmpty()) {
            return SearchLevelResult(
                items = emptyList(),
                totalCount = 0,
                truncated = false,
                metadata = mutableMapOf(),
                docsSearched = 0,
                activeChannels = emptyList()
            )
        }

        val allItems = mutableListOf<SearchItem>()
        val allMetadata = mutableMapOf<SearchItem, Pair<Any, String?>>()
        var totalDocsSearched = 0
        val allChannels = mutableSetOf<String>()

        // Execute search for each keyword in parallel
        val keywordResults = coroutineScope {
            keywords.map { kw ->
                async {
                    val queryChannels = mapOf(
                        "class" to "$.code.class(\"$kw\")",
                        "function" to "$.code.function(\"$kw\")",
                        "heading" to "$.content.heading(\"$kw\")",
                        "toc" to "$.toc[?(@.title contains \"$kw\")]"
                    )

                    val channelResults = queryChannels.mapNotNull { (channel, query) ->
                        try {
                            val result = if (documentPath != null) {
                                DocumentRegistry.queryDocument(documentPath, query)
                            } else {
                                DocumentRegistry.queryDocuments(query)
                            }
                            if (result != null) channel to result else null
                        } catch (e: Exception) {
                            logger.warn { "Search channel '$channel' for keyword '$kw' failed: ${e.message}" }
                            null
                        }
                    }.toMap()

                    kw to channelResults
                }
            }.awaitAll().toMap()
        }

        // Merge results from all keywords
        keywordResults.forEach { (kw, channelResults) ->
            channelResults.forEach { (channel, result: DocQLResult) ->
                allChannels.add(channel)
                val items = mutableListOf<SearchItem>()
                collectSearchItems(result, items, allMetadata)
                allItems.addAll(items)

                totalDocsSearched += when (result) {
                    is DocQLResult.Entities -> result.itemsByFile.size
                    is DocQLResult.TocItems -> result.itemsByFile.size
                    is DocQLResult.Chunks -> result.itemsByFile.size
                    else -> 0
                }
            }
        }

        // Deduplicate items by their unique ID
        val uniqueItems = allItems.distinctBy { it.segment.id ?: it.hashCode() }

        if (uniqueItems.isEmpty()) {
            return SearchLevelResult(
                items = emptyList(),
                totalCount = 0,
                truncated = false,
                metadata = allMetadata,
                docsSearched = totalDocsSearched,
                activeChannels = allChannels.toList()
            )
        }

        // Rerank the merged results
        val rankedLists = mapOf("merged" to uniqueItems)
        val rerankResult = reranker.rerank(
            rankedLists = rankedLists,
            query = queryForScoring,
            segmentExtractor = { it.segment }
        )

        val rerankedItems = rerankResult.items.map { it.item }

        return SearchLevelResult(
            items = rerankedItems,
            totalCount = rerankResult.totalCount,
            truncated = rerankResult.truncated,
            metadata = allMetadata,
            docsSearched = totalDocsSearched,
            activeChannels = allChannels.toList()
        )
    }

    /**
     * Filter search items by secondary keywords (for FILTER strategy).
     */
    private fun filterResultsByKeywords(
        items: List<SearchItem>,
        filterKeywords: List<String>,
        metadata: MutableMap<SearchItem, Pair<Any, String?>>
    ): List<SearchItem> {
        if (filterKeywords.isEmpty()) return items

        return items.filter { item ->
            val text = item.segment.text.lowercase()
            val name = item.segment.name.lowercase()

            // Item matches if any filter keyword is found in text or name
            filterKeywords.any { kw ->
                val kwLower = kw.lowercase()
                text.contains(kwLower) || name.contains(kwLower)
            }
        }
    }

    /**
     * Build the final search result with statistics.
     */
    private fun buildSearchResult(
        result: SearchLevelResult,
        keyword: String,
        rerankerConfig: DocumentRerankerConfig,
        keywordStats: KeywordExpansionStats
    ): ToolResult {
        val scoredResults = result.items.map { item ->
            val (originalItem, filePath) = result.metadata[item] ?: (item to null)
            ScoredResult(
                item = originalItem,
                score = 0.0, // Score already applied during reranking
                uniqueId = item.segment.id ?: item.hashCode().toString(),
                preview = item.segment.text.take(100).replace("\n", " "),
                filePath = filePath
            )
        }

        val stats = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.SMART_SEARCH,
            channels = result.activeChannels,
            documentsSearched = result.docsSearched,
            totalRawResults = result.totalCount,
            resultsAfterRerank = result.items.size,
            truncated = result.truncated,
            usedFallback = false,
            rerankerConfig = RerankerStats(
                rerankerType = "RRF+Composite(BM25,Type,NameMatch)",
                rrfK = rerankerConfig.rrfK,
                rrfWeight = rerankerConfig.rrfWeight,
                contentWeight = rerankerConfig.contentWeight,
                minScoreThreshold = rerankerConfig.minScoreThreshold
            ),
            scoringInfo = null,
            keywordExpansion = keywordStats
        )

        return ToolResult.Success(
            formatSmartResult(scoredResults, keyword, result.truncated, result.totalCount),
            stats.toMetadata()
        )
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
        reranker: DocumentReranker,
        rerankerConfig: DocumentRerankerConfig
    ): ToolResult {
        logger.info { "Smart search yielded no results, trying broader content search" }
        val fallbackQuery = "$.content.chunks()"
        val allChunksResult = if (documentPath != null) {
            DocumentRegistry.queryDocument(documentPath, fallbackQuery)
        } else {
            DocumentRegistry.queryDocuments(fallbackQuery)
        }

        if (allChunksResult is DocQLResult.Chunks) {
            val docsSearched = allChunksResult.itemsByFile.size

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

                // Build fallback search statistics
                val scores = scoredResults.map { it.score }
                val stats = DocQLSearchStats(
                    searchType = DocQLSearchStats.SearchType.FALLBACK_CONTENT,
                    channels = listOf("content_chunks"),
                    documentsSearched = docsSearched,
                    totalRawResults = relevantChunks.size,
                    resultsAfterRerank = rerankResult.items.size,
                    truncated = rerankResult.truncated,
                    usedFallback = true,
                    rerankerConfig = RerankerStats(
                        rerankerType = "Composite(BM25,Type,NameMatch)",
                        rrfK = rerankerConfig.rrfK,
                        rrfWeight = 0.0, // No RRF in fallback
                        contentWeight = 1.0,
                        minScoreThreshold = rerankerConfig.minScoreThreshold
                    ),
                    scoringInfo = if (scores.isNotEmpty()) ScoringStats(
                        scorerComponents = listOf("BM25", "TypeScore", "NameMatch"),
                        avgScore = scores.average(),
                        maxScore = scores.maxOrNull() ?: 0.0,
                        minScore = scores.minOrNull() ?: 0.0
                    ) else null
                )

                return ToolResult.Success(
                    formatSmartResult(scoredResults, keyword, rerankResult.truncated, rerankResult.totalCount),
                    stats.toMetadata()
                )
            }
        }

        // No results found - return stats for empty search
        val emptyStats = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.FALLBACK_CONTENT,
            channels = emptyList(),
            documentsSearched = if (allChunksResult is DocQLResult.Chunks) allChunksResult.itemsByFile.size else 0,
            totalRawResults = 0,
            resultsAfterRerank = 0,
            truncated = false,
            usedFallback = true,
            rerankerConfig = null,
            scoringInfo = null
        )

        return ToolResult.Success(
            "No results found for '$keyword'.\n\n${
                buildQuerySuggestion(
                    keyword,
                    DocumentRegistry.getRegisteredPaths()
                )
            }",
            emptyStats.toMetadata()
        )
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

        val stats = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.DIRECT_QUERY,
            channels = listOf(extractQueryType(query)),
            documentsSearched = 1,
            totalRawResults = getResultCount(result),
            resultsAfterRerank = getResultCount(result).coerceAtMost(params.maxResults ?: 20),
            truncated = getResultCount(result) > (params.maxResults ?: 20),
            usedFallback = false,
            rerankerConfig = null, // No reranker for direct queries
            scoringInfo = null
        )

        return ToolResult.Success(
            formatDocQLResult(result, documentPath, params.maxResults ?: 20),
            stats.toMetadata()
        )
    }

    private suspend fun queryAllDocuments(query: String): ToolResult {
        // Use the new multi-file query API
        val result = DocumentRegistry.queryDocuments(query)

        return if (!isEmptyResult(result)) {
            val docsSearched = when (result) {
                is DocQLResult.Entities -> result.itemsByFile.size
                is DocQLResult.TocItems -> result.itemsByFile.size
                is DocQLResult.Chunks -> result.itemsByFile.size
                is DocQLResult.CodeBlocks -> result.itemsByFile.size
                is DocQLResult.Tables -> result.itemsByFile.size
                is DocQLResult.Files -> result.items.size
                else -> 0
            }

            val stats = DocQLSearchStats(
                searchType = DocQLSearchStats.SearchType.DIRECT_QUERY,
                channels = listOf(extractQueryType(query)),
                documentsSearched = docsSearched,
                totalRawResults = getResultCount(result),
                resultsAfterRerank = getResultCount(result).coerceAtMost(params.maxResults ?: 20),
                truncated = getResultCount(result) > (params.maxResults ?: 20),
                usedFallback = false,
                rerankerConfig = null,
                scoringInfo = null
            )

            ToolResult.Success(
                formatDocQLResult(result, null, params.maxResults ?: 20),
                stats.toMetadata()
            )
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

            val emptyStats = DocQLSearchStats(
                searchType = DocQLSearchStats.SearchType.DIRECT_QUERY,
                channels = listOf(extractQueryType(query)),
                documentsSearched = 0,
                totalRawResults = 0,
                resultsAfterRerank = 0,
                truncated = false,
                usedFallback = false,
                rerankerConfig = null,
                scoringInfo = null
            )

            ToolResult.Success(
                "No results found for query: $query\n\n$suggestion",
                emptyStats.toMetadata()
            )
        }
    }

    private fun extractQueryType(query: String): String {
        return when {
            query.contains("$.code.class") -> "class"
            query.contains("$.code.function") -> "function"
            query.contains("$.code.classes") -> "classes"
            query.contains("$.code.functions") -> "functions"
            query.contains("$.content.heading") -> "heading"
            query.contains("$.content.chunks") -> "chunks"
            query.contains("$.toc") -> "toc"
            query.contains("$.files") -> "files"
            else -> "unknown"
        }
    }

    private fun getResultCount(result: DocQLResult): Int {
        return when (result) {
            is DocQLResult.Entities -> result.totalCount
            is DocQLResult.TocItems -> result.totalCount
            is DocQLResult.Chunks -> result.totalCount
            is DocQLResult.CodeBlocks -> result.totalCount
            is DocQLResult.Tables -> result.totalCount
            is DocQLResult.Files -> result.items.size
            else -> 0
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
                                    val pkg =
                                        if (!entity.packageName.isNullOrEmpty()) " (${entity.packageName})" else ""
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

    /**
     * Format a score value to 2 decimal places (multiplatform compatible)
     */
    private fun formatScore(value: Double): String {
        val rounded = kotlin.math.round(value * 100) / 100
        val str = rounded.toString()
        val dotIndex = str.indexOf('.')
        return if (dotIndex == -1) {
            "$str.00"
        } else {
            val currentDecimals = str.length - dotIndex - 1
            when {
                currentDecimals >= 2 -> str.substring(0, dotIndex + 3)
                currentDecimals == 1 -> str + "0"
                else -> str + "00"
            }
        }
    }
}

class DocQLTool : BaseExecutableTool<DocQLParams, ToolResult>() {
    override val name: String = "DocQL"
    override val description: String =
        "Executes a DocQL query against available documents (both in-memory and indexed)."
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
        val secondaryKeyword = map["secondaryKeyword"] as? String
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
            maxResults = maxResults ?: 20,
            secondaryKeyword = secondaryKeyword
        )
    }
}
