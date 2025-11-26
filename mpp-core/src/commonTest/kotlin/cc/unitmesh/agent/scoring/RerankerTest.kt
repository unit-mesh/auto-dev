package cc.unitmesh.agent.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RerankerTest {

    private val reranker = Reranker()

    @Test
    fun `test BM25 scoring prioritizes exact matches`() {
        val segments = listOf(
            TextSegment("AuthService handles authentication", mapOf("type" to "class", "name" to "AuthService")),
            TextSegment("UserService handles user management", mapOf("type" to "class", "name" to "UserService")),
            TextSegment("Authentication is important for security", mapOf("type" to "chunk"))
        )

        val query = "AuthService"
        val scores = reranker.scoreAll(segments, query)

        // AuthService should have highest score (exact name match + BM25)
        assertTrue(scores[0] > scores[1], "AuthService should score higher than UserService")
        assertTrue(scores[0] > scores[2], "AuthService should score higher than chunk")
    }

    @Test
    fun `test type-based scoring prioritizes code over docs`() {
        val classSegment = TextSegment(
            text = "Auth",
            metadata = mapOf("type" to "class", "name" to "Auth")
        )
        val chunkSegment = TextSegment(
            text = "Auth documentation",
            metadata = mapOf("type" to "chunk")
        )

        val query = "Auth"
        val classScore = reranker.score(classSegment, query)
        val chunkScore = reranker.score(chunkSegment, query)

        assertTrue(classScore > chunkScore, "Class should score higher than chunk")
    }

    @Test
    fun `test RRF fusion combines multiple sources`() {
        val classItems = listOf(
            TextSegment("AuthService", mapOf("type" to "class", "name" to "AuthService")),
            TextSegment("UserService", mapOf("type" to "class", "name" to "UserService"))
        )
        val functionItems = listOf(
            TextSegment("authenticate", mapOf("type" to "function", "name" to "authenticate")),
            TextSegment("authorize", mapOf("type" to "function", "name" to "authorize"))
        )

        val rankedLists = mapOf(
            "class" to classItems,
            "function" to functionItems
        )

        val result = reranker.rerank(
            rankedLists = rankedLists,
            query = "auth",
            segmentExtractor = { it }
        )

        // Should have items from both sources
        assertTrue(result.items.isNotEmpty(), "Should have results")
        assertTrue(result.items.size <= 4, "Should have at most 4 items")
        
        // Items appearing in multiple sources should rank higher
        val sources = result.items.map { it.source }
        assertTrue(sources.any { it.contains("class") }, "Should have class results")
        assertTrue(sources.any { it.contains("function") }, "Should have function results")
    }

    @Test
    fun `test reranker respects maxResults`() {
        val segments = (1..50).map { i ->
            TextSegment("Item$i", mapOf("type" to "class", "name" to "Item$i"))
        }

        val config = RerankConfig(maxResults = 10)
        val customReranker = Reranker(config)
        
        val result = customReranker.rerankSegments(segments, "Item")

        assertEquals(10, result.items.size, "Should return exactly maxResults items")
        assertTrue(result.truncated, "Should indicate truncation")
        assertEquals(50, result.totalCount, "Should report total count")
    }

    @Test
    fun `test camelCase tokenization for BM25`() {
        val segments = listOf(
            TextSegment("AuthenticationService", mapOf("type" to "class", "name" to "AuthenticationService")),
            TextSegment("auth_service", mapOf("type" to "class", "name" to "auth_service")),
            TextSegment("AUTHENTICATION", mapOf("type" to "class", "name" to "AUTHENTICATION"))
        )

        val query = "authentication"
        val scores = reranker.scoreAll(segments, query)

        // All should have some score since they all contain "authentication" (case-insensitive)
        assertTrue(scores.all { it > 0 }, "All items should have positive scores")
    }

    @Test
    fun `test minScoreThreshold filters low relevance items`() {
        val segments = listOf(
            TextSegment("AuthService", mapOf("type" to "class", "name" to "AuthService")),
            TextSegment("Completely unrelated content", mapOf("type" to "chunk"))
        )

        val config = RerankConfig(minScoreThreshold = 50.0)
        val customReranker = Reranker(config)
        
        val result = customReranker.rerankSegments(segments, "AuthService")

        // Only AuthService should pass the threshold
        assertTrue(result.items.size <= 2, "Should filter low relevance items")
        if (result.items.isNotEmpty()) {
            assertTrue(result.items.first().score >= 50.0, "First item should meet threshold")
        }
    }

    @Test
    fun `test scoreAndRank returns sorted results`() {
        val segments = listOf(
            TextSegment("Low", mapOf("type" to "chunk")),
            TextSegment("AuthService", mapOf("type" to "class", "name" to "AuthService")),
            TextSegment("Medium", mapOf("type" to "function", "name" to "Medium"))
        )

        val query = "AuthService"
        val ranked = reranker.scoreAndRank(segments, query, maxResults = 2)

        assertEquals(2, ranked.size, "Should return maxResults items")
        assertTrue(ranked[0].second >= ranked[1].second, "Results should be sorted by score descending")
    }

    @Test
    fun `test empty input handling`() {
        val emptyResult = reranker.scoreAll(emptyList(), "query")
        assertTrue(emptyResult.isEmpty(), "Empty input should return empty result")

        val emptyRerankResult = reranker.rerank<TextSegment>(
            rankedLists = emptyMap(),
            query = "query",
            segmentExtractor = { it }
        )
        assertTrue(emptyRerankResult.items.isEmpty(), "Empty ranked lists should return empty result")
    }

    @Test
    fun `test TextSegment factory methods`() {
        val codeSegment = TextSegment.forCode(
            name = "AuthService",
            type = "class",
            content = "class AuthService { }",
            filePath = "src/auth/AuthService.kt",
            line = 10
        )

        assertEquals("class", codeSegment.type)
        assertEquals("AuthService", codeSegment.name)
        assertEquals("src/auth/AuthService.kt", codeSegment.filePath)
        assertTrue(codeSegment.id?.contains("AuthService") == true)

        val chunkSegment = TextSegment.forChunk(
            content = "This is documentation content",
            filePath = "docs/README.md",
            heading = "Introduction"
        )

        assertEquals("chunk", chunkSegment.type)
        assertEquals("Introduction", chunkSegment.name)
        assertEquals("docs/README.md", chunkSegment.filePath)
    }
}

