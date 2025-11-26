package cc.unitmesh.agent.scoring

/**
 * Represents a scored item with its original data and computed score.
 * Used for reranking results from multiple retrieval sources.
 */
data class ScoredItem<T>(
    val item: T,
    val score: Double,
    val source: String = "unknown",
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Result of a reranking operation.
 */
data class RerankResult<T>(
    val items: List<ScoredItem<T>>,
    val totalCount: Int,
    val truncated: Boolean = false
)

/**
 * Configuration for document reranking behavior.
 */
data class DocumentRerankerConfig(
    /** Maximum number of results to return */
    val maxResults: Int = 20,
    /** Minimum score threshold (items below this are filtered) */
    val minScoreThreshold: Double = 0.0,
    /** RRF constant k */
    val rrfK: Int = 60,
    /** Weight for RRF score in final combination */
    val rrfWeight: Double = 0.4,
    /** Weight for content score in final combination */
    val contentWeight: Double = 0.6,
    /** Content score normalization factor */
    val contentScoreNormalizer: Double = 150.0
)

/**
 * Document reranker for RAG (Retrieval-Augmented Generation) scenarios.
 * 
 * Combines multiple scoring strategies to rank documents/code snippets:
 * 
 * 1. **RRF Fusion**: Combines rankings from multiple retrieval sources
 * 2. **BM25 Scoring**: Term frequency-based relevance
 * 3. **Type Prioritization**: Prefers code entities over documentation
 * 4. **Name Matching**: Rewards query matches in entity names
 * 
 * ## Architecture
 * 
 * This reranker uses composition of specialized scorers:
 * - [RRFScorer] for multi-source rank fusion
 * - [CompositeScorer] for content-based scoring
 * 
 * ## Usage
 * 
 * ```kotlin
 * val reranker = DocumentReranker()
 * 
 * // Multi-source reranking
 * val rankedLists = mapOf(
 *     "class" to listOf(item1, item2),
 *     "function" to listOf(item3, item1)
 * )
 * val result = reranker.rerank(rankedLists, "AuthService") { it.segment }
 * 
 * // Single source reranking
 * val segments = listOf(segment1, segment2, segment3)
 * val result = reranker.rerankSegments(segments, "query")
 * ```
 */
class DocumentReranker(
    private val config: DocumentRerankerConfig = DocumentRerankerConfig(),
    private val compositeScorer: CompositeScorer = CompositeScorer(),
    private val rrfScorer: RRFScorer<Any> = RRFScorer(RRFConfig(60))
) : ScoringModel {
    
    /**
     * Rerank items from multiple retrieval sources using RRF fusion + content scoring.
     * 
     * @param rankedLists Map of source name to list of items (already ranked by that source)
     * @param query The search query
     * @param segmentExtractor Function to extract TextSegment from item for scoring
     * @return Fused and reranked results
     */
    fun <T> rerank(
        rankedLists: Map<String, List<T>>,
        query: String,
        segmentExtractor: (T) -> TextSegment
    ): RerankResult<T> {
        if (rankedLists.isEmpty() || rankedLists.values.all { it.isEmpty() }) {
            return RerankResult(emptyList(), 0)
        }
        
        // Step 1: RRF fusion
        @Suppress("UNCHECKED_CAST")
        val rrfTyped = RRFScorer<T>(RRFConfig(config.rrfK))
        val rrfResults = rrfTyped.fuseNormalized(rankedLists)
        
        // Step 2: Content-based scoring
        val items = rrfResults.map { it.item }
        val segments = items.map { segmentExtractor(it) }
        val contentScores = compositeScorer.scoreAll(segments, query)
        
        // Step 3: Combine RRF and content scores
        val combinedScores = rrfResults.mapIndexed { index, rrfItem ->
            val normalizedContent = contentScores[index] / config.contentScoreNormalizer
            val combinedScore = (rrfItem.score * config.rrfWeight) + 
                               (normalizedContent * config.contentWeight)
            
            ScoredItem(
                item = rrfItem.item,
                score = combinedScore,
                source = rrfItem.sources.joinToString(","),
                metadata = mapOf(
                    "rrfScore" to rrfItem.score,
                    "contentScore" to contentScores[index],
                    "sources" to rrfItem.sources,
                    "ranks" to rrfItem.ranks
                )
            )
        }
        
        // Step 4: Filter, sort, and truncate
        val filtered = combinedScores.filter { it.score >= config.minScoreThreshold }
        val sorted = filtered.sortedByDescending { it.score }
        val truncated = sorted.size > config.maxResults
        val result = sorted.take(config.maxResults)
        
        return RerankResult(
            items = result,
            totalCount = sorted.size,
            truncated = truncated
        )
    }
    
    /**
     * Rerank a single list of segments (no RRF fusion).
     */
    fun rerankSegments(
        segments: List<TextSegment>,
        query: String
    ): RerankResult<TextSegment> {
        if (segments.isEmpty()) {
            return RerankResult(emptyList(), 0)
        }
        
        val scores = compositeScorer.scoreAll(segments, query)
        
        val scoredItems = segments.zip(scores).map { (segment, score) ->
            ScoredItem(
                item = segment,
                score = score,
                source = segment.type
            )
        }
        
        val filtered = scoredItems.filter { it.score >= config.minScoreThreshold }
        val sorted = filtered.sortedByDescending { it.score }
        val truncated = sorted.size > config.maxResults
        val result = sorted.take(config.maxResults)
        
        return RerankResult(
            items = result,
            totalCount = sorted.size,
            truncated = truncated
        )
    }
    
    // ==================== ScoringModel Implementation ====================
    
    override fun scoreAll(segments: List<TextSegment>, query: String): List<Double> {
        return compositeScorer.scoreAll(segments, query)
    }
    
    companion object {
        /**
         * Create a reranker optimized for code search.
         */
        fun forCodeSearch(maxResults: Int = 20): DocumentReranker = DocumentReranker(
            config = DocumentRerankerConfig(maxResults = maxResults),
            compositeScorer = CompositeScorer.forCodeSearch()
        )
        
        /**
         * Create a reranker optimized for documentation search.
         */
        fun forDocSearch(maxResults: Int = 20): DocumentReranker = DocumentReranker(
            config = DocumentRerankerConfig(maxResults = maxResults),
            compositeScorer = CompositeScorer.forDocSearch()
        )
    }
}

// ==================== Backwards Compatibility ====================

/**
 * Alias for backwards compatibility.
 * @deprecated Use [DocumentReranker] instead.
 */
@Deprecated("Use DocumentReranker instead", ReplaceWith("DocumentReranker"))
typealias Reranker = DocumentReranker

/**
 * Alias for backwards compatibility.
 * @deprecated Use [DocumentRerankerConfig] instead.
 */
@Deprecated("Use DocumentRerankerConfig instead", ReplaceWith("DocumentRerankerConfig"))
typealias RerankConfig = DocumentRerankerConfig

/**
 * Extension function to create a DocumentReranker with custom config.
 */
fun createReranker(
    maxResults: Int = 20,
    minScoreThreshold: Double = 0.0
): DocumentReranker = DocumentReranker(
    DocumentRerankerConfig(
        maxResults = maxResults,
        minScoreThreshold = minScoreThreshold
    )
)

