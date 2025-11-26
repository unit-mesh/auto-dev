package cc.unitmesh.agent.scoring

/**
 * Represents a model capable of scoring a text against a query.
 * Useful for identifying the most relevant texts when scoring multiple texts against the same query.
 * The scoring model can be employed for re-ranking purposes.
 */
interface ScoringModel {

    /**
     * Scores a given text against a given query.
     *
     * @param text  The text to be scored.
     * @param query The query against which to score the text.
     * @return the score.
     */
    fun score(text: String, query: String): Double {
        return score(TextSegment(text), query)
    }

    /**
     * Scores a given [TextSegment] against a given query.
     *
     * @param segment The [TextSegment] to be scored.
     * @param query   The query against which to score the segment.
     * @return the score.
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
}

/**
 * Represents a segment of text.
 */
data class TextSegment(
    val text: String,
    val metadata: Map<String, Any> = emptyMap()
)
