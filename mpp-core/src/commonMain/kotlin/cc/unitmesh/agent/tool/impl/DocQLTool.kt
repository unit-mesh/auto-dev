package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.DocumentReranker
import cc.unitmesh.agent.scoring.DocumentRerankerConfig
import cc.unitmesh.agent.scoring.KeywordExpander
import cc.unitmesh.agent.scoring.KeywordExpanderConfig
import cc.unitmesh.agent.scoring.RerankerType
import cc.unitmesh.agent.scoring.ScoredItem
import cc.unitmesh.agent.scoring.SearchStrategy
import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.devins.document.DocumentRegistry
import cc.unitmesh.devins.document.docql.DocQLResult
import cc.unitmesh.llm.KoogLLMService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

const val initialMaxResults = 20

@Serializable
data class DocQLParams(
    val query: String,
    val documentPath: String? = null,
    val maxResults: Int? = initialMaxResults,
    /**
     * Optional secondary keyword for filtering results when primary query returns too many.
     * Used in multi-level keyword expansion strategy.
     *
     * Example: query="base64", secondaryKeyword="encoding"
     *   → First searches for "base64", then filters by "encoding" if too many results
     */
    val secondaryKeyword: String? = null,
    /**
     * Reranker type to use for result ordering.
     *
     * Options:
     * - "heuristic" (default): Fast BM25 + type + name matching
     * - "rrf_composite": RRF fusion with composite scoring
     * - "llm_metadata": LLM-based metadata reranking (slower, more intelligent)
     * - "hybrid": Heuristic pre-filter + LLM rerank
     */
    val rerankerType: String? = null
)

