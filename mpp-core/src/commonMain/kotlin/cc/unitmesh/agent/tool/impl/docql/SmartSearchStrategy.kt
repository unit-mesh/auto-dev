package cc.unitmesh.agent.tool.impl.docql

import cc.unitmesh.agent.scoring.DocumentReranker
import cc.unitmesh.agent.scoring.DocumentRerankerConfig
import cc.unitmesh.agent.scoring.ExpandedKeywords
import cc.unitmesh.agent.scoring.KeywordExpander
import cc.unitmesh.agent.tool.ToolResult

interface SmartSearchStrategy {
    suspend fun execute(context: SmartSearchContext): ToolResult
}

data class SmartSearchContext(
    val keyword: String,
    val secondaryKeyword: String?,
    val documentPath: String?,
    val maxResults: Int,
    val reranker: DocumentReranker,
    val rerankerConfig: DocumentRerankerConfig,
    val keywordExpander: KeywordExpander,
    val expandedKeywords: ExpandedKeywords,
    val level1Results: SearchLevelResult,
    val searchExecutor: suspend (List<String>, String) -> SearchLevelResult,
    val resultBuilder: suspend (SearchLevelResult, String, KeywordExpansionStats) -> ToolResult,
    val fallbackExecutor: suspend (String, String?, Int, DocumentReranker, DocumentRerankerConfig) -> ToolResult
)

/**
 * Result of a search at a specific keyword level.
 */
data class SearchLevelResult(
    val items: List<SearchItem>,
    val totalCount: Int,
    val truncated: Boolean,
    val metadata: MutableMap<SearchItem, Pair<Any, String?>>,
    val docsSearched: Int,
    val activeChannels: List<String>
)

class KeepStrategy : SmartSearchStrategy {
    override suspend fun execute(context: SmartSearchContext): ToolResult {
        return context.resultBuilder(
            context.level1Results,
            context.keyword,
            KeywordExpansionStats(
                originalQuery = context.keyword,
                primaryKeywords = context.expandedKeywords.primary,
                secondaryKeywords = context.expandedKeywords.secondary,
                levelUsed = 1,
                strategyUsed = "KEEP",
                level1ResultCount = context.level1Results.totalCount,
                level2ResultCount = 0
            )
        )
    }
}

class FilterStrategy : SmartSearchStrategy {
    override suspend fun execute(context: SmartSearchContext): ToolResult {
        val filterKeywords = if (context.secondaryKeyword != null) {
            listOf(context.secondaryKeyword) + context.expandedKeywords.secondary.take(3)
        } else {
            context.expandedKeywords.secondary.take(4)
        }

        if (filterKeywords.isNotEmpty()) {
            val filteredResults = filterResultsByKeywords(
                context.level1Results.items,
                filterKeywords
            )

            // If filtering produces reasonable results, use them
            if (filteredResults.size >= context.keywordExpander.config.minResultsThreshold) {
                val reranked = context.reranker.rerank(
                    rankedLists = mapOf("filtered" to filteredResults),
                    query = "${context.keyword} ${filterKeywords.joinToString(" ")}",
                    segmentExtractor = { it.segment }
                )

                return context.resultBuilder(
                    SearchLevelResult(
                        items = filteredResults,
                        totalCount = reranked.totalCount,
                        truncated = reranked.truncated,
                        metadata = context.level1Results.metadata,
                        docsSearched = context.level1Results.docsSearched,
                        activeChannels = context.level1Results.activeChannels
                    ),
                    context.keyword,
                    KeywordExpansionStats(
                        originalQuery = context.keyword,
                        primaryKeywords = context.expandedKeywords.primary,
                        secondaryKeywords = filterKeywords,
                        levelUsed = 2,
                        strategyUsed = "FILTER",
                        level1ResultCount = context.level1Results.totalCount,
                        level2ResultCount = filteredResults.size
                    )
                )
            }
        }

        // Filtering didn't help much, fall back to Level 1 with truncation
        return context.resultBuilder(
            context.level1Results,
            context.keyword,
            KeywordExpansionStats(
                originalQuery = context.keyword,
                primaryKeywords = context.expandedKeywords.primary,
                secondaryKeywords = context.expandedKeywords.secondary,
                levelUsed = 1,
                strategyUsed = "KEEP_TRUNCATED",
                level1ResultCount = context.level1Results.totalCount,
                level2ResultCount = 0
            )
        )
    }

    private fun filterResultsByKeywords(
        items: List<SearchItem>,
        filterKeywords: List<String>
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
}

class ExpandStrategy : SmartSearchStrategy {
    override suspend fun execute(context: SmartSearchContext): ToolResult {
        // Too few results, expand to Level 2
        val level2Results = context.searchExecutor(
            context.expandedKeywords.upToLevel(2),
            context.keyword
        )

        // Check if Level 2 is enough
        if (level2Results.totalCount >= context.keywordExpander.config.minResultsThreshold) {
            return context.resultBuilder(
                level2Results,
                context.keyword,
                KeywordExpansionStats(
                    originalQuery = context.keyword,
                    primaryKeywords = context.expandedKeywords.primary,
                    secondaryKeywords = context.expandedKeywords.secondary,
                    levelUsed = 2,
                    strategyUsed = "EXPAND",
                    level1ResultCount = context.level1Results.totalCount,
                    level2ResultCount = level2Results.totalCount
                )
            )
        }

        // Still too few, expand to Level 3
        val level3Results = context.searchExecutor(
            context.expandedKeywords.upToLevel(3),
            context.keyword
        )

        if (level3Results.totalCount > 0) {
            return context.resultBuilder(
                level3Results,
                context.keyword,
                KeywordExpansionStats(
                    originalQuery = context.keyword,
                    primaryKeywords = context.expandedKeywords.primary,
                    secondaryKeywords = context.expandedKeywords.secondary + context.expandedKeywords.tertiary,
                    levelUsed = 3,
                    strategyUsed = "EXPAND_L3",
                    level1ResultCount = context.level1Results.totalCount,
                    level2ResultCount = level2Results.totalCount
                )
            )
        }

        return context.fallbackExecutor(
            context.keyword,
            context.documentPath,
            context.maxResults,
            context.reranker,
            context.rerankerConfig
        )
    }
}
