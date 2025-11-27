package cc.unitmesh.agent.tool.impl.docql

import cc.unitmesh.agent.scoring.ScoringBreakdown
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
    val scoringInfo: ScoringStats? = null,
    /** Keyword expansion statistics (for multi-level keyword search) */
    val keywordExpansion: KeywordExpansionStats? = null,
    /** LLM reranker statistics (when LLM-based reranking is used) */
    val llmRerankerInfo: LLMRerankerStats? = null
) {
    @Serializable
    enum class SearchType {
        SMART_SEARCH,      // Keyword-based multi-channel search with RRF fusion
        DIRECT_QUERY,      // Standard DocQL query
        FALLBACK_CONTENT,  // Fallback to content chunks search
        LLM_RERANKED       // LLM-based metadata reranking
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

        keywordExpansion?.let { kw ->
            put("docql_kw_level_used", kw.levelUsed.toString())
            put("docql_kw_original", kw.originalQuery)
            if (kw.primaryKeywords.isNotEmpty()) {
                put("docql_kw_primary", kw.primaryKeywords.joinToString(","))
            }
            if (kw.secondaryKeywords.isNotEmpty()) {
                put("docql_kw_secondary", kw.secondaryKeywords.joinToString(","))
            }
            put("docql_kw_strategy", kw.strategyUsed)
            put("docql_kw_level1_count", kw.level1ResultCount.toString())
            put("docql_kw_level2_count", kw.level2ResultCount.toString())
        }

        llmRerankerInfo?.let { llm ->
            put("docql_llm_enabled", "true")
            put("docql_llm_success", llm.success.toString())
            put("docql_llm_items_processed", llm.itemsProcessed.toString())
            put("docql_llm_items_reranked", llm.itemsReranked.toString())
            if (llm.tokensUsed > 0) {
                put("docql_llm_tokens", llm.tokensUsed.toString())
            }
            if (llm.latencyMs > 0) {
                put("docql_llm_latency_ms", llm.latencyMs.toString())
            }
            llm.explanation?.let { put("docql_llm_explanation", it.take(200)) }
            llm.error?.let { put("docql_llm_error", it) }
        }
    }

    private fun formatDouble(value: Double, decimals: Int = 2): String {
        return ScoringBreakdown.formatDouble(value, decimals)
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
                scoringInfo = ScoringStats.fromMetadata(metadata),
                keywordExpansion = KeywordExpansionStats.fromMetadata(metadata),
                llmRerankerInfo = LLMRerankerStats.fromMetadata(metadata)
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

/**
 * Statistics about keyword expansion for multi-level search.
 *
 * Tracks which keyword level was used and the search strategy applied.
 */
@Serializable
data class KeywordExpansionStats(
    /** Original query from user */
    val originalQuery: String,
    /** Primary keywords used (Level 1) */
    val primaryKeywords: List<String> = emptyList(),
    /** Secondary keywords used (Level 2) */
    val secondaryKeywords: List<String> = emptyList(),
    /** Which keyword level was ultimately used (1, 2, or 3) */
    val levelUsed: Int = 1,
    /** Strategy used: "KEEP", "EXPAND", "FILTER" */
    val strategyUsed: String = "KEEP",
    /** Result count at Level 1 */
    val level1ResultCount: Int = 0,
    /** Result count at Level 2 (if expanded) */
    val level2ResultCount: Int = 0
) {
    companion object {
        fun fromMetadata(metadata: Map<String, String>): KeywordExpansionStats? {
            val original = metadata["docql_kw_original"] ?: return null

            return KeywordExpansionStats(
                originalQuery = original,
                primaryKeywords = metadata["docql_kw_primary"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                secondaryKeywords = metadata["docql_kw_secondary"]?.split(",")?.filter { it.isNotBlank() }
                    ?: emptyList(),
                levelUsed = metadata["docql_kw_level_used"]?.toIntOrNull() ?: 1,
                strategyUsed = metadata["docql_kw_strategy"] ?: "KEEP",
                level1ResultCount = metadata["docql_kw_level1_count"]?.toIntOrNull() ?: 0,
                level2ResultCount = metadata["docql_kw_level2_count"]?.toIntOrNull() ?: 0
            )
        }
    }
}

/**
 * Statistics about LLM-based metadata reranking.
 *
 * Tracks LLM reranking performance and results.
 */
@Serializable
data class LLMRerankerStats(
    /** Whether LLM reranking was successful */
    val success: Boolean = true,
    /** Number of items processed by LLM */
    val itemsProcessed: Int = 0,
    /** Number of items in final reranked list */
    val itemsReranked: Int = 0,
    /** Tokens used by LLM call */
    val tokensUsed: Int = 0,
    /** Latency of LLM call in milliseconds */
    val latencyMs: Long = 0,
    /** Brief explanation from LLM about ranking logic */
    val explanation: String? = null,
    /** Error message if reranking failed */
    val error: String? = null,
    /** Whether fallback to heuristic was used */
    val usedFallback: Boolean = false
) {
    companion object {
        fun fromMetadata(metadata: Map<String, String>): LLMRerankerStats? {
            // Only parse if LLM reranking was enabled
            if (metadata["docql_llm_enabled"] != "true") return null

            return LLMRerankerStats(
                success = metadata["docql_llm_success"]?.toBooleanStrictOrNull() ?: true,
                itemsProcessed = metadata["docql_llm_items_processed"]?.toIntOrNull() ?: 0,
                itemsReranked = metadata["docql_llm_items_reranked"]?.toIntOrNull() ?: 0,
                tokensUsed = metadata["docql_llm_tokens"]?.toIntOrNull() ?: 0,
                latencyMs = metadata["docql_llm_latency_ms"]?.toLongOrNull() ?: 0,
                explanation = metadata["docql_llm_explanation"],
                error = metadata["docql_llm_error"],
                usedFallback = metadata["docql_llm_error"] != null
            )
        }
    }
}

