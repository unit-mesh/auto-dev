package cc.unitmesh.agent.scoring

/**
 * Represents a model capable of scoring a text against a query.
 * Useful for identifying the most relevant texts when scoring multiple texts against the same query.
 * The scoring model can be employed for re-ranking purposes.
 * 
 * ## Usage Scenarios
 * 
 * 1. **Simple Scoring**: Use [score] for single item evaluation
 * 2. **Batch Scoring**: Use [scoreAll] for efficient batch processing
 * 3. **Reranking**: Use [scoreAndRank] to get sorted results with scores
 * 
 * ## Implementations
 * 
 * - [HeuristicScoringModel]: Simple type-based + name matching scoring
 * - [Reranker]: Advanced BM25 + RRF fusion for multi-source retrieval
 */
interface ScoringModel {

    /**
     * Scores a given text against a given query.
     *
     * @param text  The text to be scored.
     * @param query The query against which to score the text.
     * @return the score (higher = more relevant).
     */
    fun score(text: String, query: String): Double {
        return score(TextSegment(text), query)
    }

    /**
     * Scores a given [TextSegment] against a given query.
     *
     * @param segment The [TextSegment] to be scored.
     * @param query   The query against which to score the segment.
     * @return the score (higher = more relevant).
     */
    fun score(segment: TextSegment, query: String): Double {
        val scores = scoreAll(listOf(segment), query)
        return scores.firstOrNull() ?: 0.0
    }

    /**
     * Scores all provided [TextSegment]s against a given query.
     *
     * @param segments The list of [TextSegment]s to score.
     * @param query    The query against which to score the segments.
     * @return the list of scores. The order of scores corresponds to the order of [TextSegment]s.
     */
    fun scoreAll(segments: List<TextSegment>, query: String): List<Double>

    /**
     * Scores and ranks segments, returning them sorted by score (descending).
     * 
     * @param segments The list of [TextSegment]s to score and rank.
     * @param query    The query against which to score the segments.
     * @param maxResults Maximum number of results to return (default: all).
     * @return List of pairs (segment, score) sorted by score descending.
     */
    fun scoreAndRank(
        segments: List<TextSegment>,
        query: String,
        maxResults: Int = Int.MAX_VALUE
    ): List<Pair<TextSegment, Double>> {
        val scores = scoreAll(segments, query)
        return segments.zip(scores)
            .sortedByDescending { it.second }
            .take(maxResults)
    }
}

/**
 * Represents a segment of text with optional metadata.
 * 
 * Common metadata keys:
 * - `type`: The type of content ("class", "function", "heading", "chunk", etc.)
 * - `name`: The name of the entity (class name, function name, heading title)
 * - `id`: Unique identifier for deduplication
 * - `filePath`: Source file path
 * - `line`: Line number in source file
 */
data class TextSegment(
    val text: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    /** Get the content type from metadata */
    val type: String get() = metadata["type"] as? String ?: "unknown"
    
    /** Get the entity name from metadata */
    val name: String get() = metadata["name"] as? String ?: ""
    
    /** Get the unique ID from metadata */
    val id: String? get() = metadata["id"] as? String
    
    /** Get the file path from metadata */
    val filePath: String? get() = metadata["filePath"] as? String
    
    companion object {
        /**
         * Create a TextSegment for a code entity (class/function).
         */
        fun forCode(
            name: String,
            type: String,
            content: String = name,
            filePath: String? = null,
            line: Int? = null
        ): TextSegment = TextSegment(
            text = content,
            metadata = buildMap {
                put("type", type)
                put("name", name)
                put("id", "${filePath ?: ""}:$name:${line ?: 0}")
                filePath?.let { put("filePath", it) }
                line?.let { put("line", it) }
            }
        )
        
        /**
         * Create a TextSegment for a document chunk.
         */
        fun forChunk(
            content: String,
            filePath: String? = null,
            heading: String? = null
        ): TextSegment = TextSegment(
            text = content,
            metadata = buildMap {
                put("type", "chunk")
                heading?.let { put("name", it) }
                put("id", "${filePath ?: ""}:${content.hashCode()}")
                filePath?.let { put("filePath", it) }
            }
        )
    }
}
