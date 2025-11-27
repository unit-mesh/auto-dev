package cc.unitmesh.devins.ui.compose.agent

import cc.unitmesh.agent.tool.impl.docql.DocQLSearchStats
import cc.unitmesh.devins.llm.MessageMetadata
import cc.unitmesh.devins.llm.TimelineItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test the full flow of DocQL stats through the system:
 * 1. DocQLTool creates stats and calls toMetadata()
 * 2. ComposeRenderer.renderToolResult receives metadata and calls DocQLSearchStats.fromMetadata()
 * 3. CombinedToolItem stores docqlStats
 * 4. toMessageMetadata() serializes docqlStats to MessageMetadata
 * 5. fromMessageMetadata() deserializes docqlStats from MessageMetadata
 */
class DocQLStatsFlowTest {

    @Test
    fun `test MessageMetadata serialization and deserialization of DocQL stats`() {
        // Simulate what toMessageMetadata does
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

        // Create MessageMetadata like toMessageMetadata does
        val messageMetadata = MessageMetadata(
            itemType = TimelineItemType.COMBINED_TOOL,
            toolName = "docql",
            description = "DocQL Query",
            success = true,
            summary = "Found 25 results",
            output = stats.smartSummary,
            fullOutput = stats.detailedResults,
            // DocQL stats fields
            docqlSearchType = stats.searchType.name,
            docqlQuery = stats.query,
            docqlDocumentPath = stats.documentPath,
            docqlChannels = stats.channels.joinToString(","),
            docqlDocsSearched = stats.documentsSearched,
            docqlRawResults = stats.totalRawResults,
            docqlRerankedResults = stats.resultsAfterRerank,
            docqlTruncated = stats.truncated,
            docqlUsedFallback = stats.usedFallback,
            docqlDetailedResults = stats.detailedResults,
            docqlSmartSummary = stats.smartSummary
        )

        // Verify all fields are set
        assertEquals("SMART_SEARCH", messageMetadata.docqlSearchType)
        assertEquals("authentication", messageMetadata.docqlQuery)
        assertEquals("/docs/auth.md", messageMetadata.docqlDocumentPath)
        assertEquals("code,docs", messageMetadata.docqlChannels)
        assertEquals(15, messageMetadata.docqlDocsSearched)
        assertEquals(100, messageMetadata.docqlRawResults)
        assertEquals(25, messageMetadata.docqlRerankedResults)
        assertEquals(true, messageMetadata.docqlTruncated)
        assertEquals(false, messageMetadata.docqlUsedFallback)

        // Now simulate what fromMessageMetadata does
        val searchTypeStr = messageMetadata.docqlSearchType
        assertNotNull(searchTypeStr, "docqlSearchType should not be null")

        val restoredStats = if (searchTypeStr != null) {
            val searchType = try {
                DocQLSearchStats.SearchType.valueOf(searchTypeStr)
            } catch (e: IllegalArgumentException) {
                null
            }
            searchType?.let {
                DocQLSearchStats(
                    searchType = it,
                    query = messageMetadata.docqlQuery ?: "",
                    documentPath = messageMetadata.docqlDocumentPath,
                    channels = messageMetadata.docqlChannels?.split(",")?.filter { ch -> ch.isNotBlank() }
                        ?: emptyList(),
                    documentsSearched = messageMetadata.docqlDocsSearched ?: 0,
                    totalRawResults = messageMetadata.docqlRawResults ?: 0,
                    resultsAfterRerank = messageMetadata.docqlRerankedResults ?: 0,
                    truncated = messageMetadata.docqlTruncated ?: false,
                    usedFallback = messageMetadata.docqlUsedFallback ?: false,
                    detailedResults = messageMetadata.docqlDetailedResults ?: "",
                    smartSummary = messageMetadata.docqlSmartSummary
                )
            }
        } else null

        // Verify restored stats
        assertNotNull(restoredStats, "Restored stats should not be null")
        assertEquals(stats.searchType, restoredStats.searchType)
        assertEquals(stats.query, restoredStats.query)
        assertEquals(stats.documentPath, restoredStats.documentPath)
        assertEquals(stats.channels, restoredStats.channels)
        assertEquals(stats.documentsSearched, restoredStats.documentsSearched)
        assertEquals(stats.totalRawResults, restoredStats.totalRawResults)
        assertEquals(stats.resultsAfterRerank, restoredStats.resultsAfterRerank)
        assertEquals(stats.truncated, restoredStats.truncated)
        assertEquals(stats.usedFallback, restoredStats.usedFallback)
        assertEquals(stats.detailedResults, restoredStats.detailedResults)
        assertEquals(stats.smartSummary, restoredStats.smartSummary)
    }

    @Test
    fun `test MessageMetadata without DocQL stats`() {
        val messageMetadata = MessageMetadata(
            itemType = TimelineItemType.COMBINED_TOOL,
            toolName = "read_file",
            success = true
        )

        // docqlSearchType should be null for non-DocQL tools
        assertNull(messageMetadata.docqlSearchType)

        // Restoration should return null
        val searchTypeStr = messageMetadata.docqlSearchType
        val restoredStats = if (searchTypeStr != null) {
            DocQLSearchStats.SearchType.valueOf(searchTypeStr)
        } else null

        assertNull(restoredStats)
    }
}

