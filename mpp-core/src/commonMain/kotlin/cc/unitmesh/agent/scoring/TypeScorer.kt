package cc.unitmesh.agent.scoring

/**
 * Type-based scoring configuration.
 * Maps content types to their base scores.
 */
data class TypeScorerConfig(
    val typeWeights: Map<String, Double> = DEFAULT_WEIGHTS
) {
    companion object {
        val DEFAULT_WEIGHTS = mapOf(
            "class" to 100.0,
            "function" to 90.0,
            "interface" to 85.0,
            "method" to 80.0,
            "struct" to 75.0,
            "enum" to 70.0,
            "constant" to 65.0,
            "heading" to 60.0,
            "toc" to 40.0,
            "chunk" to 20.0
        )
        
        val DEFAULT_SCORE = 10.0
    }
}

/**
 * Type-based scorer for content prioritization.
 * 
 * Assigns scores based on content type, useful for prioritizing
 * code entities (classes, functions) over documentation chunks.
 * 
 * ## Design Rationale
 * 
 * In code search scenarios, users typically care more about:
 * 1. **Code entities** (classes, functions) - they define the structure
 * 2. **API definitions** - how to use the code
 * 3. **Documentation** - explanations and context
 * 
 * This scorer reflects that priority by assigning higher scores
 * to code entities and lower scores to documentation chunks.
 * 
 * ## Usage
 * 
 * ```kotlin
 * val scorer = TypeScorer()
 * 
 * val classScore = scorer.score("class")      // 100.0
 * val functionScore = scorer.score("function") // 90.0
 * val chunkScore = scorer.score("chunk")       // 20.0
 * ```
 */
class TypeScorer(
    private val config: TypeScorerConfig = TypeScorerConfig()
) {
    
    /**
     * Get the score for a content type.
     * 
     * @param type The content type (e.g., "class", "function", "chunk")
     * @return The type score
     */
    fun score(type: String): Double {
        return config.typeWeights[type.lowercase()] ?: TypeScorerConfig.DEFAULT_SCORE
    }
    
    /**
     * Score a TextSegment based on its type metadata.
     * 
     * @param segment The segment to score
     * @return The type score
     */
    fun scoreSegment(segment: TextSegment): Double {
        val type = segment.metadata["type"] as? String ?: "unknown"
        return score(type)
    }
    
    /**
     * Score multiple segments by their types.
     */
    fun scoreAll(segments: List<TextSegment>): List<Double> {
        return segments.map { scoreSegment(it) }
    }
    
    /**
     * Get all configured type weights.
     */
    fun getTypeWeights(): Map<String, Double> = config.typeWeights
    
    companion object {
        /**
         * Create a TypeScorer with custom weights.
         */
        fun withWeights(vararg weights: Pair<String, Double>): TypeScorer {
            return TypeScorer(TypeScorerConfig(weights.toMap()))
        }
        
        /**
         * Create a TypeScorer that only prioritizes code entities.
         */
        fun codeOnly(): TypeScorer = TypeScorer(
            TypeScorerConfig(
                mapOf(
                    "class" to 100.0,
                    "function" to 90.0,
                    "interface" to 85.0,
                    "method" to 80.0,
                    "struct" to 75.0,
                    "enum" to 70.0,
                    "constant" to 65.0
                )
            )
        )
    }
}

