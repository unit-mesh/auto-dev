package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownDocQLExecutorTest {

    private fun createTestDocument(): DocumentFile {
        return DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0,
                fileSize = 1000,
                language = "markdown"
            ),
            toc = emptyList(),
            entities = emptyList()
        )
    }

    private class MockParserService(private val content: String) : DocumentParserService {
        override fun getDocumentContent(): String = content
        override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode = file
        override suspend fun queryHeading(keyword: String): List<DocumentChunk> = emptyList()
        override suspend fun queryChapter(chapterId: String): DocumentChunk? = null
    }

    @Test
    fun `test execute table all query`() = runTest {
        val content = """
            # Table Test
            
            | Name | Age |
            |------|-----|
            | Alice| 20  |
            | Bob  | 30  |
            
            Some text
            
            | ID | Product | Price |
            |----|---------|-------|
            | 1  | Apple   | 1.0   |
        """.trimIndent()
        
        val doc = createTestDocument()
        val parser = MockParserService(content)
        val executor = MarkdownDocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.table[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Tables>(result)
        assertEquals(2, result.itemsByFile[doc.path]?.size)
        
        val firstTable = result.itemsByFile[doc.path]!![0]
        assertEquals(2, firstTable.headers.size)
        assertEquals("Name", firstTable.headers[0])
        assertEquals("Age", firstTable.headers[1])
        assertEquals(2, firstTable.rows.size)
        
        val secondTable = result.itemsByFile[doc.path]!![1]
        assertEquals(3, secondTable.headers.size)
    }

    @Test
    fun `test execute table index query`() = runTest {
        val content = """
            | Name | Age |
            |------|-----|
            | Alice| 20  |
        """.trimIndent()
        
        val doc = createTestDocument()
        val parser = MockParserService(content)
        val executor = MarkdownDocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.table[0]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Tables>(result)
        assertEquals(1, result.itemsByFile[doc.path]?.size)
        assertEquals("Name", result.itemsByFile[doc.path]!![0].headers[0])
    }

    @Test
    fun `test execute table filter by row count`() = runTest {
        val content = """
            | T1 |
            |----|
            | R1 |
            
            | T2 |
            |----|
            | R1 |
            | R2 |
        """.trimIndent()
        
        val doc = createTestDocument()
        val parser = MockParserService(content)
        val executor = MarkdownDocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.table[?(@.rowCount>1)]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Tables>(result)
        assertEquals(1, result.itemsByFile[doc.path]?.size)
        assertEquals(2, result.itemsByFile[doc.path]!![0].rows.size)
    }

    @Test
    fun `test execute table filter by header`() = runTest {
        val content = """
            | Name | Age |
            |------|-----|
            | A    | 1   |
            
            | ID | City |
            |----|------|
            | 1  | NY   |
        """.trimIndent()
        
        val doc = createTestDocument()
        val parser = MockParserService(content)
        val executor = MarkdownDocQLExecutor(doc, parser)
        
        val query = parseDocQL("""$.content.table[?(@.headers~="City")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Tables>(result)
        assertEquals(1, result.itemsByFile[doc.path]?.size)
        assertTrue(result.itemsByFile[doc.path]!![0].headers.contains("City"))
    }

    @Test
    fun `test execute frontmatter query`() = runTest {
        val content = """
            ---
            title: Test Doc
            author: UnitMesh
            tags: [test, docql]
            ---
            
            # Content
        """.trimIndent()
        
        val doc = createTestDocument()
        val parser = MockParserService(content)
        val executor = MarkdownDocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.frontmatter")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Frontmatter>(result)
        assertEquals("Test Doc", result.data["title"])
        assertEquals("UnitMesh", result.data["author"])
        
        val tags = result.data["tags"] as List<*>
        assertEquals(2, tags.size)
        assertTrue(tags.contains("test"))
    }

    @Test
    fun `test execute frontmatter query with no frontmatter`() = runTest {
        val content = """
            # Just Content
            No frontmatter here
        """.trimIndent()
        
        val doc = createTestDocument()
        val parser = MockParserService(content)
        val executor = MarkdownDocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.frontmatter")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Empty>(result)
    }

    @Test
    fun `test execute frontmatter query with invalid frontmatter`() = runTest {
        val content = """
            ---
            invalid: [ yaml
            ---
        """.trimIndent()
        
        val doc = createTestDocument()
        val parser = MockParserService(content)
        val executor = MarkdownDocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.frontmatter")
        val result = executor.execute(query)
        
        // Should return Empty on parse error
        assertIs<DocQLResult.Empty>(result)
    }
}
