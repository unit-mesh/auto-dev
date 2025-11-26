package cc.unitmesh.agent.scoring

/**
 * RRF (Reciprocal Rank Fusion) configuration.
 * 
 * @param k The constant k in RRF formula (default 60, commonly used value)
 */
data class RRFConfig(
    val k: Int = 60
)

/**
 * Result item with RRF score.
 */
data class RRFItem<T>(
    val item: T,
    val score: Double,
    val sources: List<String>,
    val ranks: Map<String, Int>
)

/**
 * Reciprocal Rank Fusion (RRF) algorithm implementation.
 * 
 * RRF is a simple but effective method for combining rankings from multiple
 * retrieval systems. It's particularly useful when you have multiple search
 * channels (e.g., keyword search, semantic search, code structure search).
 * 
 * ## Formula
 * 
 * For each item, the RRF score is:
 * ```
 * RRF(d) = Σ 1 / (k + rank(d))
 * ```
 * 
 * Where:
 * - k = constant (typically 60)
 * - rank(d) = position in each ranked list (1-indexed)
 * 
 * ## Properties
 * 
 * - **Rank-based**: Only considers position, not raw scores
 * - **Robust to outliers**: Works well even if one system has poor scores
 * - **Simple to implement**: No normalization or calibration needed
 * 
 * ## Usage
 * 
 * ```kotlin
 * val rrf = RRFScorer()
 * 
 * val rankedLists = mapOf(
 *     "keyword" to listOf("doc1", "doc2", "doc3"),
 *     "semantic" to listOf("doc2", "doc1", "doc4")
 * )
 * 
 * val fusedResults = rrf.fuse(rankedLists)
 * // Returns items sorted by combined RRF score
 * ```
 * 
 * @see <a href="https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf">RRF Paper</a>
 */
class RRFScorer<T>(
    private val config: RRFConfig = RRFConfig()
) {
    
    /**
     * Fuse multiple ranked lists using RRF.
     * 
     * @param rankedLists Map of source name to ranked list of items
     * @return List of items with RRF scores, sorted by score descending
     */
    fun fuse(rankedLists: Map<String, List<T>>): List<RRFItem<T>> {
        if (rankedLists.isEmpty()) return emptyList()
        
        val itemScores = mutableMapOf<T, Double>()
        val itemSources = mutableMapOf<T, MutableList<String>>()
        val itemRanks = mutableMapOf<T, MutableMap<String, Int>>()
        
        rankedLists.forEach { (source, items) ->
            items.forEachIndexed { index, item ->
                val rank = index + 1  // 1-indexed
                val rrfScore = 1.0 / (config.k + rank)
                
                itemScores[item] = (itemScores[item] ?: 0.0) + rrfScore
                itemSources.getOrPut(item) { mutableListOf() }.add(source)
                itemRanks.getOrPut(item) { mutableMapOf() }[source] = rank
            }
        }
        
        return itemScores.map { (item, score) ->
            RRFItem(
                item = item,
                score = score,
                sources = itemSources[item] ?: emptyList(),
                ranks = itemRanks[item] ?: emptyMap()
            )
        }.sortedByDescending { it.score }
    }
    
    /**
     * Fuse ranked lists and return normalized scores (0-1 range).
     * 
     * Normalization is done by dividing by the maximum possible score
     * (which is the number of sources, if an item appears at rank 1 in all).
     */
    fun fuseNormalized(rankedLists: Map<String, List<T>>): List<RRFItem<T>> {
        val results = fuse(rankedLists)
        if (results.isEmpty()) return results
        
        val maxPossibleScore = rankedLists.size.toDouble() / (config.k + 1)
        
        return results.map { item ->
            item.copy(score = item.score / maxPossibleScore)
        }
    }
    
    /**
     * Calculate RRF score for a single item given its ranks in different sources.
     * 
     * @param ranks Map of source name to rank (1-indexed)
     * @return RRF score
     */
    fun calculateScore(ranks: Map<String, Int>): Double {
        return ranks.values.sumOf { rank ->
            1.0 / (config.k + rank)
        }
    }
    
    companion object {
        /**
         * Create a typed RRF scorer.
         */
        inline fun <reified T> create(k: Int = 60): RRFScorer<T> = RRFScorer(RRFConfig(k))
    }
}

