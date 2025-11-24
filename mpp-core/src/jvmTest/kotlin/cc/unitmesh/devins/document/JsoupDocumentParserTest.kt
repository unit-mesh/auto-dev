package cc.unitmesh.devins.document

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsoupDocumentParserTest {
    
    private val parser = JsoupDocumentParser()
    
    private val sampleHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="description" content="Sample HTML document for testing">
            <meta name="keywords" content="test, html, jsoup">
            <title>Test Document</title>
        </head>
        <body>
            <h1>Introduction</h1>
            <p>This is the introduction section with some content.</p>
            <p>It spans multiple paragraphs.</p>
            
            <h2>Background</h2>
            <p>Background information here.</p>
            
            <h3>History</h3>
            <p>Historical context goes here.</p>
            
            <h2>Motivation</h2>
            <p>Why this project exists.</p>
            
            <h1>Architecture</h1>
            
            <h2>Components</h2>
            <p>Description of components.</p>
            
            <h3>Frontend</h3>
            <p>Frontend architecture details.</p>
            
            <h3>Backend</h3>
            <p>Backend architecture details.</p>
            
            <h1>Implementation</h1>
            <p>Implementation details here.</p>
        </body>
        </html>
    """.trimIndent()
    
    @Test
    fun `test parse generates TOC from headings`() = runTest {
        val doc = DocumentFile(
            name = "test.html",
            path = "test.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        val result = parser.parse(doc, sampleHtml) as DocumentFile
        
        assertNotNull(result)
        assertTrue(result.toc.isNotEmpty(), "TOC should be generated from headings")
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        
        // Check that we have the expected headings
        val tocTitles = result.toc.map { it.title }
        assertTrue(tocTitles.contains("Introduction"))
        assertTrue(tocTitles.contains("Architecture"))
        assertTrue(tocTitles.contains("Implementation"))
    }
    
    @Test
    fun `test TOC preserves heading hierarchy`() = runTest {
        val doc = DocumentFile(
            name = "test.html",
            path = "test.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        val result = parser.parse(doc, sampleHtml) as DocumentFile
        
        // Check heading levels
        val h1Items = result.toc.filter { it.level == 1 }
        val h2Items = result.toc.filter { it.level == 2 }
        val h3Items = result.toc.filter { it.level == 3 }
        
        assertTrue(h1Items.isNotEmpty(), "Should have H1 headings")
        assertTrue(h2Items.isNotEmpty(), "Should have H2 headings")
        assertTrue(h3Items.isNotEmpty(), "Should have H3 headings")
        
        // Verify specific levels
        assertEquals(1, result.toc.first { it.title == "Introduction" }.level)
        assertEquals(2, result.toc.first { it.title == "Background" }.level)
        assertEquals(3, result.toc.first { it.title == "History" }.level)
    }
    
    @Test
    fun `test queryHeading finds matching headings`() = runTest {
        val doc = DocumentFile(
            name = "test.html",
            path = "test.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        parser.parse(doc, sampleHtml)
        
        val results = parser.queryHeading("Architecture")
        
        assertTrue(results.isNotEmpty(), "Should find Architecture heading")
        assertEquals("Architecture", results.first().chapterTitle)
    }
    
    @Test
    fun `test queryHeading with partial match`() = runTest {
        val doc = DocumentFile(
            name = "test.html",
            path = "test.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        parser.parse(doc, sampleHtml)
        
        val results = parser.queryHeading("arch")
        
        assertTrue(results.isNotEmpty(), "Should find headings containing 'arch'")
        assertTrue(results.any { it.chapterTitle?.contains("arch", ignoreCase = true) == true })
    }
    
    @Test
    fun `test queryChapter by anchor`() = runTest {
        val doc = DocumentFile(
            name = "test.html",
            path = "test.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        parser.parse(doc, sampleHtml)
        
        // Query by generated anchor
        val chapter = parser.queryChapter("#architecture")
        assertNotNull(chapter, "Should find chapter by anchor")
        assertEquals("Architecture", chapter.chapterTitle)
    }
    
    @Test
    fun `test document chunks contain correct content`() = runTest {
        val doc = DocumentFile(
            name = "test.html",
            path = "test.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        parser.parse(doc, sampleHtml)
        
        val introChunk = parser.queryChapter("#introduction")
        assertNotNull(introChunk)
        
        // Should contain the introduction content
        assertTrue(
            introChunk.content.contains("introduction section"),
            "Chunk content should include introduction text"
        )
    }
    
    @Test
    fun `test HTML with custom id attributes`() = runTest {
        val htmlWithIds = """
            <!DOCTYPE html>
            <html>
            <body>
                <h1 id="custom-intro">Introduction</h1>
                <p>Content here.</p>
                
                <h2 id="section-1">First Section</h2>
                <p>Section content.</p>
            </body>
            </html>
        """.trimIndent()
        
        val doc = DocumentFile(
            name = "test.html",
            path = "test.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = htmlWithIds.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        val result = parser.parse(doc, htmlWithIds) as DocumentFile
        
        // Check that custom IDs are preserved
        val introToc = result.toc.first { it.title == "Introduction" }
        assertEquals("#custom-intro", introToc.anchor)
        
        val sectionToc = result.toc.first { it.title == "First Section" }
        assertEquals("#section-1", sectionToc.anchor)
    }
    
    @Test
    fun `test empty HTML`() = runTest {
        val emptyHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Empty</title></head>
            <body></body>
            </html>
        """.trimIndent()
        
        val doc = DocumentFile(
            name = "empty.html",
            path = "empty.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = emptyHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        parser.parse(doc, emptyHtml)
        
        val results = parser.queryHeading("anything")
        assertTrue(results.isEmpty(), "Should return empty list for empty document")
    }
    
    @Test
    fun `test HTML without headings`() = runTest {
        val htmlNoHeadings = """
            <!DOCTYPE html>
            <html>
            <body>
                <p>Just some paragraph text.</p>
                <p>Another paragraph.</p>
            </body>
            </html>
        """.trimIndent()
        
        val doc = DocumentFile(
            name = "no-headings.html",
            path = "no-headings.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = htmlNoHeadings.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        val result = parser.parse(doc, htmlNoHeadings) as DocumentFile
        
        // Should still parse successfully
        assertEquals(ParseStatus.PARSED, result.metadata.parseStatus)
        assertTrue(result.toc.isEmpty(), "Should have no TOC items")
        
        // Should still extract content
        val content = parser.getDocumentContent()
        assertNotNull(content)
        assertTrue(content.contains("paragraph text"))
    }
    
    @Test
    fun `test HTML with complex structure`() = runTest {
        val complexHtml = """
            <!DOCTYPE html>
            <html>
            <body>
                <h1>Main Title</h1>
                <div class="section">
                    <p>Introduction paragraph.</p>
                    <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                    </ul>
                </div>
                
                <h2>Subsection</h2>
                <table>
                    <tr><td>Cell 1</td><td>Cell 2</td></tr>
                </table>
                
                <h3>Details</h3>
                <pre>Code block content</pre>
            </body>
            </html>
        """.trimIndent()
        
        val doc = DocumentFile(
            name = "complex.html",
            path = "complex.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = complexHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        parser.parse(doc, complexHtml)
        
        // Should extract text from various HTML elements
        val content = parser.getDocumentContent()
        assertNotNull(content)
        assertTrue(content.contains("Introduction paragraph"))
        assertTrue(content.contains("Item 1"))
        assertTrue(content.contains("Cell 1"))
        assertTrue(content.contains("Code block content"))
    }
    
    @Test
    fun `test position metadata is set correctly`() = runTest {
        val doc = DocumentFile(
            name = "test.html",
            path = "/path/to/test.html",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleHtml.length.toLong(),
                language = "html",
                mimeType = "text/html",
                formatType = DocumentFormatType.HTML
            )
        )
        
        parser.parse(doc, sampleHtml)
        
        val chunk = parser.queryChapter("#introduction")
        assertNotNull(chunk)
        assertNotNull(chunk.position)
        
        assertEquals("/path/to/test.html", chunk.position?.documentPath)
        assertEquals(DocumentFormatType.HTML, chunk.position?.formatType)
        assertTrue(chunk.position?.position is DocumentPosition.LineRange)
    }
}
