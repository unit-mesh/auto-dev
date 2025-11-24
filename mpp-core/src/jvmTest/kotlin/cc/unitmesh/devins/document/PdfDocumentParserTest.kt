package cc.unitmesh.devins.document

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PdfDocumentParserTest {

    private lateinit var parser: PdfDocumentParser

    @Before
    fun setup() {
        parser = PdfDocumentParser()
        // Initialize platform parsers to ensure factory is set up (though we use parser directly here)
        DocumentRegistry.initializePlatformParsers()
    }

    @Test
    fun `should parse PDF document with content verification`() = runTest {
        // Given
        val tempFile = createTempFileFromResource("sample2.pdf")
        val documentFile = createDocumentFile(tempFile.name, tempFile.absolutePath, tempFile.length(), DocumentFormatType.PDF)

        // When
        val result = parser.parse(documentFile, "") // Content is ignored by PdfDocumentParser

        // Then
        assertTrue(result is DocumentFile)
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        assertEquals(DocumentFormatType.PDF, result.metadata.formatType)
        
        val extractedContent = parser.getDocumentContent()
        assertNotNull(extractedContent)
        // Verify content from sample2.pdf
        assertTrue(extractedContent.contains("Consult doc/pdftex/manual.pdf"), "Should contain specific text from PDF")
        
        // Check chunks
        val chunks = parser.queryHeading("")
        assertTrue(chunks.isNotEmpty(), "Should create chunks")
        
        val firstChunk = chunks.first()
        assertNotNull(firstChunk.position)
        assertEquals(tempFile.absolutePath, firstChunk.position?.documentPath)
        assertTrue(firstChunk.position?.position is DocumentPosition.PageRange, "Position should be PageRange")
        
        val pageRange = firstChunk.position?.position as DocumentPosition.PageRange
        assertTrue(pageRange.startPage > 0)
        
        println("✓ PDF parsed successfully: ${extractedContent.length} chars")
    }

    @Test
    fun `should register PdfDocumentParser in factory`() {
        // When
        val pdfParser = DocumentParserFactory.createParser(DocumentFormatType.PDF)
        
        // Then
        assertNotNull(pdfParser)
        assertTrue(pdfParser is PdfDocumentParser, "Factory should return PdfDocumentParser for PDF")
        
        println("✓ DocumentParserFactory integration verified")
    }

    private fun createTempFileFromResource(fileName: String): File {
        val inputStream = javaClass.classLoader.getResourceAsStream(fileName)
            ?: throw IllegalArgumentException("Resource not found: $fileName")
        
        // Create temp file with .pdf extension
        val tempFile = File.createTempFile("test-", ".pdf")
        tempFile.deleteOnExit()
        
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }

    private fun createDocumentFile(
        name: String,
        path: String,
        size: Long,
        formatType: DocumentFormatType
    ): DocumentFile {
        return DocumentFile(
            name = name,
            path = path,
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = size,
                formatType = formatType
            )
        )
    }
}
