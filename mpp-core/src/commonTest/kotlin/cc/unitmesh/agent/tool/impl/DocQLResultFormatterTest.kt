package cc.unitmesh.agent.tool.impl

import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.Location
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.agent.tool.impl.docql.DocQLResultFormatter
import cc.unitmesh.agent.tool.impl.docql.ScoredResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocQLResultFormatterTest {

    @Test
    fun `test formatScore`() {
        assertEquals("0.95", DocQLResultFormatter.formatScore(0.95123))
        assertEquals("0.90", DocQLResultFormatter.formatScore(0.9))
        assertEquals("1.00", DocQLResultFormatter.formatScore(1.0))
        assertEquals("0.00", DocQLResultFormatter.formatScore(0.0))
        assertEquals("0.12", DocQLResultFormatter.formatScore(0.123456))
    }

    @Test
    fun `test buildQuerySuggestion`() {
        val suggestion = DocQLResultFormatter.buildQuerySuggestion("some query")
        assertTrue(suggestion.contains("Suggestions to find the information"))
        assertTrue(suggestion.contains("$.toc[*]"))
        assertTrue(suggestion.contains("$.content.chunks()"))

        val tocSuggestion = DocQLResultFormatter.buildQuerySuggestion("$.toc[*]")
        assertTrue(!tocSuggestion.contains("1. Try `$.toc[*]`"))
    }

    @Test
    fun `test formatSmartResult with Entities`() {
        val entity = Entity.ClassEntity(
            name = "TestClass",
            packageName = "com.example",
            location = Location("TestClass.kt", 10, 20)
        )
        
        val scoredResult = ScoredResult(
            item = entity,
            score = 0.95,
            uniqueId = "id1",
            preview = "class TestClass",
            filePath = "TestClass.kt"
        )

        val results = listOf(scoredResult)
        val formatted =
            DocQLResultFormatter.formatFallbackResult(
                results = results, keyword = "Test",
                truncated = false
            )

        assertTrue(formatted.contains("## Search Results for 'Test'"))
        assertTrue(formatted.contains("Found 1 relevant items"))
        assertTrue(formatted.contains("## TestClass.kt"))
        // New inline format: class name with line number inline
        assertTrue(formatted.contains("- **Class**: `TestClass:20`"))
        assertTrue(formatted.contains("(0.95)"))
    }

    @Test
    fun `test formatSmartResult with TOCItems`() {
        val tocItem = TOCItem(
            level = 1,
            title = "Introduction",
            anchor = "#intro",
            content = "This is the introduction content."
        )

        val scoredResult = ScoredResult(
            item = tocItem,
            score = 0.88,
            uniqueId = "id2",
            preview = "Introduction",
            filePath = "README.md"
        )

        val formatted = DocQLResultFormatter.formatFallbackResult(
            results = listOf(scoredResult),
            keyword = "intro",
            truncated = true,
            totalCount = 10
        )

        assertTrue(formatted.contains("Showing 1 of 10 results"))
        assertTrue(formatted.contains("## README.md"))
        // New compact format: scoreInfo is now (0.88) instead of (score: 0.88)
        assertTrue(formatted.contains("- **Section**: Introduction (0.88)"))
        assertTrue(formatted.contains("> This is the introduction content."))
    }
}
