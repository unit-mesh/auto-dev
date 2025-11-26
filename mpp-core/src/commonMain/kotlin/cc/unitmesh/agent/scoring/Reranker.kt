package cc.unitmesh.agent.scoring

import kotlin.math.ln

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
 * Configuration for reranking behavior.
 */
data class RerankConfig(
    /** Maximum number of results to return */
    val maxResults: Int = 20,
    /** RRF constant k (default 60, commonly used value) */
    val rrfK: Int = 60,
    /** Minimum score threshold (items below this are filtered) */
    val minScoreThreshold: Double = 0.0,
    /** Weight for BM25 scoring component */
    val bm25Weight: Double = 0.4,
    /** Weight for type-based scoring component */
    val typeWeight: Double = 0.3,
    /** Weight for name matching scoring component */
    val nameMatchWeight: Double = 0.3,
    /** BM25 parameters */
    val bm25K1: Double = 1.2,
    val bm25B: Double = 0.75,
    /** Average document length for BM25 (estimated) */
    val avgDocLength: Double = 100.0
)

/**
 * A reranker that combines multiple scoring strategies for document/code retrieval.
 * 
 * Implements:
 * - **BM25 scoring**: Classic information retrieval algorithm for term frequency
 * - **RRF (Reciprocal Rank Fusion)**: Combines rankings from multiple sources
 * - **Type-based boosting**: Prioritizes code entities (classes, functions) over docs
 * - **Name matching**: Exact and partial match bonuses
 * 
 * This is designed for RAG scenarios where we need to:
 * 1. Retrieve from multiple sources (code, docs, TOC, etc.)
 * 2. Fuse results intelligently
 * 3. Return only the most relevant items within token budget
 */
