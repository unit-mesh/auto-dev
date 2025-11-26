package cc.unitmesh.agent.scoring

import cc.unitmesh.llm.KoogLLMService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

/**
 * Rich metadata for document reranking.
 * Contains all relevant attributes for LLM-based relevance assessment.
 */
@Serializable
data class DocumentMetadataItem(
    /** Unique identifier for the document/segment */
    val id: String,
    /** File path */
    val filePath: String,
    /** File name without path */
    val fileName: String,
    /** File extension (e.g., "md", "kt", "java") */
    val extension: String,
    /** Directory containing the file */
    val directory: String,
    /** Content type ("class", "function", "heading", "chunk", etc.) */
    val contentType: String,
    /** Entity name (class name, function name, heading title) */
    val name: String,
    /** Brief content preview (first 200 chars) */
    val preview: String,
    /** Top-level heading (H1) if available */
    val h1Heading: String? = null,
    /** Parent heading if in a section */
    val parentHeading: String? = null,
    /** Last modification timestamp */
    val lastModified: Long = 0,
    /** File size in bytes */
    val fileSize: Long = 0,
    /** Document format type */
    val formatType: String? = null,
    /** Line number in source file */
    val lineNumber: Int? = null,
    /** References to other documents/entities */
    val references: List<String> = emptyList(),
    /** Tags/labels extracted from content */
    val tags: List<String> = emptyList(),
    /** Relevance hint from heuristic scoring */
    val heuristicScore: Double = 0.0
)

/**
 * Configuration for LLM-based metadata reranker.
 */
@Serializable
data class LLMMetadataRerankerConfig(
    /** Maximum number of items to include in LLM prompt (to avoid token limits) */
    val maxItemsForLLM: Int = 20,
    /** Maximum number of results to return */
    val maxResults: Int = 10,
    /** Whether to include preview text in LLM prompt */
    val includePreview: Boolean = true,
    /** Maximum preview length */
    val maxPreviewLength: Int = 100,
    /** Weight for LLM score vs heuristic score (0.0-1.0) */
    val llmWeight: Double = 0.7,
    /** Whether to use batch reranking (single prompt) or individual scoring */
    val batchReranking: Boolean = true
)

/**
 * Result of LLM-based reranking.
 */
@Serializable
data class LLMRerankResult(
    /** Reranked item IDs in order of relevance */
    val rankedIds: List<String>,
    /** Relevance scores for each item (0.0-10.0) */
    val scores: Map<String, Double>,
    /** Brief explanation from LLM */
    val explanation: String? = null,
    /** Whether LLM reranking was successful */
    val success: Boolean = true,
    /** Error message if failed */
    val error: String? = null,
    /** LLM tokens used */
    val tokensUsed: Int = 0
)

/**
 * LLM-based Metadata Reranker.
 * 
 * Uses AI to assess document relevance based on rich metadata including:
 * - File path and name patterns
 * - Content type (class, function, heading, etc.)
 * - Document structure (headings, sections)
 * - Modification time (prefer recent for some queries)
 * - References and relationships
 * 
 * ## Strategy
 * 
 * 1. First, apply heuristic scoring to pre-filter candidates
 * 2. Prepare metadata summary for top candidates
 * 3. Send to LLM with query context for intelligent reranking
 * 4. Combine LLM scores with heuristic scores
 * 
 * ## Example Usage
 * 
 * ```kotlin
 * val reranker = LLMMetadataReranker(llmService)
 * val items = listOf(
 *     DocumentMetadataItem(
 *         id = "1",
 *         filePath = "src/Auth.kt",
 *         fileName = "Auth.kt",
 *         extension = "kt",
 *         directory = "src",
 *         contentType = "class",
 *         name = "AuthService",
 *         preview = "class AuthService { ... }"
 *     ),
 *     // ... more items
 * )
 * val result = reranker.rerank(items, "authentication service implementation")
 * ```
 */
