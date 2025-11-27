package cc.unitmesh.devins.ui.compose.agent

import cc.unitmesh.agent.tool.impl.docql.DocQLSearchStats
import cc.unitmesh.agent.tool.ToolResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test that DocQL tool correctly produces metadata that can be parsed by DocQLSearchStats.fromMetadata()
 */
class DocQLMetadataFlowTest {

    @Test
    fun `test DocQLSearchStats toMetadata produces correct keys`() {
        val stats = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.SMART_SEARCH,
            query = "authentication",
            documentPath = "/docs/auth.md",
            channels = listOf("code", "docs"),
            documentsSearched = 15,
            totalRawResults = 100,
            resultsAfterRerank = 25,
            truncated = true,
            usedFallback = false,
            detailedResults = "Detailed results...",
            smartSummary = "Found 25: AuthService (class), login (function), +23 more"
        )

        val metadata = stats.toMetadata()

        // Verify all required keys are present
        assertTrue(metadata.containsKey("docql_search_type"), "Should have docql_search_type")
        assertEquals("SMART_SEARCH", metadata["docql_search_type"])

        assertTrue(metadata.containsKey("docql_query"), "Should have docql_query")
        assertEquals("authentication", metadata["docql_query"])

        assertTrue(metadata.containsKey("docql_document_path"), "Should have docql_document_path")
        assertEquals("/docs/auth.md", metadata["docql_document_path"])

        assertTrue(metadata.containsKey("docql_channels"), "Should have docql_channels")
        assertEquals("code,docs", metadata["docql_channels"])

        assertTrue(metadata.containsKey("docql_docs_searched"), "Should have docql_docs_searched")
        assertEquals("15", metadata["docql_docs_searched"])

        assertTrue(metadata.containsKey("docql_raw_results"), "Should have docql_raw_results")
        assertEquals("100", metadata["docql_raw_results"])

        assertTrue(metadata.containsKey("docql_reranked_results"), "Should have docql_reranked_results")
        assertEquals("25", metadata["docql_reranked_results"])

        assertTrue(metadata.containsKey("docql_truncated"), "Should have docql_truncated")
        assertEquals("true", metadata["docql_truncated"])

        assertTrue(metadata.containsKey("docql_used_fallback"), "Should have docql_used_fallback")
        assertEquals("false", metadata["docql_used_fallback"])

        assertTrue(metadata.containsKey("docql_detailed_results"), "Should have docql_detailed_results")
        assertEquals("Detailed results...", metadata["docql_detailed_results"])

        assertTrue(metadata.containsKey("docql_smart_summary"), "Should have docql_smart_summary")
        assertEquals("Found 25: AuthService (class), login (function), +23 more", metadata["docql_smart_summary"])
    }

    @Test
    fun `test ToolResult Success correctly stores metadata`() {
        val stats = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.SMART_SEARCH,
            query = "test query",
            documentsSearched = 10,
            totalRawResults = 50,
            resultsAfterRerank = 20,
            smartSummary = "Found 20 results",
            documentPath = "",
            channels = emptyList(),
            truncated = false,
            usedFallback = false,
            rerankerConfig = null,
            scoringInfo = null,
            keywordExpansion = null,
            llmRerankerInfo = null,
            detailedResults = "",
            fullResults = null,
        )

        val toolResult = ToolResult.Success("Found 20 results", stats.toMetadata())

        // Verify metadata is accessible
        val extractedMetadata = toolResult.extractMetadata()
        assertNotNull(extractedMetadata["docql_search_type"])
        assertEquals("SMART_SEARCH", extractedMetadata["docql_search_type"])
        assertEquals("test query", extractedMetadata["docql_query"])
    }

    @Test
    fun `test fromMetadata correctly parses metadata from ToolResult`() {
        val originalStats = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.SMART_SEARCH,
            query = "authentication",
            documentPath = "/docs/auth.md",
            channels = listOf("code", "docs"),
            documentsSearched = 15,
            totalRawResults = 100,
            resultsAfterRerank = 25,
            truncated = true,
            usedFallback = false,
            detailedResults = "Detailed results...",
            smartSummary = "Found 25: AuthService (class), login (function), +23 more"
        )

        // Simulate what DocQLTool does
        val toolResult = ToolResult.Success(originalStats.smartSummary ?: "", originalStats.toMetadata())

        // Simulate what ToolOrchestrator does
        val metadata = toolResult.extractMetadata()

        // Simulate what ComposeRenderer.renderToolResult does
        val parsedStats = DocQLSearchStats.fromMetadata(metadata)

        // Verify parsing succeeded
        assertNotNull(parsedStats, "fromMetadata should return non-null stats")
        assertEquals(originalStats.searchType, parsedStats.searchType)
        assertEquals(originalStats.query, parsedStats.query)
        assertEquals(originalStats.documentPath, parsedStats.documentPath)
        assertEquals(originalStats.channels, parsedStats.channels)
        assertEquals(originalStats.documentsSearched, parsedStats.documentsSearched)
        assertEquals(originalStats.totalRawResults, parsedStats.totalRawResults)
        assertEquals(originalStats.resultsAfterRerank, parsedStats.resultsAfterRerank)
        assertEquals(originalStats.truncated, parsedStats.truncated)
        assertEquals(originalStats.usedFallback, parsedStats.usedFallback)
        assertEquals(originalStats.detailedResults, parsedStats.detailedResults)
        assertEquals(originalStats.smartSummary, parsedStats.smartSummary)
    }

    @Test
    fun `test fromMetadata returns null for non-DocQL metadata`() {
        val metadata = mapOf(
            "some_key" to "some_value",
            "another_key" to "another_value"
        )

        val stats = DocQLSearchStats.fromMetadata(metadata)

        // Should return null because docql_search_type is missing
        assertEquals(null, stats)
    }
}

