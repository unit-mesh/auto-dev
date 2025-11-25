package cc.unitmesh.devins.document

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tika Document Parser Tests
 * 
 * Tests for TikaDocumentParser on JVM platform, parsing various document formats
 * Reference: org.springframework.ai.reader.tika.TikaDocumentReaderTests
 * 
 * @author Phodal Huang
 */
class TikaDocumentParserTest {
    
    private lateinit var parser: TikaDocumentParser
    
    @Before
    fun setup() {
        parser = TikaDocumentParser()
        // Initialize platform parsers (auto-registers Tika on JVM)
        DocumentRegistry.initializePlatformParsers()
    }
    
    @Test
    fun `should parse DOCX document with content verification`() = runTest {
        // Given
        val resourceBytes = loadResource("word-sample.docx")
        val content = String(resourceBytes, Charsets.ISO_8859_1)
        
        val documentFile = createDocumentFile("word-sample.docx", resourceBytes.size, DocumentFormatType.DOCX)
        
        // When
        val result = parser.parse(documentFile, content)
        
        // Then
        assertTrue(result is DocumentFile)
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        
        val extractedContent = parser.getDocumentContent()
        assertNotNull(extractedContent)
        assertTrue(extractedContent.contains("Two kinds of links are possible, those that refer to an external website"))
        println("✓ DOCX parsed successfully: ${extractedContent.length} chars")
    }
    
    @Test
    fun `should parse DOC document with content verification`() = runTest {
        // Given
        val resourceBytes = loadResource("word-sample.doc")
        val content = String(resourceBytes, Charsets.ISO_8859_1)
        
        val documentFile = createDocumentFile("word-sample.doc", resourceBytes.size, DocumentFormatType.DOCX)
        
        // When
        val result = parser.parse(documentFile, content)
        
        // Then
        assertTrue(result is DocumentFile)
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        
        val extractedContent = parser.getDocumentContent()
        assertNotNull(extractedContent)
        assertTrue(extractedContent.contains("The limited permissions granted above are perpetual and will not be revoked by OASIS"))
        println("✓ DOC parsed successfully: ${extractedContent.length} chars")
    }
    
    @Test
    fun `should parse PDF document with content verification`() = runTest {
        // Given
        val resourceBytes = loadResource("sample2.pdf")
        
        val documentFile = createDocumentFile("sample2.pdf", resourceBytes.size, DocumentFormatType.PDF)
        
        // When - use parseBytes for binary files (preferred method)
        val result = parser.parseBytes(documentFile, resourceBytes)
        
        // Then
        assertTrue(result is DocumentFile)
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        assertNotNull(result.metadata.mimeType)
        
        val extractedContent = parser.getDocumentContent()
        assertNotNull(extractedContent)
        assertTrue(extractedContent.contains("Consult doc/pdftex/manual.pdf from your tetex distribution for more"))
        println("✓ PDF parsed successfully: ${extractedContent.length} chars")
    }
    
    @Test
    fun `should parse PPT document with content verification`() = runTest {
        // Given
        val resourceBytes = loadResource("sample.ppt")
        
        val documentFile = createDocumentFile("sample.ppt", resourceBytes.size, DocumentFormatType.DOCX)
        
        // When - use parseBytes for binary files (preferred method)
        val result = parser.parseBytes(documentFile, resourceBytes)
        
        // Then
        assertTrue(result is DocumentFile)
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        
        val extractedContent = parser.getDocumentContent()
        assertNotNull(extractedContent)
        assertTrue(extractedContent.contains("Sed ipsum tortor, fringilla a consectetur eget, cursus posuere sem."))
        println("✓ PPT parsed successfully: ${extractedContent.length} chars")
    }
    
    @Test
    fun `should parse PPT document with legacy string method`() = runTest {
        // Test backward compatibility with String-based parse method
        val resourceBytes = loadResource("sample.ppt")
        val content = String(resourceBytes, Charsets.ISO_8859_1)
        
        val documentFile = createDocumentFile("sample.ppt", resourceBytes.size, DocumentFormatType.DOCX)
        
        // When - use legacy parse method
        val result = parser.parse(documentFile, content)
        
        // Then
        assertTrue(result is DocumentFile)
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        
        val extractedContent = parser.getDocumentContent()
        assertNotNull(extractedContent)
        assertTrue(extractedContent.contains("Sed ipsum tortor, fringilla a consectetur eget, cursus posuere sem."))
        println("✓ PPT parsed successfully (legacy): ${extractedContent.length} chars")
    }
    
    @Test
    fun `should parse PPTX document with content verification`() = runTest {
        // Given
        val resourceBytes = loadResource("sample.pptx")
        
        val documentFile = createDocumentFile("sample.pptx", resourceBytes.size, DocumentFormatType.DOCX)
        
        // When - use parseBytes for binary files (preferred method)
        val result = parser.parseBytes(documentFile, resourceBytes)
        
        // Then
        assertTrue(result is DocumentFile)
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        
        val extractedContent = parser.getDocumentContent()
        assertNotNull(extractedContent)
        assertTrue(extractedContent.contains("Lorem ipsum dolor sit amet, consectetur adipiscing elit."))
        println("✓ PPTX parsed successfully: ${extractedContent.length} chars")
    }
    
    @Test
    fun `should create document chunks with position metadata`() = runTest {
        // Given
        val resourceBytes = loadResource("sample2.pdf")
        val content = String(resourceBytes, Charsets.ISO_8859_1)
        
        val documentFile = createDocumentFile("sample2.pdf", resourceBytes.size, DocumentFormatType.PDF)
        
        // When
        parser.parse(documentFile, content)
        val chunks = parser.queryHeading("")  // Get all chunks
        
        // Then
        assertTrue(chunks.isNotEmpty(), "Should have created document chunks")
        
        val firstChunk = chunks.first()
        assertNotNull(firstChunk.position)
        assertEquals("/test/sample2.pdf", firstChunk.position?.documentPath)
        assertEquals(DocumentFormatType.PDF, firstChunk.position?.formatType)
        assertTrue(firstChunk.position?.position is DocumentPosition.LineRange)
        
        val locationString = firstChunk.position?.toLocationString()
        assertNotNull(locationString)
        assertTrue(locationString.contains("/test/sample2.pdf"))
        println("✓ Position metadata verified: $locationString")
    }
    
    @Test
    fun `should register Tika parser in factory`() {
        // When
        val docxParser = DocumentParserFactory.createParser(DocumentFormatType.DOCX)
        val markdownParser = DocumentParserFactory.createParser(DocumentFormatType.MARKDOWN)

        assertNotNull(docxParser)
        assertTrue(docxParser is TikaDocumentParser)
        
        assertNotNull(markdownParser)
        assertTrue(markdownParser is MarkdownDocumentParser)
        
        println("✓ DocumentParserFactory integration verified")
    }
    
    @Test
    fun `should detect format from file extension`() {
        // When & Then
        assertEquals(DocumentFormatType.PDF, DocumentParserFactory.detectFormat("document.pdf"))
        assertEquals(DocumentFormatType.DOCX, DocumentParserFactory.detectFormat("document.docx"))
        assertEquals(DocumentFormatType.DOCX, DocumentParserFactory.detectFormat("document.doc"))
        assertEquals(DocumentFormatType.MARKDOWN, DocumentParserFactory.detectFormat("README.md"))
        assertEquals(DocumentFormatType.PLAIN_TEXT, DocumentParserFactory.detectFormat("notes.txt"))
        
        println("✓ Format detection verified")
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

