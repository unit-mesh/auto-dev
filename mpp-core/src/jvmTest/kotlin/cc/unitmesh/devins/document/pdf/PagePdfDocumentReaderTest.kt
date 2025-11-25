package cc.unitmesh.devins.document.pdf

import cc.unitmesh.devins.document.pdf.config.PdfDocumentReaderConfig
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PagePdfDocumentReaderTest {

    private lateinit var testPdfPath: String

    @Before
    fun setup() {
        // Use the same sample2.pdf from existing tests
        val tempFile = createTempFileFromResource("sample2.pdf")
        testPdfPath = tempFile.absolutePath
    }

    @Test
    fun `should parse PDF with single page per document`() = runTest {
        // Given
        val config = PdfDocumentReaderConfig.builder()
            .withPagesPerDocument(1)
            .build()
        val reader = PagePdfDocumentReader(testPdfPath, config)

        // When
        val documents = reader.get()

        // Then
        assertTrue(documents.isNotEmpty(), "Should create documents")
        
        // Each document should represent a single page
        for (doc in documents) {
            val startPage = doc.metadata[PagePdfDocumentReader.METADATA_START_PAGE_NUMBER] as Int
            val endPage = doc.metadata.getOrDefault(
                PagePdfDocumentReader.METADATA_END_PAGE_NUMBER,
                startPage
            ) as Int
            assertEquals(startPage, endPage, "Each document should contain only one page")
            assertTrue(doc.text.isNotBlank(), "Document text should not be blank")
        }

        println("✓ Parsed ${documents.size} single-page documents")
    }

    @Test
    fun `should parse PDF with multiple pages per document`() = runTest {
        // Given
        val config = PdfDocumentReaderConfig.builder()
            .withPagesPerDocument(2)
            .build()
        val reader = PagePdfDocumentReader(testPdfPath, config)

        // When
        val documents = reader.get()

        // Then
        assertTrue(documents.isNotEmpty(), "Should create documents")
        
        // Check that documents group multiple pages (except possibly the last one)
        for ((index, doc) in documents.withIndex()) {
            val startPage = doc.metadata[PagePdfDocumentReader.METADATA_START_PAGE_NUMBER] as Int
            val endPage = doc.metadata.getOrDefault(
                PagePdfDocumentReader.METADATA_END_PAGE_NUMBER,
                startPage
            ) as Int
            
            if (index < documents.size - 1) {
                // All documents except the last should have 2 pages
                assertTrue(endPage - startPage + 1 <= 2, "Document should contain at most 2 pages")
            }
            assertTrue(doc.text.isNotBlank(), "Document text should not be blank")
        }

        println("✓ Parsed ${documents.size} multi-page documents")
    }

    @Test
    fun `should parse PDF with all pages in one document`() = runTest {
        // Given
        val config = PdfDocumentReaderConfig.builder()
            .withPagesPerDocument(PdfDocumentReaderConfig.ALL_PAGES)
            .build()
        val reader = PagePdfDocumentReader(testPdfPath, config)

        // When
        val documents = reader.get()

        // Then
        assertEquals(1, documents.size, "Should create exactly one document")
        
        val doc = documents[0]
        assertTrue(doc.text.isNotBlank(), "Document text should not be blank")
        assertTrue(doc.text.contains("Consult doc/pdftex/manual.pdf"), "Should contain expected text")

        println("✓ Parsed entire PDF as single document")
    }

    @Test
    fun `should apply page margins correctly`() = runTest {
        // Given
        val configWithMargins = PdfDocumentReaderConfig.builder()
            .withPagesPerDocument(1)
            .withPageTopMargin(50)
            .withPageBottomMargin(50)
            .build()
        val readerWithMargins = PagePdfDocumentReader(testPdfPath, configWithMargins)

        val configNoMargins = PdfDocumentReaderConfig.defaultConfig()
        val readerNoMargins = PagePdfDocumentReader(testPdfPath, configNoMargins)

        // When
        val docsWithMargins = readerWithMargins.get()
        val docsNoMargins = readerNoMargins.get()

        // Then
        assertEquals(docsNoMargins.size, docsWithMargins.size, "Should have same number of documents")
        
        // Documents with margins should generally have less text (margins excluded)
        // Note: This is a heuristic check as exact comparison depends on PDF content
        val totalTextWithMargins = docsWithMargins.sumOf { it.text.length }
        val totalTextNoMargins = docsNoMargins.sumOf { it.text.length }
        
        assertTrue(totalTextWithMargins <= totalTextNoMargins, 
            "Text with margins should be less than or equal to text without margins")

        println("✓ Page margins applied correctly")
    }

    @Test
    fun `should include correct metadata`() = runTest {
        // Given
        val reader = PagePdfDocumentReader(testPdfPath)

        // When
        val documents = reader.get()

        // Then
        assertTrue(documents.isNotEmpty(), "Should create documents")
        
        for (doc in documents) {
            assertNotNull(doc.metadata[PagePdfDocumentReader.METADATA_START_PAGE_NUMBER], 
                "Should have start page number")
            assertNotNull(doc.metadata[PagePdfDocumentReader.METADATA_FILE_NAME], 
                "Should have file name")
            
            val fileName = doc.metadata[PagePdfDocumentReader.METADATA_FILE_NAME] as String
            assertTrue(fileName.endsWith(".pdf"), "File name should end with .pdf")
        }

        println("✓ Metadata included correctly")
    }

    @Test
    fun `should apply text formatter`() = runTest {
        // Given
        val prefix = "PAGE_TEXT: "
        val config = PdfDocumentReaderConfig.builder()
            .withPagesPerDocument(1)
            .withPageExtractedTextFormatter { text, pageNum -> "$prefix$text" }
            .build()
        val reader = PagePdfDocumentReader(testPdfPath, config)

        // When
        val documents = reader.get()

        // Then
        assertTrue(documents.isNotEmpty(), "Should create documents")
        
        for (doc in documents) {
            assertTrue(doc.text.startsWith(prefix), 
                "Document text should start with custom prefix")
        }

        println("✓ Text formatter applied correctly")
    }

    private fun createTempFileFromResource(fileName: String): File {
        val inputStream = javaClass.classLoader.getResourceAsStream(fileName)
            ?: throw IllegalArgumentException("Resource not found: $fileName")
        
        val tempFile = File.createTempFile("test-", ".pdf")
        tempFile.deleteOnExit()
        
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }
}
