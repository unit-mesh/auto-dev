package cc.unitmesh.devins.ui.compose.agent

import cc.unitmesh.agent.tool.impl.docql.DocQLSearchStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocQLStatsTest {

    @Test
    fun `test DocQLSearchStats toMetadata and fromMetadata roundtrip`() {
        // Create a DocQLSearchStats with all fields
        val original = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.SMART_SEARCH,
            query = "test query",
            documentPath = "/path/to/doc",
            channels = listOf("code", "docs"),
            documentsSearched = 10,
            totalRawResults = 50,
            resultsAfterRerank = 20,
            truncated = true,
            usedFallback = false,
            detailedResults = "Detailed results here",
            smartSummary = "Found 20: TestClass (class), testMethod (function), +18 more"
        )

        // Convert to metadata
        val metadata = original.toMetadata()

        // Print metadata for debugging
        println("Metadata keys: ${metadata.keys}")
        println("Metadata: $metadata")

        // Verify key fields are present
        assertNotNull(metadata["docql_search_type"], "docql_search_type should be present")
        assertEquals("SMART_SEARCH", metadata["docql_search_type"])
        assertEquals("test query", metadata["docql_query"])
        assertEquals("/path/to/doc", metadata["docql_document_path"])
        assertEquals("code,docs", metadata["docql_channels"])
        assertEquals("10", metadata["docql_docs_searched"])
        assertEquals("50", metadata["docql_raw_results"])
        assertEquals("20", metadata["docql_reranked_results"])
        assertEquals("true", metadata["docql_truncated"])
        assertEquals("false", metadata["docql_used_fallback"])
        assertEquals("Detailed results here", metadata["docql_detailed_results"])
        assertEquals("Found 20: TestClass (class), testMethod (function), +18 more", metadata["docql_smart_summary"])

        // Convert back from metadata
        val restored = DocQLSearchStats.fromMetadata(metadata)

        // Verify restored object
        assertNotNull(restored, "Restored DocQLSearchStats should not be null")
        assertEquals(original.searchType, restored.searchType)
        assertEquals(original.query, restored.query)
        assertEquals(original.documentPath, restored.documentPath)
        assertEquals(original.channels, restored.channels)
        assertEquals(original.documentsSearched, restored.documentsSearched)
        assertEquals(original.totalRawResults, restored.totalRawResults)
        assertEquals(original.resultsAfterRerank, restored.resultsAfterRerank)
        assertEquals(original.truncated, restored.truncated)
        assertEquals(original.usedFallback, restored.usedFallback)
        assertEquals(original.detailedResults, restored.detailedResults)
        assertEquals(original.smartSummary, restored.smartSummary)
    }

    @Test
    fun `test fromMetadata returns null for empty metadata`() {
        val result = DocQLSearchStats.fromMetadata(emptyMap())
        assertNull(result, "Should return null for empty metadata")
    }

    @Test
    fun `test fromMetadata returns null for metadata without search type`() {
        val metadata = mapOf(
            "docql_query" to "test",
            "docql_docs_searched" to "10"
        )
        val result = DocQLSearchStats.fromMetadata(metadata)
        assertNull(result, "Should return null when docql_search_type is missing")
    }

    @Test
    fun `test minimal DocQLSearchStats roundtrip`() {
        val original = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.DIRECT_QUERY,
            query = "simple query"
        )

        val metadata = original.toMetadata()
        println("Minimal metadata: $metadata")

        val restored = DocQLSearchStats.fromMetadata(metadata)
        assertNotNull(restored)
        assertEquals(DocQLSearchStats.SearchType.DIRECT_QUERY, restored.searchType)
        assertEquals("simple query", restored.query)
    }
}

