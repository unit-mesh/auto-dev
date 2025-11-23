package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for DocQL queries with position tracking
 * Validates that DocQL executor returns accurate position information
 */
class DocQLPositionQueryTest {
    
    @Test
    fun `should return position metadata for heading query`() = runTest {
        // Given
        val content = """
            # Introduction
            
            Welcome to the guide.
            
            # Getting Started
            
            First steps here.
        """.trimIndent()
        
        val file = createTestDocument(content)
        val parser = MarkdownDocumentParser()
        parser.parse(file, content)
        
        val executor = DocQLExecutor(file, parser)
        
        // When
        val result = executor.execute(parseDocQL("$.content.heading(\"Introduction\")"))
        
        // Then
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        assertEquals(1, chunks.size)
        
        val chunk = chunks.first()
        assertNotNull(chunk.position)
        assertEquals("/test/doc.md", chunk.position?.documentPath)
        assertEquals(DocumentFormatType.MARKDOWN, chunk.position?.formatType)
        
        val lineRange = chunk.position?.position as? DocumentPosition.LineRange
        assertNotNull(lineRange)
        assertEquals(0, lineRange.startLine)
    }
    
    @Test
    fun `should return positions for multiple matching headings`() = runTest {
        // Given
        val content = """
            # Chapter 1: Introduction
            
            Text here.
            
            # Chapter 2: Advanced Topics
            
            More text.
            
            # Chapter 3: Conclusion
            
            Final text.
        """.trimIndent()
        
        val file = createTestDocument(content)
        val parser = MarkdownDocumentParser()
        parser.parse(file, content)
        
        val executor = DocQLExecutor(file, parser)
        
        // When
        val result = executor.execute(parseDocQL("$.content.heading(\"Chapter\")"))
        
        // Then
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        assertEquals(3, chunks.size)
        
        // Verify all chunks have position metadata
        chunks.forEach { chunk ->
            assertNotNull(chunk.position, "Chunk '${chunk.chapterTitle}' should have position")
            assertTrue(chunk.position?.position is DocumentPosition.LineRange)
        }
        
        // Verify positions are in order
        val positions = chunks.map { it.position?.position as DocumentPosition.LineRange }
        for (i in 0 until positions.size - 1) {
            assertTrue(positions[i].startLine < positions[i + 1].startLine)
        }
    }
    
    @Test
    fun `should return position for chapter query`() = runTest {
        // Given
        val content = """
            # First Chapter
            
            Content of first chapter.
            
            # Second Chapter
            
            Content of second chapter.
        """.trimIndent()
        
        val file = createTestDocument(content)
        val parser = MarkdownDocumentParser()
        parser.parse(file, content)
        
        val executor = DocQLExecutor(file, parser)
        
        // When
        val result = executor.execute(parseDocQL("$.content.chapter(\"1\")"))
        
        // Then
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        assertEquals(1, chunks.size)
        
        val chunk = chunks.first()
        assertNotNull(chunk.position)
        assertNotNull(chunk.position?.position as? DocumentPosition.LineRange)
    }
    
    @Test
    fun `should return positions for grep query`() = runTest {
        // Given
        val content = """
            # Documentation
            
            This is about knowledge base systems.
            
            # Implementation
            
            The knowledge base uses embeddings.
            
            # Testing
            
            Test your knowledge base thoroughly.
        """.trimIndent()
        
        val file = createTestDocument(content)
        val parser = MarkdownDocumentParser()
        parser.parse(file, content)
        
        val executor = DocQLExecutor(file, parser)
        
        // When
        val result = executor.execute(parseDocQL("$.content.grep(\"knowledge base\")"))
        
        // Then
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        assertTrue(chunks.isNotEmpty())
        
        // All matching chunks should have position metadata
        chunks.forEach { chunk ->
            assertNotNull(chunk.position)
            assertTrue(chunk.content.contains("knowledge base", ignoreCase = true))
            
            val lineRange = chunk.position?.position as DocumentPosition.LineRange
            assertTrue(lineRange.endLine >= lineRange.startLine)
        }
    }
    
    // Note: TOC items from h2/h3 queries don't have position metadata in the current design
    // Position metadata is only in DocumentChunks, not TOCItems
    // This test is skipped as it's not testing position tracking
    
    @Test
    fun `should format position as location string`() = runTest {
        // Given
        val content = """
            # Test Heading
            Content line 1
            Content line 2
            Content line 3
        """.trimIndent()
        
        val file = createTestDocument(content)
        val parser = MarkdownDocumentParser()
        parser.parse(file, content)
        
        val executor = DocQLExecutor(file, parser)
        
        // When
        val result = executor.execute(parseDocQL("$.content.heading(\"Test\")"))
        
        // Then
        assertTrue(result is DocQLResult.Chunks)
        val chunk = (result as DocQLResult.Chunks).items.first()
        
        assertNotNull(chunk.position)
        val locationString = chunk.position?.toLocationString()
        assertNotNull(locationString)
        assertTrue(locationString.contains("/test/doc.md"))
        assertTrue(locationString.contains(":"))
    }
    
    @Test
    fun `should handle empty query result with no positions`() = runTest {
        // Given
        val content = """
            # Introduction
            
            Some content here.
        """.trimIndent()
        
        val file = createTestDocument(content)
        val parser = MarkdownDocumentParser()
        parser.parse(file, content)
        
        val executor = DocQLExecutor(file, parser)
        
        // When
        val result = executor.execute(parseDocQL("$.content.heading(\"NonExistent\")"))
        
        // Then
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        assertTrue(chunks.isEmpty())
    }
    
    @Test
    fun `should preserve position metadata through query chain`() = runTest {
        // Given
        val content = """
            # Chapter 1
            
            First section content with important keywords.
            
            ## Section 1.1
            
            Subsection with more keywords.
        """.trimIndent()
        
        val file = createTestDocument(content)
        val parser = MarkdownDocumentParser()
        parser.parse(file, content)
        
        val executor = DocQLExecutor(file, parser)
        
        // When
        val result = executor.execute(parseDocQL("$.content.heading(\"Section 1.1\")"))
        
        // Then
        assertTrue(result is DocQLResult.Chunks)
        val chunk = (result as DocQLResult.Chunks).items.first()
        
        // Position should be preserved
        assertNotNull(chunk.position)
        assertNotNull(chunk.startLine)
        assertNotNull(chunk.endLine)
        
        // Position metadata should match legacy fields
        val lineRange = chunk.position?.position as DocumentPosition.LineRange
        assertEquals(chunk.startLine, lineRange.startLine)
        assertEquals(chunk.endLine, lineRange.endLine)
    }
    
    private fun createTestDocument(content: String): DocumentFile {
        return DocumentFile(
            name = "doc.md",
            path = "/test/doc.md",
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = content.length.toLong()
            )
        )
    }
}
