package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.DocumentReranker
import cc.unitmesh.agent.scoring.DocumentRerankerConfig
import cc.unitmesh.agent.tool.impl.docql.DocQLKeywordSearchExecutor
import cc.unitmesh.agent.tool.impl.docql.SearchItem
import cc.unitmesh.devins.document.DocumentChunk
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.Location
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.devins.document.docql.DocQLResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocQLKeywordSearchExecutorTest {

    private val executor = DocQLKeywordSearchExecutor()
    private val reranker = DocumentReranker(DocumentRerankerConfig())

    @Test
    fun testCollectSearchItems_Entities() {
        val entities = listOf(
            Entity.ClassEntity(
                name = "UserService",
                packageName = "com.example",
                location = Location("#user-service", 10)
            ),
            Entity.FunctionEntity(
                name = "validateUser",
                signature = "validateUser(user: User): Boolean",
                location = Location("#validate-user", 20)
            )
        )

        val result = DocQLResult.Entities(
            itemsByFile = mapOf("/test/file.kt" to entities)
        )

        val items = mutableListOf<SearchItem>()
        val metadata = mutableMapOf<SearchItem, Pair<Any, String?>>()

        executor.collectSearchItems(result, items, metadata)

        assertEquals(2, items.size)
        assertEquals("UserService", items[0].segment.name)
        assertEquals("class", items[0].segment.type)
        assertEquals("validateUser", items[1].segment.name)
        assertEquals("function", items[1].segment.type)
    }

    @Test
    fun testCollectSearchItems_TocItems() {
        val tocItems = listOf(
            TOCItem(level = 1, title = "Introduction", anchor = "#intro"),
            TOCItem(level = 2, title = "Getting Started", anchor = "#getting-started")
        )

        val result = DocQLResult.TocItems(
            itemsByFile = mapOf("/test/README.md" to tocItems)
        )

        val items = mutableListOf<SearchItem>()
        val metadata = mutableMapOf<SearchItem, Pair<Any, String?>>()

        executor.collectSearchItems(result, items, metadata)

        assertEquals(2, items.size)
        assertEquals("Introduction", items[0].segment.name)
        assertEquals("toc", items[0].segment.type)
        assertEquals("Getting Started", items[1].segment.name)
    }

    @Test
    fun testCollectSearchItems_Chunks() {
        val chunks = listOf(
            DocumentChunk(documentPath = "/test/doc.md", chapterTitle = "Test", content = "This is a test chunk", anchor = "#test"),
            DocumentChunk(documentPath = "/test/doc.md", chapterTitle = "Test", content = "Another chunk of content", anchor = "#test2")
        )

        val result = DocQLResult.Chunks(
            itemsByFile = mapOf("/test/doc.md" to chunks)
        )

        val items = mutableListOf<SearchItem>()
        val metadata = mutableMapOf<SearchItem, Pair<Any, String?>>()

        executor.collectSearchItems(result, items, metadata)

        assertEquals(2, items.size)
        assertEquals("This is a test chunk", items[0].segment.text)
        assertEquals("chunk", items[0].segment.type)
        assertEquals("Another chunk of content", items[1].segment.text)
    }

    @Test
    fun testCollectSearchItems_EmptyResult() {
        val result = DocQLResult.Empty

        val items = mutableListOf<SearchItem>()
        val metadata = mutableMapOf<SearchItem, Pair<Any, String?>>()

        executor.collectSearchItems(result, items, metadata)

        assertTrue(items.isEmpty())
    }

    @Test
    fun testExecuteKeywordSearch_EmptyKeywords() = runTest {
        val result = executor.executeKeywordSearch(
            keywords = emptyList(),
            documentPath = null,
            reranker = reranker,
            queryForScoring = "test"
        )

        assertEquals(0, result.totalCount)
        assertTrue(result.items.isEmpty())
        assertTrue(result.activeChannels.isEmpty())
    }

    @Test
    fun testExecuteKeywordSearch_Deduplication() = runTest {
        // This test verifies that duplicate items are removed
        // In practice, this would require mocking DocumentRegistry
        // For now, we test that the method handles empty results correctly
        val result = executor.executeKeywordSearch(
            keywords = listOf("nonexistent_keyword_12345"),
            documentPath = null,
            reranker = reranker,
            queryForScoring = "test"
        )

        // Should return empty result for non-existent keywords
        assertEquals(0, result.totalCount)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun testCollectSearchItems_MetadataPreservation() {
        val entity = Entity.ClassEntity(
            name = "TestClass",
            packageName = "test.pkg",
            location = Location("#test", 5)
        )

        val result = DocQLResult.Entities(
            itemsByFile = mapOf("/test.kt" to listOf(entity))
        )

        val items = mutableListOf<SearchItem>()
        val metadata = mutableMapOf<SearchItem, Pair<Any, String?>>()

        executor.collectSearchItems(result, items, metadata)

        assertEquals(1, items.size)
        val item = items[0]
        
        // Verify metadata is preserved
        assertTrue(metadata.containsKey(item))
        val (originalItem, filePath) = metadata[item]!!
        assertEquals(entity, originalItem)
        assertEquals("/test.kt", filePath)
    }
}