class LLMMetadataReranker(
    private val llmService: KoogLLMService,
    private val config: LLMMetadataRerankerConfig = LLMMetadataRerankerConfig()
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    /**
     * Rerank items using LLM-based metadata analysis.
     * 
     * @param items List of items with metadata
     * @param query User's search query
     * @return Reranking result with ordered IDs and scores
     */
    suspend fun rerank(
        items: List<DocumentMetadataItem>,
        query: String
    ): LLMRerankResult {
        if (items.isEmpty()) {
            return LLMRerankResult(
                rankedIds = emptyList(),
                scores = emptyMap(),
                success = true
            )
        }
        
        // Step 1: Pre-filter using heuristic scores if too many items
        val candidates = if (items.size > config.maxItemsForLLM) {
            items.sortedByDescending { it.heuristicScore }
                .take(config.maxItemsForLLM)
        } else {
            items
        }
        
        // Step 2: Build LLM prompt
        val prompt = buildRerankPrompt(candidates, query)
        
        // Step 3: Get LLM response
        return try {
            val response = collectLLMResponse(prompt)
            parseRerankResponse(response, candidates)
        } catch (e: Exception) {
            logger.error(e) { "LLM reranking failed, falling back to heuristic" }
            fallbackToHeuristic(candidates)
        }
    }
    
    /**
     * Rerank TextSegments by first extracting metadata.
     */
    suspend fun rerankSegments(
        segments: List<TextSegment>,
        query: String,
        metadataExtractor: (TextSegment) -> DocumentMetadataItem
    ): RerankResult<TextSegment> {
        val items = segments.map { metadataExtractor(it) }
        val llmResult = rerank(items, query)
        
        if (!llmResult.success) {
            // Fall back to heuristic-only reranking
            val heuristicScorer = CompositeScorer()
            return DocumentReranker(
                config = DocumentRerankerConfig(maxResults = config.maxResults)
            ).rerankSegments(segments, query)
        }
        
        // Map back to segments with LLM scores
        val idToSegment = segments.zip(items).associate { (segment, item) -> item.id to segment }
        val scoredItems = llmResult.rankedIds.mapNotNull { id ->
            val segment = idToSegment[id] ?: return@mapNotNull null
            val score = llmResult.scores[id] ?: 0.0
            ScoredItem(
                item = segment,
                score = score,
                source = "llm_rerank",
                metadata = mapOf("llm_score" to score)
            )
        }
        
        return RerankResult(
            items = scoredItems.take(config.maxResults),
            totalCount = scoredItems.size,
            truncated = scoredItems.size > config.maxResults
        )
    }
    
    /**
     * Build the prompt for LLM reranking.
     */
    private fun buildRerankPrompt(items: List<DocumentMetadataItem>, query: String): String {
        return buildString {
            appendLine("You are a document relevance ranking expert. Rank the following documents by relevance to the user's query.")
            appendLine()
            appendLine("## User Query")
            appendLine("\"$query\"")
            appendLine()
            appendLine("## Documents to Rank")
            appendLine()
            
            items.forEachIndexed { index, item ->
                appendLine("### [${item.id}] ${item.name}")
                appendLine("- **Type**: ${item.contentType}")
                appendLine("- **Path**: ${item.filePath}")
                if (item.h1Heading != null) {
                    appendLine("- **Document Title**: ${item.h1Heading}")
                }
                if (item.parentHeading != null) {
                    appendLine("- **Section**: ${item.parentHeading}")
                }
                if (config.includePreview && item.preview.isNotBlank()) {
                    val preview = item.preview.take(config.maxPreviewLength)
                    appendLine("- **Preview**: $preview${if (item.preview.length > config.maxPreviewLength) "..." else ""}")
                }
                if (item.tags.isNotEmpty()) {
                    appendLine("- **Tags**: ${item.tags.joinToString(", ")}")
                }
                appendLine()
            }
            
            appendLine("## Instructions")
            appendLine()
            appendLine("1. Analyze each document's relevance to the query")
            appendLine("2. Consider: name match, content type match, path relevance, document structure")
            appendLine("3. Return a JSON object with exactly this format:")
            appendLine()
            appendLine("```json")
            appendLine("{")
            appendLine("  \"ranked_ids\": [\"id1\", \"id2\", ...],  // IDs in order of relevance (most relevant first)")
            appendLine("  \"scores\": {\"id1\": 9.5, \"id2\": 8.0, ...},  // Relevance scores (0-10)")
            appendLine("  \"explanation\": \"Brief explanation of ranking logic\"")
            appendLine("}")
            appendLine("```")
            appendLine()
            appendLine("Respond ONLY with the JSON object, no other text.")
        }
    }
    
    /**
     * Collect complete LLM response.
     */
    private suspend fun collectLLMResponse(prompt: String): String {
        val responseBuilder = StringBuilder()
        llmService.streamPrompt(
            userPrompt = prompt,
            compileDevIns = false
        ).toList().forEach { chunk ->
            responseBuilder.append(chunk)
        }
        return responseBuilder.toString()
    }
    
    /**
     * Parse LLM response into rerank result.
     */
    private fun parseRerankResponse(
        response: String,
        candidates: List<DocumentMetadataItem>
    ): LLMRerankResult {
        return try {
            // Extract JSON from response (handle markdown code blocks)
            val jsonStr = extractJson(response)
            val parsed = json.decodeFromString<LLMRerankResponse>(jsonStr)
            
            // Validate and normalize
            val validIds = candidates.map { it.id }.toSet()
            val rankedIds = parsed.ranked_ids.filter { it in validIds }
            val scores = parsed.scores.filterKeys { it in validIds }
            
            // Add any missing items with low scores
            val missingIds = validIds - rankedIds.toSet()
            val completeRankedIds = rankedIds + missingIds.toList()
            val completeScores = scores + missingIds.associateWith { 0.0 }
            
            LLMRerankResult(
                rankedIds = completeRankedIds,
                scores = completeScores,
                explanation = parsed.explanation,
                success = true
            )
        } catch (e: Exception) {
            logger.warn { "Failed to parse LLM rerank response: ${e.message}" }
            fallbackToHeuristic(candidates)
        }
    }
    
    /**
     * Extract JSON from response (handles markdown code blocks).
     */
    private fun extractJson(response: String): String {
        // Try to find JSON in code block
        val codeBlockPattern = Regex("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```")
        val match = codeBlockPattern.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // Try to find raw JSON object
        val jsonPattern = Regex("\\{[\\s\\S]*\\}")
        val jsonMatch = jsonPattern.find(response)
        if (jsonMatch != null) {
            return jsonMatch.value
        }
        
        return response.trim()
    }
    
    /**
     * Fallback to heuristic-based ranking when LLM fails.
     */
    private fun fallbackToHeuristic(candidates: List<DocumentMetadataItem>): LLMRerankResult {
        val sorted = candidates.sortedByDescending { it.heuristicScore }
        return LLMRerankResult(
            rankedIds = sorted.map { it.id },
            scores = sorted.associate { it.id to it.heuristicScore },
            explanation = "Fallback to heuristic scoring",
            success = false,
            error = "LLM reranking failed, used heuristic fallback"
        )
    }
    
    companion object {
        /**
         * Create a DocumentMetadataItem from TextSegment with additional context.
         */
        fun createMetadataItem(
            segment: TextSegment,
            index: Int,
            h1Heading: String? = null,
            parentHeading: String? = null,
            lastModified: Long = 0,
            fileSize: Long = 0,
            heuristicScore: Double = 0.0
        ): DocumentMetadataItem {
            val filePath = segment.filePath ?: ""
            val fileName = filePath.substringAfterLast('/')
            val extension = fileName.substringAfterLast('.', "")
            val directory = filePath.substringBeforeLast('/', "")
            
            return DocumentMetadataItem(
                id = segment.id ?: "item_$index",
                filePath = filePath,
                fileName = fileName,
                extension = extension,
                directory = directory,
                contentType = segment.type,
                name = segment.name.ifEmpty { fileName },
                preview = segment.text.take(200),
                h1Heading = h1Heading,
                parentHeading = parentHeading,
                lastModified = lastModified,
                fileSize = fileSize,
                heuristicScore = heuristicScore
            )
        }
    }
}

