package cc.unitmesh.devins.document

import cc.unitmesh.devins.document.docql.DocQLResult
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * DocQL Multi-Format Tests
 * 
 * Tests DocQL query execution on various document formats (PDF, DOCX, Markdown)
 * Verifies that DocumentRegistry and platform-specific parsers work correctly
 * 
 * @author Phodal Huang
 */
class DocQLMultiFormatTest {
    
    @Before
    fun setup() {
        // Clear cache and initialize platform parsers
        DocumentRegistry.clearCache()
        DocumentRegistry.initializePlatformParsers()
    }
    
    @Test
    fun `should query PDF document with DocQL`() = runTest {
        // Given - Parse and register PDF document
        val resourceBytes = loadResource("sample2.pdf")
        
        val documentFile = createDocumentFile("sample2.pdf", resourceBytes.size, DocumentFormatType.PDF)
        val parser = DocumentRegistry.getParser(DocumentFormatType.PDF)
        assertNotNull(parser, "PDF parser should be available on JVM")
        
        // Use parseBytes for binary files
        val tikaParser = parser as? TikaDocumentParser
        assertNotNull(tikaParser, "PDF parser should be TikaDocumentParser")
        val parsedDoc = tikaParser.parseBytes(documentFile, resourceBytes)
        DocumentRegistry.registerDocument(documentFile.path, parsedDoc, parser)
        
        // When - Query using DocQL
        val result = DocumentRegistry.queryDocument(
            documentFile.path,
            "$.content.heading(\"Consult\")"
        )
        
        // Then
        assertNotNull(result)
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        assertTrue(chunks.isNotEmpty(), "Should find chunks containing 'Consult'")
        
        // Verify position metadata exists
        val firstChunk = chunks.first()
        assertNotNull(firstChunk.position)
        assertEquals(DocumentFormatType.PDF, firstChunk.position?.formatType)
        
        println("✓ PDF DocQL query successful: found ${chunks.size} chunks")
        println("  Location: ${firstChunk.position?.toLocationString()}")
    }
    
    @Test
    fun `should query DOCX document with DocQL`() = runTest {
        // Given - Parse and register DOCX document
        val resourceBytes = loadResource("word-sample.docx")
        val content = String(resourceBytes, Charsets.ISO_8859_1)
        
        val documentFile = createDocumentFile("word-sample.docx", resourceBytes.size, DocumentFormatType.DOCX)
        val parser = DocumentRegistry.getParser(DocumentFormatType.DOCX)
        assertNotNull(parser, "DOCX parser should be available on JVM")
        
        val parsedDoc = parser.parse(documentFile, content)
        DocumentRegistry.registerDocument(documentFile.path, parsedDoc, parser)
        
        // When - Query using DocQL
        val result = DocumentRegistry.queryDocument(
            documentFile.path,
            "$.content.heading(\"links\")"
        )
        
        // Then
        assertNotNull(result)
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        assertTrue(chunks.isNotEmpty(), "Should find chunks containing 'links'")
        
        // Verify position metadata exists
        val firstChunk = chunks.first()
        assertNotNull(firstChunk.position)
        assertEquals(DocumentFormatType.DOCX, firstChunk.position?.formatType)
        
        println("✓ DOCX DocQL query successful: found ${chunks.size} chunks")
        println("  Location: ${firstChunk.position?.toLocationString()}")
    }
    
    @Test
    fun `should query DOC document with DocQL`() = runTest {
        // Given - Parse and register DOC document
        val resourceBytes = loadResource("word-sample.doc")
        val content = String(resourceBytes, Charsets.ISO_8859_1)
        
        val documentFile = createDocumentFile("word-sample.doc", resourceBytes.size, DocumentFormatType.DOCX)
        val parser = DocumentRegistry.getParser(DocumentFormatType.DOCX)
        assertNotNull(parser)
        
        val parsedDoc = parser.parse(documentFile, content)
        DocumentRegistry.registerDocument(documentFile.path, parsedDoc, parser)
        
        // When - Query using DocQL
        val result = DocumentRegistry.queryDocument(
            documentFile.path,
            "$.content.heading(\"permissions\")"
        )
        
        // Then
        assertNotNull(result)
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        assertTrue(chunks.isNotEmpty(), "Should find chunks containing 'permissions'")
        
        println("✓ DOC DocQL query successful: found ${chunks.size} chunks")
    }
    