class Reranker(
    private val config: RerankConfig = RerankConfig()
) : ScoringModel {

    /**
     * Rerank items from multiple retrieval sources using RRF fusion.
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

        // Step 1: Calculate RRF scores for each item
        val rrfScores = mutableMapOf<T, Double>()
        val itemSources = mutableMapOf<T, MutableList<String>>()
        
        rankedLists.forEach { (source, items) ->
            items.forEachIndexed { rank, item ->
                // RRF formula: 1 / (k + rank)
                val rrfScore = 1.0 / (config.rrfK + rank + 1)
                rrfScores[item] = (rrfScores[item] ?: 0.0) + rrfScore
                itemSources.getOrPut(item) { mutableListOf() }.add(source)
            }
        }

        // Step 2: Calculate content-based scores and combine
        val allItems = rrfScores.keys.toList()
        val segments = allItems.map { segmentExtractor(it) }
        val contentScores = scoreAll(segments, query)

        // Step 3: Combine RRF and content scores
        val combinedScores = allItems.mapIndexed { index, item ->
            val rrfScore = rrfScores[item] ?: 0.0
            val contentScore = contentScores[index]
            
            // Normalize RRF score (max possible is number of sources)
            val normalizedRrf = rrfScore / rankedLists.size
            
            // Combined score: weighted average
            val combinedScore = (normalizedRrf * 0.4) + (contentScore / 150.0 * 0.6)
            
            ScoredItem(
                item = item,
                score = combinedScore,
                source = itemSources[item]?.joinToString(",") ?: "unknown",
                metadata = mapOf(
                    "rrfScore" to rrfScore,
                    "contentScore" to contentScore,
                    "sources" to (itemSources[item] ?: emptyList<String>())
                )
            )
        }

        // Step 4: Sort, filter, and truncate
        val sorted = combinedScores
            .filter { it.score >= config.minScoreThreshold }
            .sortedByDescending { it.score }
        
        val truncated = sorted.size > config.maxResults
        val result = sorted.take(config.maxResults)

        return RerankResult(
            items = result,
            totalCount = sorted.size,
            truncated = truncated
        )
    }

    /**
     * Simple rerank for a single list of segments.
     */
    fun rerankSegments(
        segments: List<TextSegment>,
        query: String
    ): RerankResult<TextSegment> {
        val scores = scoreAll(segments, query)
        
        val scoredItems = segments.zip(scores).map { (segment, score) ->
            ScoredItem(
                item = segment,
                score = score,
                source = segment.metadata["type"] as? String ?: "unknown"
            )
        }
        
        val sorted = scoredItems
            .filter { it.score >= config.minScoreThreshold }
            .sortedByDescending { it.score }
        
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
        if (segments.isEmpty()) return emptyList()
        
        // Precompute query terms for BM25
        val queryTerms = tokenize(query)
        
        // Calculate average document length for BM25
        val avgDocLen = segments.map { it.text.length.toDouble() }.average()
            .takeIf { it > 0 } ?: config.avgDocLength
        
        // Build document frequency map for IDF
        val docFreq = buildDocumentFrequency(segments, queryTerms)
        val numDocs = segments.size
        
        return segments.map { segment ->
            calculateCombinedScore(segment, query, queryTerms, avgDocLen, docFreq, numDocs)
        }
    }

    private fun calculateCombinedScore(
        segment: TextSegment,
        query: String,
        queryTerms: List<String>,
        avgDocLen: Double,
        docFreq: Map<String, Int>,
        numDocs: Int
    ): Double {
        // 1. BM25 Score
        val bm25Score = calculateBM25Score(segment.text, queryTerms, avgDocLen, docFreq, numDocs)
        
        // 2. Type Score (code entities > docs)
        val typeScore = calculateTypeScore(segment)
        
        // 3. Name Match Score
        val nameScore = calculateNameMatchScore(segment, query)
        
        // Combine with configurable weights
        return (bm25Score * config.bm25Weight * 100) +  // Scale BM25 to similar range
               (typeScore * config.typeWeight) +
               (nameScore * config.nameMatchWeight)
    }

    // ==================== BM25 Implementation ====================

    /**
     * Calculate BM25 score for a document against query terms.
     * 
     * BM25 formula: sum over query terms of:
     *   IDF(qi) * (f(qi, D) * (k1 + 1)) / (f(qi, D) + k1 * (1 - b + b * |D|/avgdl))
     * 
     * Where:
     * - f(qi, D) = term frequency of qi in document D
     * - |D| = document length
     * - avgdl = average document length
     * - k1, b = tuning parameters (typically k1=1.2, b=0.75)
     */
    private fun calculateBM25Score(
        text: String,
        queryTerms: List<String>,
        avgDocLen: Double,
        docFreq: Map<String, Int>,
        numDocs: Int
    ): Double {
        val docTerms = tokenize(text)
        val termFreq = docTerms.groupingBy { it }.eachCount()
        val docLen = docTerms.size.toDouble()
        
        var score = 0.0
        
        for (term in queryTerms) {
            val tf = termFreq[term] ?: 0
            if (tf == 0) continue
            
            // IDF with smoothing
            val df = docFreq[term] ?: 0
            val idf = ln((numDocs - df + 0.5) / (df + 0.5) + 1.0)
            
            // BM25 term score
            val numerator = tf * (config.bm25K1 + 1)
            val denominator = tf + config.bm25K1 * (1 - config.bm25B + config.bm25B * docLen / avgDocLen)
            
            score += idf * (numerator / denominator)
        }
        
        return score
    }

    private fun buildDocumentFrequency(
        segments: List<TextSegment>,
        queryTerms: List<String>
    ): Map<String, Int> {
        val docFreq = mutableMapOf<String, Int>()
        
        for (segment in segments) {
            val docTerms = tokenize(segment.text).toSet()
            for (term in queryTerms) {
                if (term in docTerms) {
                    docFreq[term] = (docFreq[term] ?: 0) + 1
                }
            }
        }
        
        return docFreq
    }

    /**
     * Tokenize text into terms for BM25.
     * Handles camelCase, snake_case, and common separators.
     */
    private fun tokenize(text: String): List<String> {
        // Split on common separators and camelCase boundaries
        return text
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")  // camelCase
            .replace(Regex("[_\\-./]"), " ")            // separators
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.length > 1 }  // Filter single chars
    }

    // ==================== Type-based Scoring ====================

    private fun calculateTypeScore(segment: TextSegment): Double {
        val type = segment.metadata["type"] as? String ?: "unknown"
        
        return when (type) {
            "class" -> 100.0
            "function" -> 90.0
            "interface" -> 85.0
            "method" -> 80.0
            "heading" -> 60.0
            "toc" -> 40.0
            "chunk" -> 20.0
            else -> 10.0
        }
    }

    // ==================== Name Match Scoring ====================

    private fun calculateNameMatchScore(segment: TextSegment, query: String): Double {
        val name = segment.metadata["name"] as? String ?: ""
        val content = segment.text
        
        var score = 0.0
        
        // Exact name match (highest priority)
        if (name.equals(query, ignoreCase = true)) {
            score += 50.0
        } 
        // Name starts with query
        else if (name.startsWith(query, ignoreCase = true)) {
            score += 35.0
        }
        // Name contains query
        else if (name.contains(query, ignoreCase = true)) {
            score += 20.0
        }
        
        // Content contains query (lower priority)
        if (content.contains(query, ignoreCase = true)) {
            // Count occurrences for frequency bonus
            val occurrences = content.lowercase().split(query.lowercase()).size - 1
            score += minOf(occurrences * 2.0, 10.0)  // Cap at 10
        }
        
        // Camel case match bonus (e.g., "AS" matches "AuthService")
        if (query.all { it.isUpperCase() } && query.length >= 2) {
            val initials = name.filter { it.isUpperCase() }
            if (initials.contains(query)) {
                score += 15.0
            }
        }
        
        return score
    }
}

/**
 * Extension function to create a Reranker with custom config.
 */
fun createReranker(
    maxResults: Int = 20,
    minScoreThreshold: Double = 0.0
): Reranker = Reranker(
    RerankConfig(
        maxResults = maxResults,
        minScoreThreshold = minScoreThreshold
    )
)