/**
 * Internal DTO for parsing LLM response.
 */
@Serializable
private data class LLMRerankResponse(
    val ranked_ids: List<String> = emptyList(),
    val scores: Map<String, Double> = emptyMap(),
    val explanation: String? = null
)

/**
 * Reranker type selection for user configuration.
 */
@Serializable
enum class RerankerType {
    /** Heuristic-based reranker (BM25 + Type + Name matching) */
    HEURISTIC,
    
    /** RRF + Composite scorer */
    RRF_COMPOSITE,
    
    /** LLM-based metadata reranker */
    LLM_METADATA,
    
    /** Hybrid: Heuristic pre-filter + LLM rerank */
    HYBRID
}

/**
 * Factory for creating appropriate reranker based on type.
 */
object RerankerFactory {
    /**
     * Create a reranker based on type.
     * 
     * @param type Reranker type
     * @param llmService LLM service (required for LLM_METADATA and HYBRID types)
     * @param config Reranker configuration
     * @return ScoringModel implementation
     */
    fun create(
        type: RerankerType,
        llmService: KoogLLMService? = null,
        maxResults: Int = 20
    ): ScoringModel {
        return when (type) {
            RerankerType.HEURISTIC -> CompositeScorer()
            RerankerType.RRF_COMPOSITE -> DocumentReranker(
                config = DocumentRerankerConfig(maxResults = maxResults)
            )
            RerankerType.LLM_METADATA -> {
                requireNotNull(llmService) { "LLM service required for LLM_METADATA reranker" }
                // Wrap LLMMetadataReranker as ScoringModel
                LLMMetadataRerankerWrapper(
                    reranker = LLMMetadataReranker(
                        llmService = llmService,
                        config = LLMMetadataRerankerConfig(maxResults = maxResults)
                    )
                )
            }
            RerankerType.HYBRID -> {
                requireNotNull(llmService) { "LLM service required for HYBRID reranker" }
                HybridReranker(
                    compositeScorer = CompositeScorer(),
                    llmReranker = LLMMetadataReranker(
                        llmService = llmService,
                        config = LLMMetadataRerankerConfig(maxResults = maxResults)
                    ),
                    maxResults = maxResults
                )
            }
        }
    }
}