    @Test
    fun `should verify DocumentRegistry manages multiple documents`() = runTest {
        // Given - Register multiple documents
        val formats = listOf(
            Triple("sample2.pdf", DocumentFormatType.PDF, "sample2.pdf"),
            Triple("word-sample.docx", DocumentFormatType.DOCX, "word-sample.docx")
        )
        
        formats.forEach { (fileName, format, _) ->
            val resourceBytes = loadResource(fileName)
            val content = String(resourceBytes, Charsets.ISO_8859_1)
            val docFile = createDocumentFile(fileName, resourceBytes.size, format)
            val parser = DocumentRegistry.getParser(format)
            assertNotNull(parser)
            
            val parsed = parser.parse(docFile, content)
            DocumentRegistry.registerDocument(docFile.path, parsed, parser)
        }
        
        // When
        val registeredPaths = DocumentRegistry.getRegisteredPaths()
        
        // Then
        assertEquals(2, registeredPaths.size)
        assertTrue(DocumentRegistry.isDocumentRegistered("/test/sample2.pdf"))
        assertTrue(DocumentRegistry.isDocumentRegistered("/test/word-sample.docx"))
        
        println("✓ DocumentRegistry managing ${registeredPaths.size} documents")
    }
    
    @Test
    fun `should auto-detect and use correct parser`() {
        // When
        val pdfParser = DocumentRegistry.getParserForFile("document.pdf")
        val docxParser = DocumentRegistry.getParserForFile("document.docx")
        val mdParser = DocumentRegistry.getParserForFile("README.md")
        
        // Then
        assertNotNull(pdfParser)
        assertTrue(pdfParser is TikaDocumentParser)
        
        assertNotNull(docxParser)
        assertTrue(docxParser is TikaDocumentParser)
        
        assertNotNull(mdParser)
        assertTrue(mdParser is MarkdownDocumentParser)
        
        println("✓ Parser auto-detection working correctly")
    }
    
    @Test
    fun `should verify position metadata in DocQL results`() = runTest {
        // Given
        val resourceBytes = loadResource("sample2.pdf")
        val documentFile = createDocumentFile("sample2.pdf", resourceBytes.size, DocumentFormatType.PDF)
        val parser = DocumentRegistry.getParser(DocumentFormatType.PDF)!!
        
        // Use parseBytes for binary files
        val tikaParser = parser as TikaDocumentParser
        val parsedDoc = tikaParser.parseBytes(documentFile, resourceBytes)
        DocumentRegistry.registerDocument(documentFile.path, parsedDoc, parser)
        
        // When - Query and get chunks
        val result = DocumentRegistry.queryDocument(documentFile.path, "$.content.heading(\"\")")
        
        // Then - Verify all chunks have position metadata
        assertTrue(result is DocQLResult.Chunks)
        val chunks = (result as DocQLResult.Chunks).items
        
        chunks.forEach { chunk ->
            assertNotNull(chunk.position, "Each chunk should have position metadata")
            assertEquals(documentFile.path, chunk.position?.documentPath)
            assertEquals(DocumentFormatType.PDF, chunk.position?.formatType)
            assertTrue(chunk.position?.position is DocumentPosition.LineRange)
            
            // Verify location string can be generated
            val locationString = chunk.position?.toLocationString()
            assertNotNull(locationString)
            assertTrue(locationString.contains(documentFile.path))
        }
        
        println("✓ All ${chunks.size} chunks have valid position metadata")
    }
    
    /**
     * Helper function to load test resource bytes
     */
    private fun loadResource(fileName: String): ByteArray {
        val inputStream = javaClass.classLoader.getResourceAsStream(fileName)
            ?: throw IllegalArgumentException("Resource not found: $fileName")
        return inputStream.use { it.readBytes() }
    }
    
    /**
     * Helper function to create DocumentFile
     */
    private fun createDocumentFile(
        name: String,
        size: Int,
        formatType: DocumentFormatType
    ): DocumentFile {
        return DocumentFile(
            name = name,
            path = "/test/$name",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = size.toLong(),
                formatType = formatType
            )
        )
    }
}

