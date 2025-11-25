package cc.unitmesh.devins.document

import cc.unitmesh.devins.document.docql.DocQLResult
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class DocumentRegistryMultiFileQueryTest {
    
    @BeforeTest
    fun setup() {
        DocumentRegistry.clearCache()
    }
    
    @AfterTest
    fun cleanup() {
        DocumentRegistry.clearCache()
    }
    
    @Test
    fun `test query multiple documents returns results grouped by file`() = runTest {
        // Setup: Register multiple documents with TOC
        val doc1 = DocumentFile(
            name = "doc1.md",
            path = "docs/doc1.md",
            metadata = DocumentMetadata(
                lastModified = 0,
                fileSize = 1000
            ),
            toc = listOf(
                TOCItem(1, "Introduction", "#intro"),
                TOCItem(1, "Getting Started", "#start")
            )
        )
        
        val doc2 = DocumentFile(
            name = "doc2.md",
            path = "docs/doc2.md",
            metadata = DocumentMetadata(
                lastModified = 0,
                fileSize = 1000
            ),
            toc = listOf(
                TOCItem(1, "Introduction", "#intro"),
                TOCItem(1, "Advanced Topics", "#advanced")
            )
        )
        
        val parser = MarkdownDocumentParser()
        DocumentRegistry.registerDocument("docs/doc1.md", doc1, parser)
        DocumentRegistry.registerDocument("docs/doc2.md", doc2, parser)
        
        // Test: Query all documents
        val result = DocumentRegistry.queryDocuments("$.toc[*]")
        
        println("Multi-file query result: $result")
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(2, result.itemsByFile.size, "Should have results from 2 files")
        assertEquals(4, result.totalCount, "Should have 4 total TOC items")
        
        assertTrue(result.itemsByFile.containsKey("docs/doc1.md"))
        assertTrue(result.itemsByFile.containsKey("docs/doc2.md"))
        
        assertEquals(2, result.itemsByFile["docs/doc1.md"]?.size)
        assertEquals(2, result.itemsByFile["docs/doc2.md"]?.size)
    }
    
    @Test
    fun `test query with heading search across multiple files`() = runTest {
        // Setup: Create parsers with queryHeading support
        val doc1Content = """
            # Introduction
            This is doc1 introduction.
            
            # Features
            Doc1 features here.
        """.trimIndent()
        
        val doc2Content = """
            # Introduction
            This is doc2 introduction.
            
            # Setup
            Doc2 setup here.
        """.trimIndent()
        
        val parser1 = MarkdownDocumentParser()
        val doc1 = parser1.parse(
            DocumentFile("doc1.md", "docs/doc1.md", 
                DocumentMetadata(lastModified = 0, fileSize = doc1Content.length.toLong())),
            doc1Content
        ) as DocumentFile
        
        val parser2 = MarkdownDocumentParser()
        val doc2 = parser2.parse(
            DocumentFile("doc2.md", "docs/doc2.md",
                DocumentMetadata(lastModified = 0, fileSize = doc2Content.length.toLong())),
            doc2Content
        ) as DocumentFile
        
        DocumentRegistry.registerDocument("docs/doc1.md", doc1, parser1)
        DocumentRegistry.registerDocument("docs/doc2.md", doc2, parser2)
        
        // Test: Query heading across all files
        val result = DocumentRegistry.queryDocuments("$.content.heading(\"Introduction\")")
        
        println("Heading query result: $result")
        
        assertIs<DocQLResult.Chunks>(result)
        assertEquals(2, result.itemsByFile.size, "Should find 'Introduction' in both files")
        assertTrue(result.totalCount >= 2, "Should have at least 2 chunks")
    }
    
    @Test
    fun `test files query returns all available files`() = runTest {
        // Setup: Register multiple documents
        for (i in 1..5) {
            val doc = DocumentFile(
                name = "doc$i.md",
                path = "docs/section${i % 2}/doc$i.md",
                metadata = DocumentMetadata(lastModified = 0, fileSize = 1000)
            )
            DocumentRegistry.registerDocument(doc.path, doc, MarkdownDocumentParser())
        }
        
        // Test: Query all files
        val result = DocumentRegistry.queryDocuments("$.files[*]")
        
        println("Files query result: $result")
        
        assertIs<DocQLResult.Files>(result)
        assertEquals(5, result.items.size, "Should list all 5 files")
        
        // Verify file info includes path, name, directory
        result.items.forEach { file ->
            assertTrue(file.path.isNotEmpty())
            assertTrue(file.name.isNotEmpty())
            assertTrue(file.name.endsWith(".md"))
        }
    }
    
    @Test
    fun `test files query with path filter`() = runTest {
        // Setup: Register documents in different directories
        val docs = listOf(
            "docs/api/rest.md",
            "docs/api/graphql.md",
            "docs/guides/quickstart.md",
            "docs/guides/advanced.md",
            "src/main.kt"
        )
        
        docs.forEach { path ->
            val doc = DocumentFile(
                name = path.substringAfterLast('/'),
                path = path,
                metadata = DocumentMetadata(lastModified = 0, fileSize = 1000)
            )
            DocumentRegistry.registerDocument(path, doc, MarkdownDocumentParser())
        }
        
        // Test: Filter by directory
        val result = DocumentRegistry.queryDocuments("$.files[?(@.path contains \"api\")]")
        
        println("Filtered files result: $result")
        
        assertIs<DocQLResult.Files>(result)
        assertEquals(2, result.items.size, "Should find only files in 'api' directory")
        
        result.items.forEach { file ->
            assertTrue(file.path.contains("api"), "All results should contain 'api'")
        }
    }
    
    @Test
    fun `test files query with extension filter`() = runTest {
        // Setup: Register documents with different extensions
        val docs = listOf(
            "docs/readme.md",
            "docs/guide.md",
            "src/main.kt",
            "src/utils.kt",
            "config.yaml"
        )
        
        docs.forEach { path ->
            val doc = DocumentFile(
                name = path.substringAfterLast('/'),
                path = path,
                metadata = DocumentMetadata(lastModified = 0, fileSize = 1000)
            )
            DocumentRegistry.registerDocument(path, doc, MarkdownDocumentParser())
        }
        
        // Test: Filter by extension (using path contains since extension filter is path-based)
        val result = DocumentRegistry.queryDocuments("$.files[?(@.path contains \".kt\")]")
        
        assertIs<DocQLResult.Files>(result)
        assertEquals(2, result.items.size, "Should find only .kt files")
        
        result.items.forEach { file ->
            assertTrue(file.path.endsWith(".kt"), "All results should be .kt files")
        }
    }
    
    @Test
    fun `test empty query returns empty result`() = runTest {
        // Setup: Register documents without matching content
        val doc = DocumentFile(
            name = "doc.md",
            path = "docs/doc.md",
            metadata = DocumentMetadata(lastModified = 0, fileSize = 1000),
            toc = listOf(TOCItem(1, "Setup", "#setup"))
        )
        
        DocumentRegistry.registerDocument("docs/doc.md", doc, MarkdownDocumentParser())
        
        // Test: Query for non-existent heading
        val result = DocumentRegistry.queryDocuments("$.toc[?(@.title contains \"NonExistent\")]")
        
        println("Empty query result: $result")
        
        // Should return Empty or TocItems with empty map
        assertTrue(
            result is DocQLResult.Empty || 
            (result is DocQLResult.TocItems && result.totalCount == 0),
            "Should return empty result"
        )
    }
    
    @Test
    fun `test query specific files subset`() = runTest {
        // Setup: Register multiple documents
        val docs = listOf("docs/a.md", "docs/b.md", "docs/c.md")
        docs.forEach { path ->
            val doc = DocumentFile(
                name = path.substringAfterLast('/'),
                path = path,
                metadata = DocumentMetadata(lastModified = 0, fileSize = 1000),
                toc = listOf(TOCItem(1, "Title", "#title"))
            )
            DocumentRegistry.registerDocument(path, doc, MarkdownDocumentParser())
        }
        
        // Test: Query only specific files
        val result = DocumentRegistry.queryDocuments(
            "$.toc[*]",
            documentPaths = listOf("docs/a.md", "docs/c.md")
        )
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(2, result.itemsByFile.size, "Should only query specified files")
        assertTrue(result.itemsByFile.containsKey("docs/a.md"))
        assertTrue(result.itemsByFile.containsKey("docs/c.md"))
        assertFalse(result.itemsByFile.containsKey("docs/b.md"))
    }
    
    @Test
    fun `test totalCount property calculation`() = runTest {
        // Setup: Register documents with different numbers of items
        val doc1 = DocumentFile(
            name = "doc1.md",
            path = "docs/doc1.md",
            metadata = DocumentMetadata(lastModified = 0, fileSize = 1000),
            toc = listOf(
                TOCItem(1, "A", "#a"),
                TOCItem(1, "B", "#b"),
                TOCItem(1, "C", "#c")
            )
        )
        
        val doc2 = DocumentFile(
            name = "doc2.md",
            path = "docs/doc2.md",
            metadata = DocumentMetadata(lastModified = 0, fileSize = 1000),
            toc = listOf(
                TOCItem(1, "X", "#x"),
                TOCItem(1, "Y", "#y")
            )
        )
        
        DocumentRegistry.registerDocument("docs/doc1.md", doc1, MarkdownDocumentParser())
        DocumentRegistry.registerDocument("docs/doc2.md", doc2, MarkdownDocumentParser())
        
        // Test: Verify totalCount
        val result = DocumentRegistry.queryDocuments("$.toc[*]")
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(5, result.totalCount, "totalCount should sum all items from all files")
        assertEquals(3, result.itemsByFile["docs/doc1.md"]?.size)
        assertEquals(2, result.itemsByFile["docs/doc2.md"]?.size)
    }
    
    @Test
    fun `test compressed path summary triggers at threshold`() = runTest {
        DocumentRegistry.clearCache()
        
        // Test with small number (below threshold)
        for (i in 1..10) {
            val doc = DocumentFile(
                name = "doc$i.md",
                path = "docs/doc$i.md",
                metadata = DocumentMetadata(lastModified = 0, fileSize = 1000)
            )
            DocumentRegistry.registerDocument(doc.path, doc, MarkdownDocumentParser())
        }
        
        val smallSummary = DocumentRegistry.getCompressedPathsSummary(threshold = 20)
        println("\nSmall summary (10 files):\n$smallSummary\n")
        
        assertTrue(smallSummary.contains("Available documents (10)"), 
            "Should show simple list for small number")
        assertFalse(smallSummary.contains("directory structure"), 
            "Should not show tree structure for small number")
        
        // Add more files to exceed threshold
        for (i in 11..25) {
            val doc = DocumentFile(
                name = "doc$i.md",
                path = "docs/section${i % 3}/doc$i.md",
                metadata = DocumentMetadata(lastModified = 0, fileSize = 1000)
            )
            DocumentRegistry.registerDocument(doc.path, doc, MarkdownDocumentParser())
        }
        
        val largeSummary = DocumentRegistry.getCompressedPathsSummary(threshold = 20)
        println("\nLarge summary (25 files):\n$largeSummary\n")
        
        assertTrue(largeSummary.contains("25 total"), 
            "Should show total count")
        assertTrue(largeSummary.contains("directory structure"), 
            "Should show tree structure for large number")
        assertTrue(largeSummary.contains("$.files[*]"), 
            "Should suggest files query")
        assertTrue(largeSummary.contains("💡 Tip:"), 
            "Should provide helpful tip")
    }
}

