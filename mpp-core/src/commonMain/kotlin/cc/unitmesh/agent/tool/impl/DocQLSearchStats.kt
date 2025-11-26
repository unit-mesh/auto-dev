package cc.unitmesh.agent.tool.impl

import kotlinx.serialization.Serializable

/**
 * Search statistics for DocQL tool results.
 * Provides detailed information about the search execution for transparency.
 */
@Serializable
data class DocQLSearchStats(
    /** Type of search performed */
    val searchType: SearchType,
    /** Query channels used (for smart search) */
    val channels: List<String> = emptyList(),
    /** Documents/files searched */
    val documentsSearched: Int = 0,
    /** Total raw results before reranking */
    val totalRawResults: Int = 0,
    /** Results after reranking and filtering */
    val resultsAfterRerank: Int = 0,
    /** Whether results were truncated due to maxResults */
    val truncated: Boolean = false,
    /** Whether fallback search was used */
    val usedFallback: Boolean = false,
    /** Reranker configuration used */
    val rerankerConfig: RerankerStats? = null,
    /** Scoring breakdown for top results */
    val scoringInfo: ScoringStats? = null
) {
    @Serializable
    enum class SearchType {
        SMART_SEARCH,      // Keyword-based multi-channel search with RRF fusion
        DIRECT_QUERY,      // Standard DocQL query
        FALLBACK_CONTENT   // Fallback to content chunks search
    }
    
    /**
     * Serialize to metadata map for ToolResult
     */
    fun toMetadata(): Map<String, String> = buildMap {
        put("docql_search_type", searchType.name)
        if (channels.isNotEmpty()) {
            put("docql_channels", channels.joinToString(","))
        }
        put("docql_docs_searched", documentsSearched.toString())
        put("docql_raw_results", totalRawResults.toString())
        put("docql_reranked_results", resultsAfterRerank.toString())
        put("docql_truncated", truncated.toString())
        put("docql_used_fallback", usedFallback.toString())
        
        rerankerConfig?.let { config ->
            put("docql_reranker_type", config.rerankerType)
            put("docql_rrf_k", config.rrfK.toString())
            put("docql_rrf_weight", config.rrfWeight.toString())
            put("docql_content_weight", config.contentWeight.toString())
            put("docql_min_score", config.minScoreThreshold.toString())
        }
        
        scoringInfo?.let { scoring ->
            put("docql_scorer_components", scoring.scorerComponents.joinToString(","))
            put("docql_avg_score", formatDouble(scoring.avgScore))
            put("docql_max_score", formatDouble(scoring.maxScore))
            put("docql_min_score_value", formatDouble(scoring.minScore))
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
        // Ensure we have exactly `decimals` decimal places
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
    
    companion object {
        /**
         * Parse from metadata map
         */
        fun fromMetadata(metadata: Map<String, String>): DocQLSearchStats? {
            val searchTypeStr = metadata["docql_search_type"] ?: return null
            val searchType = try {
                SearchType.valueOf(searchTypeStr)
            } catch (e: IllegalArgumentException) {
                return null
            }
            
            return DocQLSearchStats(
                searchType = searchType,
                channels = metadata["docql_channels"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                documentsSearched = metadata["docql_docs_searched"]?.toIntOrNull() ?: 0,
                totalRawResults = metadata["docql_raw_results"]?.toIntOrNull() ?: 0,
                resultsAfterRerank = metadata["docql_reranked_results"]?.toIntOrNull() ?: 0,
                truncated = metadata["docql_truncated"]?.toBooleanStrictOrNull() ?: false,
                usedFallback = metadata["docql_used_fallback"]?.toBooleanStrictOrNull() ?: false,
                rerankerConfig = RerankerStats.fromMetadata(metadata),
                scoringInfo = ScoringStats.fromMetadata(metadata)
            )
        }
    }
}

/**
 * Statistics about the reranker configuration used
 */
@Serializable
data class RerankerStats(
    /** Type of reranker (e.g., "RRF+BM25", "BM25", "RRF") */
    val rerankerType: String,
    /** RRF constant k */
    val rrfK: Int,
    /** Weight for RRF score */
    val rrfWeight: Double,
    /** Weight for content/BM25 score */
    val contentWeight: Double,
    /** Minimum score threshold used */
    val minScoreThreshold: Double
) {
    companion object {
        fun fromMetadata(metadata: Map<String, String>): RerankerStats? {
            val type = metadata["docql_reranker_type"] ?: return null
            return RerankerStats(
                rerankerType = type,
                rrfK = metadata["docql_rrf_k"]?.toIntOrNull() ?: 60,
                rrfWeight = metadata["docql_rrf_weight"]?.toDoubleOrNull() ?: 0.4,
                contentWeight = metadata["docql_content_weight"]?.toDoubleOrNull() ?: 0.6,
                minScoreThreshold = metadata["docql_min_score"]?.toDoubleOrNull() ?: 0.0
            )
        }
    }
}

/**
 * Statistics about the scoring results
 */
@Serializable
data class ScoringStats(
    /** Scorer components used (e.g., ["BM25", "TypeScore", "NameMatch"]) */
    val scorerComponents: List<String>,
    /** Average score of returned results */
    val avgScore: Double,
    /** Maximum score */
    val maxScore: Double,
    /** Minimum score (after filtering) */
    val minScore: Double
) {
    companion object {
        fun fromMetadata(metadata: Map<String, String>): ScoringStats? {
            val components = metadata["docql_scorer_components"]?.split(",")?.filter { it.isNotBlank() }
            if (components.isNullOrEmpty()) return null
            
            return ScoringStats(
                scorerComponents = components,
                avgScore = metadata["docql_avg_score"]?.toDoubleOrNull() ?: 0.0,
                maxScore = metadata["docql_max_score"]?.toDoubleOrNull() ?: 0.0,
                minScore = metadata["docql_min_score_value"]?.toDoubleOrNull() ?: 0.0
            )
        }
    }
}

