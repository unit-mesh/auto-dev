package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals

class BiomeLinterTest {
    @Test
    fun `should parse biome output with diagnostics`() {
        val output = """
{
  "diagnostics": [
    {"severity": "error", "message": "Missing semicolon"},
    {"severity": "warning", "message": "Unused variable"}
  ]
}
        """.trimIndent()

        val filePath = "test.js"
        val issues = BiomeLinter.parseBiomeOutput(output, filePath)

        assertEquals(2, issues.size, "Should parse 2 issues")

        val firstIssue = issues[0]
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, firstIssue.severity)
        assertEquals("Missing semicolon", firstIssue.message)

        val secondIssue = issues[1]
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, secondIssue.severity)
        assertEquals("Unused variable", secondIssue.message)
    }

    @Test
    fun `should handle empty diagnostics`() {
        val output = """{"diagnostics": []}"""
        val issues = BiomeLinter.parseBiomeOutput(output, "test.js")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle non-JSON output`() {
        val output = "Some error message"
        val issues = BiomeLinter.parseBiomeOutput(output, "test.js")
        assertEquals(0, issues.size)
    }
}
