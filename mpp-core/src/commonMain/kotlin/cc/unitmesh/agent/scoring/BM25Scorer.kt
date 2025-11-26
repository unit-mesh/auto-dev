package cc.unitmesh.agent.scoring

import kotlin.math.ln

/**
 * BM25 configuration parameters.
 * 
 * @param k1 Term frequency saturation parameter (typically 1.2)
 * @param b Document length normalization parameter (typically 0.75)
 * @param avgDocLength Default average document length when cannot be computed
 */
data class BM25Config(
    val k1: Double = 1.2,
    val b: Double = 0.75,
    val avgDocLength: Double = 100.0
)

/**
 * BM25 (Best Matching 25) scoring algorithm implementation.
 * 
 * BM25 is a bag-of-words retrieval function that ranks documents based on
 * query terms appearing in each document, regardless of their proximity.
 * 
 * ## Formula
 * 
 * For each query term qi:
 * ```
 * score(D, Q) = Σ IDF(qi) * (f(qi,D) * (k1+1)) / (f(qi,D) + k1 * (1 - b + b * |D|/avgdl))
 * ```
 * 
 * Where:
 * - f(qi, D) = term frequency of qi in document D
 * - |D| = document length
 * - avgdl = average document length in the collection
 * - k1, b = tuning parameters
 * 
 * ## Usage
 * 
 * ```kotlin
 * val scorer = BM25Scorer()
 * 
 * // Single document scoring
 * val score = scorer.score("document text", listOf("query", "terms"))
 * 
 * // Batch scoring (more efficient)
 * val documents = listOf("doc1", "doc2", "doc3")
 * val scores = scorer.scoreAll(documents, listOf("query"))
 * ```
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Okapi_BM25">BM25 on Wikipedia</a>
 */
class BM25Scorer(
    private val config: BM25Config = BM25Config()
) {
    
    /**
     * Score a single document against query terms.
     * 
     * Note: This is less efficient than [scoreAll] for multiple documents
     * as IDF cannot be computed properly for a single document.
     * 
     * @param text The document text to score
     * @param queryTerms Pre-tokenized query terms
     * @return BM25 score (higher = more relevant)
     */
    fun score(text: String, queryTerms: List<String>): Double {
        val docTerms = tokenize(text)
        val termFreq = docTerms.groupingBy { it }.eachCount()
        val docLen = docTerms.size.toDouble()
        
        var score = 0.0
        
        for (term in queryTerms) {
            val tf = termFreq[term] ?: 0
            if (tf == 0) continue
            
            // For single doc, use simplified IDF (assume term appears in 1 of 2 docs)
            val idf = ln(2.0)
            
            val numerator = tf * (config.k1 + 1)
            val denominator = tf + config.k1 * (1 - config.b + config.b * docLen / config.avgDocLength)
            
            score += idf * (numerator / denominator)
        }
        
        return score
    }
    
    /**
     * Score multiple documents against query terms.
     * 
     * This is more accurate than scoring single documents as it computes
     * proper IDF values across the document collection.
     * 
     * @param documents List of document texts to score
     * @param queryTerms Pre-tokenized query terms
     * @return List of BM25 scores (same order as input documents)
     */
    fun scoreAll(documents: List<String>, queryTerms: List<String>): List<Double> {
        if (documents.isEmpty()) return emptyList()
        if (queryTerms.isEmpty()) return documents.map { 0.0 }
        
        // Calculate average document length
        val tokenizedDocs = documents.map { tokenize(it) }
        val avgDocLen = tokenizedDocs.map { it.size.toDouble() }.average()
            .takeIf { it > 0 } ?: config.avgDocLength
        
        // Build document frequency map for IDF
        val docFreq = buildDocumentFrequency(tokenizedDocs, queryTerms)
        val numDocs = documents.size
        
        return tokenizedDocs.map { docTerms ->
            calculateBM25(docTerms, queryTerms, avgDocLen, docFreq, numDocs)
        }
    }
    
    /**
     * Score TextSegments against query terms.
     * Uses segment text for scoring.
     */
    fun scoreSegments(segments: List<TextSegment>, queryTerms: List<String>): List<Double> {
        return scoreAll(segments.map { it.text }, queryTerms)
    }
    
    private fun calculateBM25(
        docTerms: List<String>,
        queryTerms: List<String>,
        avgDocLen: Double,
        docFreq: Map<String, Int>,
        numDocs: Int
    ): Double {
        val termFreq = docTerms.groupingBy { it }.eachCount()
        val docLen = docTerms.size.toDouble()
        
        var score = 0.0
        
        for (term in queryTerms) {
            val tf = termFreq[term] ?: 0
            if (tf == 0) continue
            
            // IDF with smoothing to avoid negative values
            val df = docFreq[term] ?: 0
            val idf = ln((numDocs - df + 0.5) / (df + 0.5) + 1.0)
            
            // BM25 term score
            val numerator = tf * (config.k1 + 1)
            val denominator = tf + config.k1 * (1 - config.b + config.b * docLen / avgDocLen)
            
            score += idf * (numerator / denominator)
        }
        
        return score
    }
    
    private fun buildDocumentFrequency(
        tokenizedDocs: List<List<String>>,
        queryTerms: List<String>
    ): Map<String, Int> {
        val docFreq = mutableMapOf<String, Int>()
        
        for (docTerms in tokenizedDocs) {
            val uniqueTerms = docTerms.toSet()
            for (term in queryTerms) {
                if (term in uniqueTerms) {
                    docFreq[term] = (docFreq[term] ?: 0) + 1
                }
            }
        }
        
        return docFreq
    }
    
    companion object {
        /**
         * Tokenize text into terms for BM25.
         * Handles camelCase, snake_case, and common separators.
         * 
         * @param text The text to tokenize
         * @return List of lowercase tokens
         */
        fun tokenize(text: String): List<String> {
            return text
                .replace(Regex("([a-z])([A-Z])"), "$1 $2")  // camelCase
                .replace(Regex("[_\\-./]"), " ")            // separators
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.length > 1 }  // Filter single chars
        }
    }
}

