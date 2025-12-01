package cc.unitmesh.devins.idea.renderer.markdown

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for MarkdownTextParser utility functions.
 * These tests verify text extraction from Markdown AST nodes.
 */
class MarkdownTextParserTest {

    private val flavour = GFMFlavourDescriptor()
    private val parser = MarkdownParser(flavour)

    // ============ Header Text Extraction Tests ============

    @Test
    fun `should extract ATX header text level 1`() {
        val markdown = "# Hello World"
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val headerNode = findNodeOfType(tree, MarkdownElementTypes.ATX_1)

        assertNotNull(headerNode)
        val text = MarkdownTextParser.extractHeaderText(headerNode, markdown)
        assertEquals("Hello World", text)
    }

    @Test
    fun `should extract ATX header text level 2`() {
        val markdown = "## Section Title"
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val headerNode = findNodeOfType(tree, MarkdownElementTypes.ATX_2)

        assertNotNull(headerNode)
        val text = MarkdownTextParser.extractHeaderText(headerNode, markdown)
        assertEquals("Section Title", text)
    }

    @Test
    fun `should extract ATX header text level 3`() {
        val markdown = "### Subsection"
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val headerNode = findNodeOfType(tree, MarkdownElementTypes.ATX_3)

        assertNotNull(headerNode)
        val text = MarkdownTextParser.extractHeaderText(headerNode, markdown)
        assertEquals("Subsection", text)
    }

    @Test
    fun `should extract SETEXT header text level 1`() {
        val markdown = """
            Main Title
            ==========
        """.trimIndent()
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val headerNode = findNodeOfType(tree, MarkdownElementTypes.SETEXT_1)

        assertNotNull(headerNode)
        val text = MarkdownTextParser.extractHeaderText(headerNode, markdown)
        assertEquals("Main Title", text)
    }

    @Test
    fun `should extract SETEXT header text level 2`() {
        val markdown = """
            Sub Title
            ---------
        """.trimIndent()
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val headerNode = findNodeOfType(tree, MarkdownElementTypes.SETEXT_2)

        assertNotNull(headerNode)
        val text = MarkdownTextParser.extractHeaderText(headerNode, markdown)
        assertEquals("Sub Title", text)
    }

    // ============ Code Fence Content Extraction Tests ============

    @Test
    fun `should extract code fence content without language`() {
        val markdown = """
            ```
            val x = 1
            val y = 2
            ```
        """.trimIndent()
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val codeFenceNode = findNodeOfType(tree, MarkdownElementTypes.CODE_FENCE)

        assertNotNull(codeFenceNode)
        val content = MarkdownTextParser.extractCodeFenceContent(codeFenceNode, markdown)
        assertEquals("val x = 1\nval y = 2", content)
    }

    @Test
    fun `should extract code fence content with language`() {
        val markdown = """
            ```kotlin
            fun hello() = println("Hello")
            ```
        """.trimIndent()
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val codeFenceNode = findNodeOfType(tree, MarkdownElementTypes.CODE_FENCE)

        assertNotNull(codeFenceNode)
        val content = MarkdownTextParser.extractCodeFenceContent(codeFenceNode, markdown)
        assertEquals("fun hello() = println(\"Hello\")", content)
    }

    @Test
    fun `should extract code fence language`() {
        val markdown = """
            ```javascript
            console.log("test");
            ```
        """.trimIndent()
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val codeFenceNode = findNodeOfType(tree, MarkdownElementTypes.CODE_FENCE)

        assertNotNull(codeFenceNode)
        val language = MarkdownTextParser.extractCodeFenceLanguage(codeFenceNode, markdown)
        assertEquals("javascript", language)
    }

    @Test
    fun `should return null for code fence without language`() {
        val markdown = """
            ```
            some code
            ```
        """.trimIndent()
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val codeFenceNode = findNodeOfType(tree, MarkdownElementTypes.CODE_FENCE)

        assertNotNull(codeFenceNode)
        val language = MarkdownTextParser.extractCodeFenceLanguage(codeFenceNode, markdown)
        assertNull(language)
    }

    // ============ Code Span Text Extraction Tests ============

    @Test
    fun `should extract inline code span text`() {
        val markdown = "Use `println()` to print"
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val codeSpanNode = findNodeOfType(tree, MarkdownElementTypes.CODE_SPAN)

        assertNotNull(codeSpanNode)
        val text = MarkdownTextParser.extractCodeSpanText(codeSpanNode, markdown)
        assertEquals("println()", text)
    }

    // ============ Helper Function ============

    private fun findNodeOfType(node: ASTNode, type: org.intellij.markdown.IElementType): ASTNode? {
        if (node.type == type) return node
        for (child in node.children) {
            val found = findNodeOfType(child, type)
            if (found != null) return found
        }
        return null
    }
}

