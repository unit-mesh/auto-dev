package cc.unitmesh.devti.language.agenttool.scrapy;

import cc.unitmesh.devti.language.agenttool.browse.DocumentCleaner
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class DocumentCleanerTest {

    @Test
    fun shouldCleanHtmlWithValidTitleAndDescription() {
        // Given
        val html = "<html><head><title>Test Title</title><meta http-equiv=\"Content-Language\" content=\"en\"></head><body><p>Test Text</p></body></html>"

        // When
        val documentContent = DocumentCleaner().cleanHtml(html)

        // Then
        assertEquals("Test Title", documentContent.title)
        assertEquals("en", documentContent.language)
        assertEquals("Test Text", documentContent.body)
        assertNull(documentContent.description)
    }

    @Test
    fun shouldCleanHtmlWithValidMetaDescription() {
        // Given
        val html = "<html><head><title>Test Title</title><meta name=\"description\" content=\"Test Description\"></head><body><p>Test Text</p></body></html>"

        // When
        val documentContent = DocumentCleaner().cleanHtml(html)

        // Then
        assertEquals("Test Title", documentContent.title)
        assertNull(documentContent.language)
        assertEquals("Test Description", documentContent.description)
        assertEquals("Test Text", documentContent.body)
    }

    @Test
    fun `test articleNode with valid document`() {
        // Given
        val html = """
            <html>
                <body>
                    <div itemprop="articleBody">This is the article body</div>
                </body>
            </html>
        """.trimIndent()

        // When
        val documentContent = DocumentCleaner().cleanHtml(html)

        // Then
        assertEquals("This is the article body", documentContent.body)
    }
}
