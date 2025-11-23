package cc.unitmesh.devins.document

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarkdownDocumentParserTest {
    
    private val parser = MarkdownDocumentParser()
    
    private val sampleMarkdown = """
        # Introduction
        
        This is the introduction section with some content.
        It spans multiple lines.
        
        ## Background
        
        Background information here.
        
        ### History
        
        Historical context goes here.
        
        ## Motivation
        
        Why this project exists.
        
        # Architecture
        
        ## Components
        
        Description of components.
        
        ### Frontend
        
        Frontend architecture details.
        
        ### Backend
        
        Backend architecture details.
        
        # Implementation
        
        Implementation details here.
    """.trimIndent()
    
    @Test
    fun `test parse generates TOC`() = runTest {
        val doc = DocumentFile(
            name = "test.md",
            path = "test.md",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleMarkdown.length.toLong(),
                language = "markdown",
                mimeType = "text/markdown"
            )
        )
        
        val result = parser.parse(doc, sampleMarkdown)
        
        assertNotNull(result)
        // TOC should have been built (though not directly returned in current implementation)
    }
    
    @Test
    fun `test queryHeading finds matching headings`() = runTest {
        val doc = DocumentFile(
            name = "test.md",
            path = "test.md",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleMarkdown.length.toLong(),
                language = "markdown",
                mimeType = "text/markdown"
            )
        )
        
        parser.parse(doc, sampleMarkdown)
        
        val results = parser.queryHeading("Architecture")
        
        assertTrue(results.isNotEmpty(), "Should find Architecture heading")
        assertEquals("Architecture", results.first().chapterTitle)
    }
    
    @Test
    fun `test queryHeading with partial match`() = runTest {
        val doc = DocumentFile(
            name = "test.md",
            path = "test.md",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleMarkdown.length.toLong(),
                language = "markdown",
                mimeType = "text/markdown"
            )
        )
        
        parser.parse(doc, sampleMarkdown)
        
        val results = parser.queryHeading("arch")
        
        assertTrue(results.isNotEmpty(), "Should find headings containing 'arch'")
        assertTrue(results.any { it.chapterTitle?.contains("arch", ignoreCase = true) == true })
    }
    
    @Test
    fun `test queryChapter by numeric ID`() = runTest {
        val doc = DocumentFile(
            name = "test.md",
            path = "test.md",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleMarkdown.length.toLong(),
                language = "markdown",
                mimeType = "text/markdown"
            )
        )
        
        parser.parse(doc, sampleMarkdown)
        
        // Query first chapter (1 = Introduction)
        val chapter1 = parser.queryChapter("1")
        assertNotNull(chapter1, "Should find chapter 1")
        assertEquals("Introduction", chapter1.chapterTitle)
        
        // Query second chapter (2 = Architecture)
        val chapter2 = parser.queryChapter("2")
        assertNotNull(chapter2)
        assertEquals("Architecture", chapter2.chapterTitle)
    }
    
    @Test
    fun `test queryChapter by hierarchical ID`() = runTest {
        val doc = DocumentFile(
            name = "test.md",
            path = "test.md",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleMarkdown.length.toLong(),
                language = "markdown",
                mimeType = "text/markdown"
            )
        )
        
        parser.parse(doc, sampleMarkdown)
        
        // Query sub-chapter (1.1 = Background)
        val chapter11 = parser.queryChapter("1.1")
        assertNotNull(chapter11, "Should find chapter 1.1")
        assertEquals("Background", chapter11.chapterTitle)
        
        // Query deeper sub-chapter (1.1.1 = History)
        val chapter111 = parser.queryChapter("1.1.1")
        assertNotNull(chapter111, "Should find chapter 1.1.1")
        assertEquals("History", chapter111.chapterTitle)
    }
    
    @Test
    fun `test document chunks contain correct content`() = runTest {
        val doc = DocumentFile(
            name = "test.md",
            path = "test.md",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = sampleMarkdown.length.toLong(),
                language = "markdown",
                mimeType = "text/markdown"
            )
        )
        
        parser.parse(doc, sampleMarkdown)
        
        val introChunk = parser.queryChapter("1")
        assertNotNull(introChunk)
        
        // Should contain the introduction content
        assertTrue(
            introChunk.content.contains("introduction section"),
            "Chunk content should include introduction text"
        )
    }
    
    @Test
    fun `test empty markdown`() = runTest {
        val doc = DocumentFile(
            name = "empty.md",
            path = "empty.md",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = 0L,
                language = "markdown",
                mimeType = "text/markdown"
            )
        )
        
        parser.parse(doc, "")
        
        val results = parser.queryHeading("anything")
        assertTrue(results.isEmpty(), "Should return empty list for empty document")
    }
    
    @Test
    fun `test markdown with code blocks`() = runTest {
        val markdownWithCode = """
            # Code Example
            
            Here's some code:
            
            ```kotlin
            fun hello() {
                println("Hello")
            }
            ```
            
            More text after code.
        """.trimIndent()
        
        val doc = DocumentFile(
            name = "code.md",
            path = "code.md",
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0,
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = 0L,
                fileSize = markdownWithCode.length.toLong(),
                language = "markdown",
                mimeType = "text/markdown"
            )
        )
        
        parser.parse(doc, markdownWithCode)
        
        val results = parser.queryHeading("Code Example")
        assertTrue(results.isNotEmpty())
        assertTrue(results.first().content.contains("println"))
    }
}
