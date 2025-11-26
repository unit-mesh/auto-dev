package cc.unitmesh.agent.scoring

/**
 * Name matching scorer configuration.
 */
data class NameMatchConfig(
    /** Score for exact name match */
    val exactMatchScore: Double = 50.0,
    /** Score for name starting with query */
    val prefixMatchScore: Double = 35.0,
    /** Score for name containing query */
    val containsMatchScore: Double = 20.0,
    /** Score per occurrence in content (capped) */
    val contentMatchScorePerOccurrence: Double = 2.0,
    /** Maximum score from content matches */
    val maxContentMatchScore: Double = 10.0,
    /** Score for camelCase initials match */
    val initialsMatchScore: Double = 15.0
)

/**
 * Name matching scorer for entity name relevance.
 * 
 * Scores how well an entity name matches the search query.
 * Supports various matching strategies:
 * 
 * - **Exact match**: "AuthService" matches "AuthService" perfectly
 * - **Prefix match**: "Auth" matches "AuthService" 
 * - **Contains match**: "Service" matches "AuthService"
 * - **Initials match**: "AS" matches "AuthService" (camelCase initials)
 * - **Content frequency**: Multiple occurrences boost relevance
 * 
 * ## Usage
 * 
 * ```kotlin
 * val scorer = NameMatchScorer()
 * 
 * val segment = TextSegment(
 *     text = "class AuthService { ... }",
 *     metadata = mapOf("name" to "AuthService")
 * )
 * 
 * scorer.score(segment, "AuthService")  // 60.0 (exact + content)
 * scorer.score(segment, "Auth")         // 45.0 (prefix + content)
 * scorer.score(segment, "AS")           // 25.0 (initials + content)
 * ```
 */
class NameMatchScorer(
    private val config: NameMatchConfig = NameMatchConfig()
) {
    
    /**
     * Score a segment against a query based on name matching.
     * 
     * @param segment The segment to score
     * @param query The search query
     * @return Name match score
     */
    fun score(segment: TextSegment, query: String): Double {
        val name = segment.metadata["name"] as? String ?: ""
        val content = segment.text
        
        var score = 0.0
        
        // Name matching (mutually exclusive, highest match wins)
        score += when {
            name.equals(query, ignoreCase = true) -> config.exactMatchScore
            name.startsWith(query, ignoreCase = true) -> config.prefixMatchScore
            name.contains(query, ignoreCase = true) -> config.containsMatchScore
            else -> 0.0
        }
        
        // Content frequency bonus
        if (content.contains(query, ignoreCase = true)) {
            val occurrences = content.lowercase().split(query.lowercase()).size - 1
            score += minOf(
                occurrences * config.contentMatchScorePerOccurrence,
                config.maxContentMatchScore
            )
        }
        
        // CamelCase initials match (e.g., "AS" matches "AuthService")
        if (query.length >= 2 && query.all { it.isUpperCase() }) {
            val initials = name.filter { it.isUpperCase() }
            if (initials.contains(query)) {
                score += config.initialsMatchScore
            }
        }
        
        return score
    }
    
    /**
     * Score a name directly against a query (without content).
     */
    fun scoreName(name: String, query: String): Double {
        var score = 0.0
        
        score += when {
            name.equals(query, ignoreCase = true) -> config.exactMatchScore
            name.startsWith(query, ignoreCase = true) -> config.prefixMatchScore
            name.contains(query, ignoreCase = true) -> config.containsMatchScore
            else -> 0.0
        }
        
        // CamelCase initials match
        if (query.length >= 2 && query.all { it.isUpperCase() }) {
            val initials = name.filter { it.isUpperCase() }
            if (initials.contains(query)) {
                score += config.initialsMatchScore
            }
        }
        
        return score
    }
    
    /**
     * Score multiple segments.
     */
    fun scoreAll(segments: List<TextSegment>, query: String): List<Double> {
        return segments.map { score(it, query) }
    }
    
    companion object {
        /**
         * Create a strict scorer that only rewards exact matches.
         */
        fun strict(): NameMatchScorer = NameMatchScorer(
            NameMatchConfig(
                exactMatchScore = 100.0,
                prefixMatchScore = 0.0,
                containsMatchScore = 0.0,
                contentMatchScorePerOccurrence = 0.0,
                initialsMatchScore = 0.0
            )
        )
        
        /**
         * Create a fuzzy scorer that rewards partial matches.
         */
        fun fuzzy(): NameMatchScorer = NameMatchScorer(
            NameMatchConfig(
                exactMatchScore = 50.0,
                prefixMatchScore = 40.0,
                containsMatchScore = 30.0,
                contentMatchScorePerOccurrence = 5.0,
                maxContentMatchScore = 20.0,
                initialsMatchScore = 25.0
            )
        )
    }
}

