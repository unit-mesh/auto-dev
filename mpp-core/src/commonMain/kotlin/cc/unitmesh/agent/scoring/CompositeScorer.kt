package cc.unitmesh.agent.scoring

/**
 * Configuration for composite scoring.
 */
data class CompositeScorerConfig(
    /** Weight for BM25 scoring component */
    val bm25Weight: Double = 0.4,
    /** Weight for type-based scoring component */
    val typeWeight: Double = 0.3,
    /** Weight for name matching scoring component */
    val nameMatchWeight: Double = 0.3,
    /** Scale factor for BM25 to normalize to similar range as other scorers */
    val bm25ScaleFactor: Double = 100.0
)

/**
 * Composite scorer that combines multiple scoring strategies.
 * 
 * This scorer implements a weighted combination of:
 * - **BM25**: Term frequency-based relevance
 * - **Type scoring**: Prioritizes code entities
 * - **Name matching**: Rewards query matches in entity names
 * 
 * The final score is a weighted sum of all component scores.
 * 
 * ## Usage
 * 
 * ```kotlin
 * val scorer = CompositeScorer()
 * 
 * val segments = listOf(
 *     TextSegment("class AuthService", mapOf("type" to "class", "name" to "AuthService")),
 *     TextSegment("user authentication docs", mapOf("type" to "chunk"))
 * )
 * 
 * val scores = scorer.scoreAll(segments, "AuthService")
 * // Returns weighted scores combining all strategies
 * ```
 */
class CompositeScorer(
    private val config: CompositeScorerConfig = CompositeScorerConfig(),
    private val bm25Scorer: BM25Scorer = BM25Scorer(),
    private val typeScorer: TypeScorer = TypeScorer(),
    private val nameMatchScorer: NameMatchScorer = NameMatchScorer()
) : ScoringModel {
    
    override fun scoreAll(segments: List<TextSegment>, query: String): List<Double> {
        if (segments.isEmpty()) return emptyList()
        
        // Tokenize query once for BM25
        val queryTerms = BM25Scorer.tokenize(query)
        
        // Get BM25 scores
        val bm25Scores = bm25Scorer.scoreSegments(segments, queryTerms)
        
        // Get type and name match scores
        val typeScores = typeScorer.scoreAll(segments)
        val nameMatchScores = nameMatchScorer.scoreAll(segments, query)
        
        // Combine scores
        return segments.indices.map { i ->
            combineScores(bm25Scores[i], typeScores[i], nameMatchScores[i])
        }
    }
    
    private fun combineScores(bm25: Double, typeScore: Double, nameMatch: Double): Double {
        return (bm25 * config.bm25ScaleFactor * config.bm25Weight) +
               (typeScore * config.typeWeight) +
               (nameMatch * config.nameMatchWeight)
    }
    
    /**
     * Get individual component scores for debugging/analysis.
     */
    fun scoreWithBreakdown(segment: TextSegment, query: String): ScoringBreakdown {
        val queryTerms = BM25Scorer.tokenize(query)
        val bm25 = bm25Scorer.scoreSegments(listOf(segment), queryTerms).first()
        val typeScore = typeScorer.scoreSegment(segment)
        val nameMatch = nameMatchScorer.score(segment, query)
        
        return ScoringBreakdown(
            bm25 = bm25,
            typeScore = typeScore,
            nameMatch = nameMatch,
            combined = combineScores(bm25, typeScore, nameMatch),
            weights = mapOf(
                "bm25" to config.bm25Weight,
                "type" to config.typeWeight,
                "nameMatch" to config.nameMatchWeight
            )
        )
    }
    
    companion object {
        /**
         * Create a scorer optimized for code search.
         */
        fun forCodeSearch(): CompositeScorer = CompositeScorer(
            config = CompositeScorerConfig(
                bm25Weight = 0.3,
                typeWeight = 0.4,  // Higher weight for code entities
                nameMatchWeight = 0.3
            ),
            typeScorer = TypeScorer.codeOnly()
        )
        
        /**
         * Create a scorer optimized for documentation search.
         */
        fun forDocSearch(): CompositeScorer = CompositeScorer(
            config = CompositeScorerConfig(
                bm25Weight = 0.5,  // Higher weight for term matching
                typeWeight = 0.2,
                nameMatchWeight = 0.3
            )
        )
    }
}

/**
 * Detailed scoring breakdown for analysis.
 */
data class ScoringBreakdown(
    val bm25: Double,
    val typeScore: Double,
    val nameMatch: Double,
    val combined: Double,
    val weights: Map<String, Double>
) {
    override fun toString(): String {
        return buildString {
            appendLine("Scoring Breakdown:")
            appendLine("  BM25: ${formatDouble(bm25)} (weight: ${formatDouble(weights["bm25"]!! * 100, 1)}%)")
            appendLine("  Type: ${formatDouble(typeScore)} (weight: ${formatDouble(weights["type"]!! * 100, 1)}%)")
            appendLine("  Name: ${formatDouble(nameMatch)} (weight: ${formatDouble(weights["nameMatch"]!! * 100, 1)}%)")
            appendLine("  Combined: ${formatDouble(combined)}")
        }
    }
    
    private fun formatDouble(value: Double, decimals: Int = 2): String {
        val factor = when (decimals) {
            0 -> 1.0
            1 -> 10.0
            2 -> 100.0
            3 -> 1000.0
            else -> generateSequence(1.0) { it * 10 }.take(decimals + 1).last()
        }
        val rounded = kotlin.math.round(value * factor) / factor
        val str = rounded.toString()
        val dotIndex = str.indexOf('.')
        return if (dotIndex == -1) {
            "$str.${"0".repeat(decimals)}"
        } else {
            val currentDecimals = str.length - dotIndex - 1
            if (currentDecimals >= decimals) {
                str.substring(0, dotIndex + decimals + 1)
            } else {
                str + "0".repeat(decimals - currentDecimals)
            }
        }
    }
}

