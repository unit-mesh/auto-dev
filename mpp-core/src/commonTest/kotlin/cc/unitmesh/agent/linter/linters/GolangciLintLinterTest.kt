package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GolangciLintLinterTest {
    @Test
    fun `should parse golangci-lint output correctly`() {
        val output = """
bad_go.go:5:2: "os" imported and not used (typecheck)
bad_go.go:6:2: "io" imported and not used (typecheck)
bad_go.go:13:2: declared and not used: y (typecheck)
3 issues:
* typecheck: 3
        """.trimIndent()

        val filePath = "bad_go.go"
        val issues = GolangciLintLinter.parseGolangciLintOutput(output, filePath)

        assertEquals(3, issues.size, "Should parse 3 issues")

        // Check first issue
        val firstIssue = issues[0]
        assertEquals(5, firstIssue.line)
        assertEquals(2, firstIssue.column)
        assertEquals("typecheck", firstIssue.rule)
        assertTrue(firstIssue.message.contains("os"))

        // Check second issue
        val secondIssue = issues[1]
        assertEquals(6, secondIssue.line)
        assertEquals(2, secondIssue.column)
        assertEquals("typecheck", secondIssue.rule)
        assertTrue(secondIssue.message.contains("io"))

        // Check third issue
        val thirdIssue = issues[2]
        assertEquals(13, thirdIssue.line)
        assertEquals(2, thirdIssue.column)
        assertEquals("typecheck", thirdIssue.rule)
        assertTrue(thirdIssue.message.contains("declared and not used"))
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = GolangciLintLinter.parseGolangciLintOutput(output, "test.go")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle output with only summary line`() {
        val output = "0 issues"
        val issues = GolangciLintLinter.parseGolangciLintOutput(output, "test.go")
        assertEquals(0, issues.size)
    }
}