/**
 * Wrapper to adapt LLMMetadataReranker to ScoringModel interface.
 */
private class LLMMetadataRerankerWrapper(
    private val reranker: LLMMetadataReranker
) : ScoringModel {
    
    override fun scoreAll(segments: List<TextSegment>, query: String): List<Double> {
        // For synchronous scoring, we can't use LLM, so fall back to heuristic
        return CompositeScorer().scoreAll(segments, query)
    }
}

/**
 * Hybrid reranker that combines heuristic pre-filtering with LLM reranking.
 */
class HybridReranker(
    private val compositeScorer: CompositeScorer,
    private val llmReranker: LLMMetadataReranker,
    private val maxResults: Int = 20,
    private val llmCandidateCount: Int = 30
) : ScoringModel {
    
    override fun scoreAll(segments: List<TextSegment>, query: String): List<Double> {
        // Synchronous version uses only heuristic
        return compositeScorer.scoreAll(segments, query)
    }
    
    /**
     * Async reranking with LLM enhancement.
     */
    suspend fun rerankAsync(
        segments: List<TextSegment>,
        query: String,
        metadataProvider: (TextSegment, Double) -> DocumentMetadataItem
    ): RerankResult<TextSegment> {
        if (segments.isEmpty()) {
            return RerankResult(emptyList(), 0, false)
        }
        
        // Step 1: Heuristic scoring
        val heuristicScores = compositeScorer.scoreAll(segments, query)
        
        // Step 2: Pre-filter top candidates
        val candidates = segments.zip(heuristicScores)
            .sortedByDescending { it.second }
            .take(llmCandidateCount)
        
        // Step 3: Create metadata items with heuristic scores
        val metadataItems = candidates.mapIndexed { index, (segment, score) ->
            metadataProvider(segment, score)
        }
        
        // Step 4: LLM reranking
        val llmResult = llmReranker.rerank(metadataItems, query)
        
        // Step 5: Map back to segments
        val idToSegment = candidates.associate { (segment, _) ->
            (segment.id ?: segment.hashCode().toString()) to segment
        }
        
        val scoredItems = llmResult.rankedIds.mapNotNull { id ->
            val segment = idToSegment[id] ?: return@mapNotNull null
            val score = llmResult.scores[id] ?: 0.0
            ScoredItem(
                item = segment,
                score = score,
                source = if (llmResult.success) "hybrid_llm" else "hybrid_heuristic"
            )
        }
        
        return RerankResult(
            items = scoredItems.take(maxResults),
            totalCount = scoredItems.size,
            truncated = scoredItems.size > maxResults
        )
    }
}

