package cc.unitmesh.diagram.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class HtmlLabelParserTest {
    
    @Test
    fun `should detect HTML labels correctly`() {
        assertTrue(HtmlLabelParser.isHtmlLabel("<b>Bold text</b>"))
        assertTrue(HtmlLabelParser.isHtmlLabel("<b>旅行代理</b><br/>协调预订航班、酒店和支付"))
        assertTrue(HtmlLabelParser.isHtmlLabel("Text with <br/> tag"))
        assertFalse(HtmlLabelParser.isHtmlLabel("Plain text"))
        assertFalse(HtmlLabelParser.isHtmlLabel("Text without tags"))
    }
    
    @Test
    fun `should parse simple HTML labels`() {
        val htmlLabel = "<b>Bold text</b>"
        val result = HtmlLabelParser.parseHtmlLabel(htmlLabel)
        assertEquals("**Bold text**", result)
    }
    
    @Test
    fun `should parse HTML labels with line breaks`() {
        val htmlLabel = "<b>旅行代理</b><br/>协调预订航班、酒店和支付"
        val result = HtmlLabelParser.parseHtmlLabel(htmlLabel)
        assertTrue(result.contains("**旅行代理**"))
        assertTrue(result.contains("协调预订航班、酒店和支付"))
        assertTrue(result.contains("\n"))
    }
    
    @Test
    fun `should parse complex HTML labels`() {
        val htmlLabel = """
            <b>航班代理</b><br/>技能: findFlights(query: object): flightOptions
        """.trimIndent()
        val result = HtmlLabelParser.parseHtmlLabel(htmlLabel)
        assertTrue(result.contains("**航班代理**"))
        assertTrue(result.contains("findFlights"))
        assertTrue(result.contains("flightOptions"))
    }
    
    @Test
    fun `should handle HTML labels wrapped in angle brackets`() {
        val htmlLabel = "<b>旅行代理</b><br/>协调预订航班、酒店和支付"
        val wrappedLabel = "<$htmlLabel>"
        val result = HtmlLabelParser.parseHtmlLabel(wrappedLabel)
        assertTrue(result.contains("**旅行代理**"))
        assertTrue(result.contains("协调预订航班、酒店和支付"))
    }
    
    @Test
    fun `should parse structured HTML information`() {
        val htmlLabel = "<b>旅行代理</b><br/>协调预订航班、酒店和支付"
        val info = HtmlLabelParser.parseStructuredHtml(htmlLabel)
        
        assertEquals("旅行代理", info.title)
        assertEquals("协调预订航班、酒店和支付", info.description)
        assertTrue(info.plainText.contains("旅行代理"))
        assertTrue(info.plainText.contains("协调预订航班、酒店和支付"))
        assertEquals(htmlLabel, info.originalHtml)
    }
    
    @Test
    fun `should handle HTML with multiple formatting tags`() {
        val htmlLabel = "<b>Bold</b> and <i>italic</i> and <u>underlined</u> text"
        val result = HtmlLabelParser.parseHtmlLabel(htmlLabel)
        assertTrue(result.contains("**Bold**"))
        assertTrue(result.contains("*italic*"))
        assertTrue(result.contains("_underlined_"))
    }
    
    @Test
    fun `should remove unknown HTML tags but keep content`() {
        val htmlLabel = "<span>Some text</span> with <div>other content</div>"
        val result = HtmlLabelParser.parseHtmlLabel(htmlLabel)
        assertEquals("Some text with other content", result)
    }
    
    @Test
    fun `should handle empty or blank labels`() {
        assertEquals("", HtmlLabelParser.parseHtmlLabel(""))
        assertEquals("", HtmlLabelParser.parseHtmlLabel("   "))
        assertEquals("", HtmlLabelParser.parseHtmlLabel("<>"))
    }
    
    @Test
    fun `should clean up extra whitespace`() {
        val htmlLabel = "<b>Title</b><br/>   Multiple   spaces   here   "
        val result = HtmlLabelParser.parseHtmlLabel(htmlLabel)
        assertFalse(result.contains("   "))
        assertTrue(result.contains("**Title**"))
        assertTrue(result.contains("Multiple spaces here"))
    }
    
    @Test
    fun `should handle HTML without title in structured parsing`() {
        val htmlLabel = "Just some text<br/>with a line break"
        val info = HtmlLabelParser.parseStructuredHtml(htmlLabel)
        
        assertNull(info.title)
        assertEquals("with a line break", info.description)
        assertTrue(info.plainText.contains("Just some text"))
    }
    
    @Test
    fun `should handle HTML with only title in structured parsing`() {
        val htmlLabel = "<b>Only Title</b>"
        val info = HtmlLabelParser.parseStructuredHtml(htmlLabel)
        
        assertEquals("Only Title", info.title)
        assertNull(info.description)
        assertEquals("**Only Title**", info.plainText)
    }
}
