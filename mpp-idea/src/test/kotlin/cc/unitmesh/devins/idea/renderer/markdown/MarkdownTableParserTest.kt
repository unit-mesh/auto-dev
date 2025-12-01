package cc.unitmesh.devins.idea.renderer.markdown

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Markdown table parsing using intellij-markdown GFM parser.
 * These tests verify that the parser correctly identifies table structure.
 */
class MarkdownTableParserTest {

    private val flavour = GFMFlavourDescriptor()
    private val parser = MarkdownParser(flavour)

    @Test
    fun `should parse simple table with header and rows`() {
        val markdown = """
            | Header 1 | Header 2 | Header 3 |
            |----------|----------|----------|
            | Cell 1   | Cell 2   | Cell 3   |
            | Cell 4   | Cell 5   | Cell 6   |
        """.trimIndent()

        val tree = parser.buildMarkdownTreeFromString(markdown)
        val tableNode = findTableNode(tree)

        assertNotNull(tableNode, "Table node should be found")
        assertEquals(GFMElementTypes.TABLE, tableNode.type)

        // Find header
        val headerNode = tableNode.children.find { it.type == GFMElementTypes.HEADER }
        assertNotNull(headerNode, "Header node should be found")

        // Count header cells
        val headerCells = headerNode.children.filter { it.type == GFMTokenTypes.CELL }
        assertEquals(3, headerCells.size, "Should have 3 header cells")

        // Find body rows
        val bodyRows = tableNode.children.filter { it.type == GFMElementTypes.ROW }
        assertEquals(2, bodyRows.size, "Should have 2 body rows")

        // Verify first row cells
        val firstRowCells = bodyRows[0].children.filter { it.type == GFMTokenTypes.CELL }
        assertEquals(3, firstRowCells.size, "First row should have 3 cells")
    }

    @Test
    fun `should extract cell text correctly`() {
        val markdown = """
            | Name | Age | City |
            |------|-----|------|
            | Alice | 30 | NYC |
        """.trimIndent()

        val tree = parser.buildMarkdownTreeFromString(markdown)
        val tableNode = findTableNode(tree)
        assertNotNull(tableNode)

        val headerNode = tableNode.children.find { it.type == GFMElementTypes.HEADER }
        assertNotNull(headerNode)

        val headerCells = headerNode.children.filter { it.type == GFMTokenTypes.CELL }
        val headerTexts = headerCells.map { extractCellText(it, markdown) }

        assertEquals(listOf("Name", "Age", "City"), headerTexts)

        val bodyRow = tableNode.children.find { it.type == GFMElementTypes.ROW }
        assertNotNull(bodyRow)

        val bodyCells = bodyRow.children.filter { it.type == GFMTokenTypes.CELL }
        val bodyTexts = bodyCells.map { extractCellText(it, markdown) }

        assertEquals(listOf("Alice", "30", "NYC"), bodyTexts)
    }

    @Test
    fun `should handle table with inline formatting`() {
        val markdown = """
            | Feature | Status |
            |---------|--------|
            | **Bold** | `code` |
            | *Italic* | ~~strike~~ |
        """.trimIndent()

        val tree = parser.buildMarkdownTreeFromString(markdown)
        val tableNode = findTableNode(tree)
        assertNotNull(tableNode)

        val bodyRows = tableNode.children.filter { it.type == GFMElementTypes.ROW }
        assertEquals(2, bodyRows.size)
    }

    @Test
    fun `should calculate column count from header`() {
        val markdown = """
            | A | B | C | D | E |
            |---|---|---|---|---|
            | 1 | 2 | 3 | 4 | 5 |
        """.trimIndent()

        val tree = parser.buildMarkdownTreeFromString(markdown)
        val tableNode = findTableNode(tree)
        assertNotNull(tableNode)

        val headerNode = tableNode.children.find { it.type == GFMElementTypes.HEADER }
        assertNotNull(headerNode)

        val columnCount = headerNode.children.count { it.type == GFMTokenTypes.CELL }
        assertEquals(5, columnCount)
    }

    @Test
    fun `should find table separator`() {
        val markdown = """
            | H1 | H2 |
            |----|-----|
            | C1 | C2 |
        """.trimIndent()

        val tree = parser.buildMarkdownTreeFromString(markdown)
        val tableNode = findTableNode(tree)
        assertNotNull(tableNode)

        val hasSeparator = tableNode.children.any { it.type == GFMTokenTypes.TABLE_SEPARATOR }
        assertTrue(hasSeparator, "Table should have separator")
    }

    private fun findTableNode(node: ASTNode): ASTNode? {
        if (node.type == GFMElementTypes.TABLE) return node
        for (child in node.children) {
            val found = findTableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun extractCellText(cell: ASTNode, content: String): String {
        return cell.getTextInNode(content).toString()
            .replace("|", "")
            .replace("`", "")
            .replace("**", "")
            .replace("*", "")
            .trim()
    }
}

