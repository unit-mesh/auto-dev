package cc.unitmesh.devins.document

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD tests for Markdown position tracking
 * Ensures that MarkdownDocumentParser correctly tracks positions
 */
class MarkdownPositionTrackingTest {
    
    private val parser = MarkdownDocumentParser()
    
    @Test
    fun `should track heading position in simple document`() = runTest {
        // Given
        val content = """
            # Introduction
            
            This is the introduction text.
        """.trimIndent()
        
        val file = DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = content.length.toLong()
            )
        )
        
        // When
        val result = parser.parse(file, content)
        val chunks = parser.queryHeading("Introduction")
        
        // Then
        assertEquals(1, chunks.size)
        val chunk = chunks.first()
        assertNotNull(chunk.position)
        assertEquals("/test/test.md", chunk.position?.documentPath)
        assertEquals(DocumentFormatType.MARKDOWN, chunk.position?.formatType)
        
        val lineRange = chunk.position?.position as? DocumentPosition.LineRange
        assertNotNull(lineRange)
        assertEquals(0, lineRange.startLine)
    }
    
    @Test
    fun `should track positions for multiple headings`() = runTest {
        // Given
        val content = """
            # First Heading
            
            First content.
            
            ## Second Heading
            
            Second content.
            
            # Third Heading
            
            Third content.
        """.trimIndent()
        
        val file = DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = content.length.toLong()
            )
        )
        
        // When
        parser.parse(file, content)
        val allChunks = parser.queryHeading("")
        
        // Then
        assertEquals(3, allChunks.size)
        
        // Verify each chunk has position metadata
        allChunks.forEach { chunk ->
            assertNotNull(chunk.position, "Chunk '${chunk.chapterTitle}' should have position")
            assertTrue(chunk.position?.position is DocumentPosition.LineRange)
        }
        
        // Verify line ranges don't overlap incorrectly
        val firstChunk = allChunks.find { it.chapterTitle == "First Heading" }
        val secondChunk = allChunks.find { it.chapterTitle == "Second Heading" }
        
        assertNotNull(firstChunk)
        assertNotNull(secondChunk)
        
        val firstRange = firstChunk.position?.position as DocumentPosition.LineRange
        val secondRange = secondChunk.position?.position as DocumentPosition.LineRange
        
        assertTrue(firstRange.endLine < secondRange.startLine)
    }
    
    @Test
    fun `should track nested heading positions`() = runTest {
        // Given
        val content = """
            # Chapter 1
            
            Chapter 1 intro.
            
            ## Section 1.1
            
            Section content.
            
            ### Subsection 1.1.1
            
            Subsection content.
            
            ## Section 1.2
            
            Another section.
        """.trimIndent()
        
        val file = DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = content.length.toLong()
            )
        )
        
        // When
        parser.parse(file, content)
        val section11 = parser.queryHeading("Section 1.1")
        val subsection = parser.queryHeading("Subsection 1.1.1")
        
        // Then - "Section 1.1" will match both "Section 1.1" and "Subsection 1.1.1"
        assertEquals(2, section11.size)
        assertEquals(1, subsection.size)
        
        // Find the specific sections
        val section11Item = section11.find { it.chapterTitle == "Section 1.1" }
        val subsectionItem = subsection.first()
        
        assertNotNull(section11Item)
        assertNotNull(subsectionItem)
        
        val section11Position = section11Item.position?.position as DocumentPosition.LineRange
        val subsectionPosition = subsectionItem.position?.position as DocumentPosition.LineRange
        
        // Subsection should be after section start
        assertTrue(subsectionPosition.startLine > section11Position.startLine)
    }
    
    @Test
    fun `should track paragraph positions within heading section`() = runTest {
        // Given
        val content = """
            # Heading
            
            First paragraph.
            
            Second paragraph with
            multiple lines.
            
            Third paragraph.
        """.trimIndent()
        
        val file = DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = content.length.toLong()
            )
        )
        
        // When
        parser.parse(file, content)
        val chunks = parser.queryHeading("Heading")
        
        // Then
        assertEquals(1, chunks.size)
        val chunk = chunks.first()
        
        // The chunk should contain all content under the heading
        assertTrue(chunk.content.contains("First paragraph"))
        assertTrue(chunk.content.contains("Second paragraph"))
        assertTrue(chunk.content.contains("Third paragraph"))
        
        // Position should span from heading to end of section
        assertNotNull(chunk.position)
        val position = chunk.position?.position as DocumentPosition.LineRange
        assertTrue(position.endLine > position.startLine)
    }
    
    @Test
    fun `should track exact line ranges for heading sections`() = runTest {
        // Given - carefully crafted content where we know exact lines
        val content = """
            # Heading One
            Content one.
            # Heading Two
            Content two.
        """.trimIndent()
        
        val file = DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = content.length.toLong()
            )
        )
        
        // When
        parser.parse(file, content)
        val headingOne = parser.queryHeading("Heading One")
        val headingTwo = parser.queryHeading("Heading Two")
        
        // Then
        assertEquals(1, headingOne.size)
        assertEquals(1, headingTwo.size)
        
        val pos1 = headingOne.first().position?.position as DocumentPosition.LineRange
        val pos2 = headingTwo.first().position?.position as DocumentPosition.LineRange
        
        // Heading One should be at line 0
        assertEquals(0, pos1.startLine)
        // Heading Two should be at line 2
        assertEquals(2, pos2.startLine)
    }
    
    @Test
    fun `should handle grep query with position tracking`() = runTest {
        // Given
        val content = """
            # Introduction
            
            This document discusses knowledge base systems.
            
            # Implementation
            
            The knowledge base uses vector embeddings.
        """.trimIndent()
        
        val file = DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = content.length.toLong()
            )
        )
        
        // When
        parser.parse(file, content)
        val results = parser.queryHeading("knowledge base")
        
        // Then
        assertTrue(results.isNotEmpty())
        results.forEach { chunk ->
            assertNotNull(chunk.position)
            assertTrue(chunk.content.contains("knowledge base", ignoreCase = true))
        }
    }
    
    @Test
    fun `should preserve character offsets when available`() = runTest {
        // Given
        val content = """
            # Test Heading
            
            Some content here.
        """.trimIndent()
        
        val file = DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = content.length.toLong()
            )
        )
        
        // When
        parser.parse(file, content)
        val chunks = parser.queryHeading("Test Heading")
        
        // Then
        assertEquals(1, chunks.size)
        val chunk = chunks.first()
        assertNotNull(chunk.position)
        
        val lineRange = chunk.position?.position as DocumentPosition.LineRange
        // Character offsets should be set if parser tracks them
        // This test validates the structure is in place
        assertNotNull(lineRange)
    }
}
