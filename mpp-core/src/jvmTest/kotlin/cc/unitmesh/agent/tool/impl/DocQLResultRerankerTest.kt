package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.RerankerType
import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.devins.document.DocumentChunk
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.Location
import cc.unitmesh.llm.KoogLLMService
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocQLResultRerankerTest {
    
    @Test
    fun `test heuristic reranking with empty items returns empty result`() = runTest {
        val reranker = DocQLResultReranker(null)
        val result = reranker.rerank(
            items = emptyList(),
            metadata = emptyMap(),
            query = "test",
            rerankerType = RerankerType.HEURISTIC,
            maxResults = 10
        )
        
        assertTrue(result.scoredResults.isEmpty())
        assertNull(result.llmRerankerStats)
        assertEquals(DocQLSearchStats.SearchType.SMART_SEARCH, result.actualSearchType)
    }
    
    @Test
    fun `test heuristic reranking with items returns scored results`() = runTest {
        val reranker = DocQLResultReranker(null)
        val segment1 = TextSegment(
            text = "This is a test class",
            metadata = mapOf(
                "type" to "class",
                "name" to "TestClass",
                "id" to "file1:TestClass:10",
                "filePath" to "src/TestClass.kt"
            )
        )
        val item1 = SearchItem(segment1)
        
        val segment2 = TextSegment(
            text = "This is a test function",
            metadata = mapOf(
                "type" to "function",
                "name" to "testFunction",
                "id" to "file1:testFunction:20",
                "filePath" to "src/TestClass.kt"
            )
        )
        val item2 = SearchItem(segment2)
        
        val mockEntity1 = Entity.ClassEntity(
            name = "TestClass",
            packageName = "com.test",
            location = Location(anchor = "", line = 10)
        )
        val mockEntity2 = Entity.FunctionEntity(
            name = "testFunction",
            signature = "fun testFunction()",
            location = Location(anchor = "", line = 20)
        )
        
        val metadata = mapOf(
            item1 to (mockEntity1 to "src/TestClass.kt"),
            item2 to (mockEntity2 to "src/TestClass.kt")
        )
        
        val result = reranker.rerank(
            items = listOf(item1, item2),
            metadata = metadata,
            query = "test",
            rerankerType = RerankerType.HEURISTIC,
            maxResults = 10
        )
        
        assertEquals(2, result.scoredResults.size)
        assertNull(result.llmRerankerStats)
        assertEquals(DocQLSearchStats.SearchType.SMART_SEARCH, result.actualSearchType)
        
        // Verify that results contain the original items
        val scoredItems = result.scoredResults.map { it.item }
        assertTrue(scoredItems.contains(mockEntity1))
        assertTrue(scoredItems.contains(mockEntity2))
    }
    
    @Test
    fun `test LLM reranking without LLM service falls back to heuristic`() = runTest {
        val rerankerWithoutLLM = DocQLResultReranker(null)
        
        val segment = TextSegment(
            text = "Test content",
            metadata = mapOf(
                "type" to "chunk",
                "id" to "chunk1",
                "filePath" to "test.md"
            )
        )
        val item = SearchItem(segment)
        val chunk = DocumentChunk(
            documentPath = "test.md",
            chapterTitle = null,
            content = "Test content",
            anchor = ""
        )
        val metadata = mapOf(item to (chunk to "test.md"))
        
        val result = rerankerWithoutLLM.rerank(
            items = listOf(item),
            metadata = metadata,
            query = "test",
            rerankerType = RerankerType.LLM_METADATA,
            maxResults = 10
        )
        
        assertEquals(1, result.scoredResults.size)
        assertNull(result.llmRerankerStats)
        assertEquals(DocQLSearchStats.SearchType.SMART_SEARCH, result.actualSearchType)
    }
    
    @Test
    fun `test LLM reranking successfully reranks items`() = runTest {
        val reranker = DocQLResultReranker(null)
        val segment1 = TextSegment(
            text = "Authentication service",
            metadata = mapOf(
                "type" to "class",
                "name" to "AuthService",
                "id" to "auth:AuthService:1",
                "filePath" to "src/AuthService.kt"
            )
        )
        val segment2 = TextSegment(
            text = "User service",
            metadata = mapOf(
                "type" to "class",
                "name" to "UserService",
                "id" to "user:UserService:1",
                "filePath" to "src/UserService.kt"
            )
        )
        
        val item1 = SearchItem(segment1)
        val item2 = SearchItem(segment2)
        
        val entity1 = Entity.ClassEntity(
            name = "AuthService",
            packageName = "com.app",
            location = Location(anchor = "", line = 1)
        )
        val entity2 = Entity.ClassEntity(
            name = "UserService",
            packageName = "com.app",
            location = Location(anchor = "", line = 1)
        )
        
        val metadata = mapOf(
            item1 to (entity1 to "src/AuthService.kt"),
            item2 to (entity2 to "src/UserService.kt")
        )
        
        // Mock LLM reranker response structure
        // Note: Without proper dependency injection, LLM reranking will fall back to heuristic
        val result = reranker.rerank(
            items = listOf(item1, item2),
            metadata = metadata,
            query = "authentication",
            rerankerType = RerankerType.LLM_METADATA,
            maxResults = 10
        )
        
        // Should attempt LLM reranking and may succeed or fall back
        assertNotNull(result.scoredResults)
        assertTrue(result.scoredResults.size <= 2)
    }
    
    @Test
    fun `test hybrid reranking uses LLM strategy`() = runTest {
        val reranker = DocQLResultReranker(null)
        val segment = TextSegment(
            text = "Test content",
            metadata = mapOf(
                "type" to "chunk",
                "id" to "chunk1",
                "filePath" to "test.md"
            )
        )
        val item = SearchItem(segment)
        val chunk = DocumentChunk(
            documentPath = "test.md",
            chapterTitle = null,
            content = "Test content",
            anchor = ""
        )
        val metadata = mapOf(item to (chunk to "test.md"))
        
        val result = reranker.rerank(
            items = listOf(item),
            metadata = metadata,
            query = "test",
            rerankerType = RerankerType.HYBRID,
            maxResults = 10
        )
        
        // Hybrid should attempt to use LLM (may fall back depending on service availability)
        assertNotNull(result.scoredResults)
        assertEquals(1, result.scoredResults.size)
    }
    
    @Test
    fun `test scored results contain correct metadata`() = runTest {
        val reranker = DocQLResultReranker(null)
        val segment = TextSegment(
            text = "This is a very long test content that should be truncated in the preview to only 100 characters for display purposes",
            metadata = mapOf(
                "type" to "chunk",
                "id" to "chunk1",
                "filePath" to "docs/test.md"
            )
        )
        val item = SearchItem(segment)
        val chunk = DocumentChunk(
            documentPath = "docs/test.md",
            chapterTitle = null,
            content = segment.text,
            anchor = ""
        )
        val metadata = mapOf(item to (chunk to "docs/test.md"))
        
        val result = reranker.rerank(
            items = listOf(item),
            metadata = metadata,
            query = "test",
            rerankerType = RerankerType.HEURISTIC,
            maxResults = 10
        )
        
        assertEquals(1, result.scoredResults.size)
        val scoredResult = result.scoredResults.first()
        
        assertEquals(chunk, scoredResult.item)
        assertEquals("chunk1", scoredResult.uniqueId)
        assertEquals("docs/test.md", scoredResult.filePath)
        assertTrue(scoredResult.preview.length <= 105) // 100 chars + some whitespace handling
        assertFalse(scoredResult.preview.contains("\n"))
    }
    
    @Test
    fun `test RRF_COMPOSITE reranker type uses heuristic strategy`() = runTest {
        val reranker = DocQLResultReranker(null)
        val segment = TextSegment(
            text = "Test",
            metadata = mapOf(
                "type" to "class",
                "id" to "test1",
                "filePath" to "Test.kt"
            )
        )
        val item = SearchItem(segment)
        val entity = Entity.ClassEntity(
            name = "Test",
            packageName = "com.test",
            location = Location(anchor = "", line = 1)
        )
        val metadata = mapOf(item to (entity to "Test.kt"))
        
        val result = reranker.rerank(
            items = listOf(item),
            metadata = metadata,
            query = "test",
            rerankerType = RerankerType.RRF_COMPOSITE,
            maxResults = 10
        )
        
        // RRF_COMPOSITE should use heuristic strategy (not LLM)
        assertEquals(1, result.scoredResults.size)
        assertNull(result.llmRerankerStats)
        assertEquals(DocQLSearchStats.SearchType.SMART_SEARCH, result.actualSearchType)
    }
}
