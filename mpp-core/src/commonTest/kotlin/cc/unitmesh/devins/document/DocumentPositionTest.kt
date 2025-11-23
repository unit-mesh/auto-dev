package cc.unitmesh.devins.document

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for DocumentPosition and PositionMetadata
 * Testing the position tracking abstraction for multi-format support
 */
class DocumentPositionTest {
    
    @Test
    fun `should create LineRange position`() {
        // Given
        val startLine = 10
        val endLine = 20
        
        // When
        val position = DocumentPosition.LineRange(
            startLine = startLine,
            endLine = endLine
        )
        
        // Then
        assertEquals(startLine, position.startLine)
        assertEquals(endLine, position.endLine)
    }
    
    @Test
    fun `should create LineRange with character offsets`() {
        // Given
        val startLine = 5
        val endLine = 8
        val startOffset = 120
        val endOffset = 350
        
        // When
        val position = DocumentPosition.LineRange(
            startLine = startLine,
            endLine = endLine,
            startOffset = startOffset,
            endOffset = endOffset
        )
        
        // Then
        assertEquals(startLine, position.startLine)
        assertEquals(endLine, position.endLine)
        assertEquals(startOffset, position.startOffset)
        assertEquals(endOffset, position.endOffset)
    }
    
    @Test
    fun `should create PageRange position`() {
        // Given
        val startPage = 1
        val endPage = 5
        
        // When
        val position = DocumentPosition.PageRange(
            startPage = startPage,
            endPage = endPage
        )
        
        // Then
        assertEquals(startPage, position.startPage)
        assertEquals(endPage, position.endPage)
    }
    
    @Test
    fun `should create SectionRange position`() {
        // Given
        val sectionId = "chapter-1.2"
        val paragraphIndex = 3
        
        // When
        val position = DocumentPosition.SectionRange(
            sectionId = sectionId,
            paragraphIndex = paragraphIndex
        )
        
        // Then
        assertEquals(sectionId, position.sectionId)
        assertEquals(paragraphIndex, position.paragraphIndex)
    }
    
    @Test
    fun `should create PositionMetadata for Markdown document`() {
        // Given
        val documentPath = "/path/to/document.md"
        val position = DocumentPosition.LineRange(
            startLine = 10,
            endLine = 15
        )
        
        // When
        val metadata = PositionMetadata(
            documentPath = documentPath,
            formatType = DocumentFormatType.MARKDOWN,
            position = position
        )
        
        // Then
        assertEquals(documentPath, metadata.documentPath)
        assertEquals(DocumentFormatType.MARKDOWN, metadata.formatType)
        assertEquals(position, metadata.position)
        assertTrue(metadata.position is DocumentPosition.LineRange)
    }
    
    @Test
    fun `should create PositionMetadata for PDF document`() {
        // Given
        val documentPath = "/path/to/document.pdf"
        val position = DocumentPosition.PageRange(
            startPage = 1,
            endPage = 3
        )
        
        // When
        val metadata = PositionMetadata(
            documentPath = documentPath,
            formatType = DocumentFormatType.PDF,
            position = position
        )
        
        // Then
        assertEquals(documentPath, metadata.documentPath)
        assertEquals(DocumentFormatType.PDF, metadata.formatType)
        assertEquals(position, metadata.position)
        assertTrue(metadata.position is DocumentPosition.PageRange)
    }
    
    @Test
    fun `should format position as string for Markdown`() {
        // Given
        val position = DocumentPosition.LineRange(
            startLine = 10,
            endLine = 15
        )
        val metadata = PositionMetadata(
            documentPath = "/path/to/doc.md",
            formatType = DocumentFormatType.MARKDOWN,
            position = position
        )
        
        // When
        val formatted = metadata.toLocationString()
        
        // Then
        assertEquals("/path/to/doc.md:10-15", formatted)
    }
    
    @Test
    fun `should format position as string for single line`() {
        // Given
        val position = DocumentPosition.LineRange(
            startLine = 10,
            endLine = 10
        )
        val metadata = PositionMetadata(
            documentPath = "/path/to/doc.md",
            formatType = DocumentFormatType.MARKDOWN,
            position = position
        )
        
        // When
        val formatted = metadata.toLocationString()
        
        // Then
        assertEquals("/path/to/doc.md:10", formatted)
    }
    
    @Test
    fun `should format position as string for PDF`() {
        // Given
        val position = DocumentPosition.PageRange(
            startPage = 5,
            endPage = 7
        )
        val metadata = PositionMetadata(
            documentPath = "/path/to/doc.pdf",
            formatType = DocumentFormatType.PDF,
            position = position
        )
        
        // When
        val formatted = metadata.toLocationString()
        
        // Then
        assertEquals("/path/to/doc.pdf:page 5-7", formatted)
    }
    
    @Test
    fun `should check if position contains line number`() {
        // Given
        val position = DocumentPosition.LineRange(
            startLine = 10,
            endLine = 20
        )
        
        // When & Then
        assertTrue(position.contains(15))
        assertTrue(position.contains(10))
        assertTrue(position.contains(20))
        assertTrue(!position.contains(5))
        assertTrue(!position.contains(25))
    }
    
    @Test
    fun `should check if positions overlap`() {
        // Given
        val position1 = DocumentPosition.LineRange(
            startLine = 10,
            endLine = 20
        )
        val position2 = DocumentPosition.LineRange(
            startLine = 15,
            endLine = 25
        )
        val position3 = DocumentPosition.LineRange(
            startLine = 25,
            endLine = 30
        )
        
        // When & Then
        assertTrue(position1.overlaps(position2))
        assertTrue(position2.overlaps(position1))
        assertTrue(!position1.overlaps(position3))
        assertTrue(!position3.overlaps(position1))
    }
}
