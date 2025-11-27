package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for DocQL structure queries
 */
class DocQLStructureTest {
    
    @BeforeTest
    fun setup() {
        // Clear the registry before each test
        DocumentRegistry.clearCache()
    }
    
    @AfterTest
    fun teardown() {
        DocumentRegistry.clearCache()
    }
    
    private fun createTestDocument(name: String, path: String): Pair<DocumentFile, DocumentParserService> {
        val doc = DocumentFile(
            name = name,
            path = path,
            metadata = DocumentMetadata(
                lastModified = 0,
                fileSize = 100,
                language = "markdown"
            )
        )
        val parser = object : DocumentParserService {
            override fun getDocumentContent(): String = "# $name"
            override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode = file
            override suspend fun queryHeading(keyword: String): List<DocumentChunk> = emptyList()
            override suspend fun queryChapter(chapterId: String): DocumentChunk? = null
        }
        return doc to parser
    }
    
    @Test
    fun `test structure query across multiple documents`() = runTest {
        // Register multiple test documents
        val docs = listOf(
            "src/main/kotlin/Service.kt",
            "src/main/kotlin/Repository.kt",
            "src/test/kotlin/ServiceTest.kt",
            "docs/README.md",
            "docs/api/endpoints.md"
        )
        
        for (path in docs) {
            val (doc, parser) = createTestDocument(path.substringAfterLast('/'), path)
            DocumentRegistry.registerDocument(path, doc, parser)
        }
        
        // Query structure
        val result = DocumentRegistry.queryDocuments("$.structure")
        
        assertIs<DocQLResult.Structure>(result)
        assertEquals(5, result.fileCount)
        assertEquals(5, result.paths.size)
        assertTrue(result.tree.isNotEmpty())
        
        // Check directory count - should have: src, src/main, src/main/kotlin, src/test, src/test/kotlin, docs, docs/api
        assertTrue(result.directoryCount >= 5)
    }
    
    @Test
    fun `test structure tree format`() = runTest {
        // Register documents in a known structure
        val docs = listOf(
            "src/main/App.kt",
            "src/main/Utils.kt",
            "src/test/AppTest.kt"
        )
        
        for (path in docs) {
            val (doc, parser) = createTestDocument(path.substringAfterLast('/'), path)
            DocumentRegistry.registerDocument(path, doc, parser)
        }
        
        val result = DocumentRegistry.queryDocuments("$.structure")
        
        assertIs<DocQLResult.Structure>(result)
        
        // Tree should contain directory and file names
        assertTrue(result.tree.contains("src"))
        assertTrue(result.tree.contains("main"))
        assertTrue(result.tree.contains("test"))
        assertTrue(result.tree.contains("App.kt"))
        assertTrue(result.tree.contains("Utils.kt"))
        assertTrue(result.tree.contains("AppTest.kt"))
    }
    
    @Test
    fun `test structure query with single file`() = runTest {
        val (doc, parser) = createTestDocument("README.md", "README.md")
        DocumentRegistry.registerDocument("README.md", doc, parser)
        
        val result = DocumentRegistry.queryDocuments("$.structure")
        
        assertIs<DocQLResult.Structure>(result)
        assertEquals(1, result.fileCount)
        assertEquals(0, result.directoryCount)  // No directories for root-level file
        assertTrue(result.tree.contains("README.md"))
    }
    
    @Test
    fun `test structure query with empty registry`() = runTest {
        val result = DocumentRegistry.queryDocuments("$.structure")
        
        assertIs<DocQLResult.Empty>(result)
    }
    
    @Test
    fun `test files query basic`() = runTest {
        val docs = listOf(
            "src/main/Service.kt",
            "src/main/Repository.kt",
            "docs/README.md"
        )
        
        for (path in docs) {
            val (doc, parser) = createTestDocument(path.substringAfterLast('/'), path)
            DocumentRegistry.registerDocument(path, doc, parser)
        }
        
        val result = DocumentRegistry.queryDocuments("$.files[*]")
        
        assertIs<DocQLResult.Files>(result)
        assertEquals(3, result.items.size)
    }
    
    @Test
    fun `test files query filter by extension`() = runTest {
        val docs = listOf(
            "src/main/Service.kt",
            "src/main/Utils.java",
            "docs/README.md"
        )
        
        for (path in docs) {
            val (doc, parser) = createTestDocument(path.substringAfterLast('/'), path)
            DocumentRegistry.registerDocument(path, doc, parser)
        }
        
        val result = DocumentRegistry.queryDocuments("""$.files[?(@.extension=="kt")]""")
        
        assertIs<DocQLResult.Files>(result)
        assertEquals(1, result.items.size)
        assertEquals("kt", result.items[0].extension)
    }
    
    @Test
    fun `test files query filter by path contains`() = runTest {
        val docs = listOf(
            "src/main/Service.kt",
            "src/test/ServiceTest.kt",
            "docs/README.md"
        )
        
        for (path in docs) {
            val (doc, parser) = createTestDocument(path.substringAfterLast('/'), path)
            DocumentRegistry.registerDocument(path, doc, parser)
        }
        
        val result = DocumentRegistry.queryDocuments("""$.files[?(@.path~="test")]""")
        
        assertIs<DocQLResult.Files>(result)
        assertEquals(1, result.items.size)
        assertTrue(result.items[0].path.contains("test"))
    }
    
    @Test
    fun `test files query get specific index`() = runTest {
        val docs = listOf(
            "a/first.kt",
            "b/second.kt",
            "c/third.kt"
        )
        
        for (path in docs) {
            val (doc, parser) = createTestDocument(path.substringAfterLast('/'), path)
            DocumentRegistry.registerDocument(path, doc, parser)
        }
        
        val result = DocumentRegistry.queryDocuments("$.files[0]")
        
        assertIs<DocQLResult.Files>(result)
        assertEquals(1, result.items.size)
    }
    
    @Test
    fun `test structure result formatting`() = runTest {
        val (doc, parser) = createTestDocument("Test.kt", "src/main/Test.kt")
        DocumentRegistry.registerDocument("src/main/Test.kt", doc, parser)
        
        val result = DocumentRegistry.queryDocuments("$.structure")
        
        assertIs<DocQLResult.Structure>(result)
        
        // Test the formatDocQLResult method
        val formatted = result.formatDocQLResult()
        assertTrue(formatted.contains("File Structure"))
        assertTrue(formatted.contains("directories"))
        assertTrue(formatted.contains("files"))
    }
}

