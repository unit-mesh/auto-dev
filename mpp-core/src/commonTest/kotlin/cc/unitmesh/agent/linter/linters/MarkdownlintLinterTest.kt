package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownlintLinterTest {
    @Test
    fun `should parse markdownlint output correctly`() {
        val output = """
bad.md:5 MD022/blanks-around-headings Headings should be surrounded by blank lines [Expected: 1; Actual: 0; Below] [Context: "## heading without blank line before"]
bad.md:18:81 MD013/line-length Line length [Expected: 80; Actual: 148]
bad.md:22 MD012/no-multiple-blanks Multiple consecutive blank lines [Expected: 1; Actual: 2]
bad.md:24:31 MD009/no-trailing-spaces Trailing spaces [Expected: 0 or 2; Actual: 4]
bad.md:36:1 MD033/no-inline-html Inline HTML [Element: div]
        """.trimIndent()

        val filePath = "bad.md"
        val issues = MarkdownlintLinter.parseMarkdownlintOutput(output, filePath)

        assertEquals(5, issues.size, "Should parse 5 issues")

        // Check issue without column
        val firstIssue = issues[0]
        assertEquals(5, firstIssue.line)
        assertEquals(1, firstIssue.column) // default to 1 when no column
        assertEquals("MD022", firstIssue.rule)
        assertTrue(firstIssue.message.contains("Headings should be surrounded"))

        // Check issue with column
        val secondIssue = issues[1]
        assertEquals(18, secondIssue.line)
        assertEquals(81, secondIssue.column)
        assertEquals("MD013", secondIssue.rule)
        assertTrue(secondIssue.message.contains("Line length"))

        // Check multiple blanks issue
        val blankIssue = issues.find { it.rule == "MD012" }
        assertEquals(22, blankIssue?.line)
        assertTrue(blankIssue?.message?.contains("Multiple consecutive") == true)

        // Check trailing spaces issue
        val trailingIssue = issues.find { it.rule == "MD009" }
        assertEquals(24, trailingIssue?.line)
        assertEquals(31, trailingIssue?.column)

        // Check inline HTML issue
        val htmlIssue = issues.find { it.rule == "MD033" }
        assertEquals(36, htmlIssue?.line)
        assertTrue(htmlIssue?.message?.contains("Inline HTML") == true)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = MarkdownlintLinter.parseMarkdownlintOutput(output, "test.md")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean markdown`() {
        val output = "All checks passed"
        val issues = MarkdownlintLinter.parseMarkdownlintOutput(output, "test.md")
        assertEquals(0, issues.size)
    }
}