object DocQLSchema : DeclarativeToolSchema(
    description = """
        Executes a DocQL query against available documents (both in-memory and indexed).
        
        ## SMART SEARCH (Recommended)
        Simply provide a keyword or phrase, and the tool will automatically:
        1. Search for Classes and Functions matching the keyword (High Priority)
        2. Search for Headings and Content matching the keyword (Medium Priority)
        3. Rerank results to show the most relevant code and documentation first.
        4. 如果用户使用的语言搜索不到结果，可以从用户的语言编码方式来搜索，比如拼音搜索、拼音编写、英语等。
        
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
                When the primary query returns too many results (>20), the secondary keyword
                is used to filter and prioritize the most relevant results.
                
                Example: query="Auth", secondaryKeyword="Service" 
                  → Finds AuthService, AuthenticationService with higher priority
            """.trimIndent(),
            required = false
        ),
        "rerankerType" to string(
            description = """
                Reranker algorithm to use for ordering results.
                
                Options:
                - "heuristic" (default): Fast BM25 + type + name matching. Best for quick searches.
                - "rrf_composite": RRF fusion with composite scoring. Better for multi-source results.
                - "llm_metadata": LLM-based intelligent reranking using document metadata.
                  Considers file paths, headings, modification time, references. Slower but smarter.
                - "hybrid": Heuristic pre-filter + LLM rerank. Balance of speed and quality.
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
    tool: DocQLTool,
    private val llmService: KoogLLMService? = null
) : BaseToolInvocation<DocQLParams, ToolResult>(params, tool) {

    /** Parsed reranker type for this invocation */
    private val rerankerType: RerankerType = DocQLTool.parseRerankerType(params.rerankerType)

    /** Reranker instance for search result ordering */
    private val resultReranker = DocQLResultReranker(llmService)

    /** Keyword search executor */
    private val keywordSearchExecutor = DocQLKeywordSearchExecutor()

    /** Direct query executor */
    private val directQueryExecutor = DocQLDirectQueryExecutor(params.maxResults ?: initialMaxResults)

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
                    maxResults = params.maxResults ?: initialMaxResults,
                    secondaryKeyword = params.secondaryKeyword
                )
            } else {
                // Standard DocQL execution
                if (params.documentPath != null) {
                    directQueryExecutor.querySingleDocument(params.documentPath, params.query)
                } else {
                    directQueryExecutor.queryAllDocuments(params.query)
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

        // === LEVEL 1: Try primary keywords first ===
        val level1Results = keywordSearchExecutor.executeKeywordSearch(
            keywords = expandedKeywords.primary,
            documentPath = documentPath,
            reranker = reranker,
            queryForScoring = keyword
        )

        val strategyType = keywordExpander.recommendStrategy(level1Results.totalCount, 1)

        val context = SmartSearchContext(
            keyword = keyword,
            secondaryKeyword = secondaryKeyword,
            documentPath = documentPath,
            maxResults = maxResults,
            reranker = reranker,
            rerankerConfig = rerankerConfig,
            keywordExpander = keywordExpander,
            expandedKeywords = expandedKeywords,
            level1Results = level1Results,
            searchExecutor = { kw, query ->
                keywordSearchExecutor.executeKeywordSearch(kw, documentPath, reranker, query)
            },
            resultBuilder = { res, kw, stats ->
                buildSearchResult(res, kw, rerankerConfig, stats)
            },
            fallbackExecutor = { kw, path, max, rr, rc ->
                executeFallbackSearch(kw, path, rr, rc)
            }
        )

        val strategy = when (strategyType) {
            SearchStrategy.KEEP -> KeepStrategy()
            SearchStrategy.FILTER -> FilterStrategy()
            SearchStrategy.EXPAND -> ExpandStrategy()
        }

        return strategy.execute(context)
    }

    /**
     * Build the final search result with statistics.
     * Optionally applies LLM-based reranking when enabled.
     */
    private suspend fun buildSearchResult(
        result: SearchLevelResult,
        keyword: String,
        rerankerConfig: DocumentRerankerConfig,
        keywordStats: KeywordExpansionStats
    ): ToolResult {
        val rerankResult = resultReranker.rerank(
            items = result.items,
            metadata = result.metadata,
            query = keyword,
            rerankerType = rerankerType,
            maxResults = params.maxResults ?: initialMaxResults
        )

        val scoredResults = rerankResult.scoredResults
        val llmRerankerStats = rerankResult.llmRerankerStats
        val actualSearchType = rerankResult.actualSearchType

        val rerankerTypeName = when (rerankerType) {
            RerankerType.LLM_METADATA -> "LLM_Metadata"
            RerankerType.HYBRID -> "Hybrid(RRF+LLM)"
            RerankerType.HEURISTIC -> "Composite(BM25,Type,NameMatch)"
            RerankerType.RRF_COMPOSITE -> "RRF+Composite(BM25,Type,NameMatch)"
        }

        val stats = DocQLSearchStats(
            searchType = actualSearchType,
            channels = result.activeChannels,
            documentsSearched = result.docsSearched,
            totalRawResults = result.totalCount,
            resultsAfterRerank = scoredResults.size,
            truncated = result.truncated,
            usedFallback = false,
            rerankerConfig = RerankerStats(
                rerankerType = rerankerTypeName,
                rrfK = rerankerConfig.rrfK,
                rrfWeight = rerankerConfig.rrfWeight,
                contentWeight = rerankerConfig.contentWeight,
                minScoreThreshold = rerankerConfig.minScoreThreshold
            ),
            scoringInfo = null,
            keywordExpansion = keywordStats,
            llmRerankerInfo = llmRerankerStats
        )

        return ToolResult.Success(
            DocQLResultFormatter.formatSmartResult(scoredResults, keyword, result.truncated, result.totalCount),
            stats.toMetadata()
        )
    }

    private suspend fun executeFallbackSearch(
        keyword: String,
        documentPath: String?,
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
                        preview = scoredItem.item.text.take(DEFAULT_CHAR_LENGTH).replace("\n", " "),
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
                    DocQLResultFormatter.formatSmartResult(scoredResults, keyword, rerankResult.truncated, rerankResult.totalCount),
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
            "No results found for '$keyword'.\n\n${DocQLResultFormatter.buildQuerySuggestion(keyword)}",
            emptyStats.toMetadata()
        )
    }
}

class DocQLTool(
    private val llmService: KoogLLMService? = null
) : BaseExecutableTool<DocQLParams, ToolResult>() {
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

    override fun createInvocation(params: DocQLParams): ToolInvocation<DocQLParams, ToolResult> {
        return createToolInvocation(params)
    }

    override fun createToolInvocation(params: DocQLParams): ToolInvocation<DocQLParams, ToolResult> {
        return DocQLInvocation(params, this, llmService)
    }

    private fun convertMapToDocQLParams(map: Map<String, Any>): DocQLParams {
        val query = map["query"] as? String
            ?: throw ToolException("Missing required parameter 'query'", ToolErrorType.MISSING_REQUIRED_PARAMETER)

        val documentPath = map["documentPath"] as? String
        val secondaryKeyword = map["secondaryKeyword"] as? String
        val rerankerType = map["rerankerType"] as? String
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
            maxResults = maxResults ?: initialMaxResults,
            secondaryKeyword = secondaryKeyword,
            rerankerType = rerankerType
        )
    }

    companion object {
        /**
         * Parse reranker type from string.
         */
        fun parseRerankerType(typeStr: String?): RerankerType {
            return when (typeStr?.lowercase()) {
                "heuristic" -> RerankerType.HEURISTIC
                "rrf_composite", "rrf" -> RerankerType.RRF_COMPOSITE
                "llm_metadata", "llm" -> RerankerType.LLM_METADATA
                "hybrid" -> RerankerType.HYBRID
                else -> RerankerType.HEURISTIC  // Default: fast, no AI costs
            }
        }
    }
}
