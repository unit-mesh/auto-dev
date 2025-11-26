package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.CompositeScorer
import cc.unitmesh.agent.scoring.DocumentMetadataItem
import cc.unitmesh.agent.scoring.LLMMetadataReranker
import cc.unitmesh.agent.scoring.LLMMetadataRerankerConfig
import cc.unitmesh.agent.scoring.RerankerType
import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.devins.document.DocumentRegistry
import cc.unitmesh.llm.KoogLLMService
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Handles reranking of DocQL search results using different strategies.
 * 
 * Supports:
 * - Heuristic-based scoring (fast, no AI costs)
 * - LLM metadata reranking (intelligent, uses AI model)
 * - Hybrid approach (combines both)
 */
class DocQLResultReranker(
    private val llmService: KoogLLMService?
) {
    /**
     * Rerank search items according to the specified strategy.
     * 
     * @param items The search items to rerank
     * @param metadata Metadata associated with each search item
     * @param query The original search query
     * @param rerankerType The reranking strategy to use
     * @param maxResults Maximum number of results to return
     * @return Reranking result with scored items and statistics
     */
    suspend fun rerank(
        items: List<SearchItem>,
        metadata: Map<SearchItem, Pair<Any, String?>>,
        query: String,
        rerankerType: RerankerType,
        maxResults: Int
    ): RerankResult {
        if (items.isEmpty()) {
            return RerankResult(
                scoredResults = emptyList(),
                llmRerankerStats = null,
                actualSearchType = DocQLSearchStats.SearchType.SMART_SEARCH
            )
        }

        // Check if LLM reranking should be applied
        val shouldUseLLMRerank = rerankerType in listOf(RerankerType.LLM_METADATA, RerankerType.HYBRID)
        
        return if (shouldUseLLMRerank && llmService != null) {
            performLLMRerank(items, metadata, query, maxResults)
        } else {
            performHeuristicRerank(items, metadata, query)
        }
    }

    /**
     * Perform LLM-based metadata reranking.
     */
    private suspend fun performLLMRerank(
        items: List<SearchItem>,
        metadata: Map<SearchItem, Pair<Any, String?>>,
        query: String,
        maxResults: Int
    ): RerankResult {
        val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        
        return try {
            val llmReranker = LLMMetadataReranker(
                llmService = llmService!!,
                config = LLMMetadataRerankerConfig(
                    maxItemsForLLM = 20,
                    maxResults = maxResults,
                    includePreview = true
                )
            )
            
            // Create metadata items for LLM reranking
            val metadataItems = items.mapIndexed { index, item ->
                val filePath = item.segment.filePath ?: ""
                val heuristicScore = CompositeScorer().score(item.segment, query)
                
                DocumentMetadataItem(
                    id = item.segment.id ?: "item_$index",
                    filePath = filePath,
                    fileName = filePath.substringAfterLast('/'),
                    extension = filePath.substringAfterLast('.', ""),
                    directory = filePath.substringBeforeLast('/', ""),
                    contentType = item.segment.type,
                    name = item.segment.name.ifEmpty { filePath.substringAfterLast('/') },
                    preview = item.segment.text.take(200),
                    h1Heading = metadata[item]?.second?.let { getH1FromPath(it) },
                    heuristicScore = heuristicScore
                )
            }
            
            val llmResult = llmReranker.rerank(metadataItems, query)
            val endTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            
            // Map LLM results back to scored results
            val idToItem = items.associateBy { it.segment.id ?: it.hashCode().toString() }
            val scoredResults = llmResult.rankedIds.mapNotNull { id ->
                val item = idToItem[id] ?: return@mapNotNull null
                val (originalItem, filePath) = metadata[item] ?: (item to null)
                ScoredResult(
                    item = originalItem,
                    score = llmResult.scores[id] ?: 0.0,
                    uniqueId = id,
                    preview = item.segment.text.take(100).replace("\n", " "),
                    filePath = filePath
                )
            }
            
            val llmRerankerStats = LLMRerankerStats(
                success = llmResult.success,
                itemsProcessed = metadataItems.size,
                itemsReranked = scoredResults.size,
                latencyMs = endTime - startTime,
                explanation = llmResult.explanation,
                error = llmResult.error,
                usedFallback = !llmResult.success
            )
            
            logger.info { "LLM reranking completed: ${scoredResults.size} items, ${endTime - startTime}ms" }
            
            RerankResult(
                scoredResults = scoredResults,
                llmRerankerStats = llmRerankerStats,
                actualSearchType = DocQLSearchStats.SearchType.LLM_RERANKED
            )
        } catch (e: Exception) {
            logger.warn { "LLM reranking failed, falling back to heuristic: ${e.message}" }
            
            // Fall back to heuristic scoring
            val heuristicResult = performHeuristicRerank(items, metadata, query)
            
            heuristicResult.copy(
                llmRerankerStats = LLMRerankerStats(
                    success = false,
                    itemsProcessed = items.size,
                    itemsReranked = heuristicResult.scoredResults.size,
                    error = e.message,
                    usedFallback = true
                )
            )
        }
    }

    /**
     * Perform heuristic-based scoring (no LLM required).
     */
    private fun performHeuristicRerank(
        items: List<SearchItem>,
        metadata: Map<SearchItem, Pair<Any, String?>>,
        query: String
    ): RerankResult {
        val scoredResults = items.map { item ->
            val (originalItem, filePath) = metadata[item] ?: (item to null)
            ScoredResult(
                item = originalItem,
                score = 0.0, // Score already applied during reranking
                uniqueId = item.segment.id ?: item.hashCode().toString(),
                preview = item.segment.text.take(100).replace("\n", " "),
                filePath = filePath
            )
        }
        
        return RerankResult(
            scoredResults = scoredResults,
            llmRerankerStats = null,
            actualSearchType = DocQLSearchStats.SearchType.SMART_SEARCH
        )
    }

    /**
     * Helper to get H1 heading from a file path (if document is registered).
     */
    private fun getH1FromPath(filePath: String): String? {
        return try {
            val docPair = DocumentRegistry.getDocument(filePath)
            if (docPair?.first is cc.unitmesh.devins.document.DocumentFile) {
                val docFile = docPair.first as cc.unitmesh.devins.document.DocumentFile
                docFile.toc.firstOrNull { it.level == 1 }?.title
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Wrapper for search items to enable RRF fusion.
 */
data class SearchItem(
    val segment: TextSegment
)

/**
 * Result of a single scored item.
 */
data class ScoredResult(
    val item: Any,
    val score: Double,
    val uniqueId: String,
    val preview: String,
    val filePath: String? = null
)

/**
 * Result of the reranking operation.
 */
data class RerankResult(
    val scoredResults: List<ScoredResult>,
    val llmRerankerStats: LLMRerankerStats?,
    val actualSearchType: DocQLSearchStats.SearchType
)
