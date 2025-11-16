package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShellCheckLinterTest {
    @Test
    fun `should parse shellcheck JSON output correctly`() {
        val output = """
[{"file":"bad_script.sh","line":6,"endLine":6,"column":1,"endColumn":11,"level":"warning","code":2034,"message":"UNUSED_VAR appears unused. Verify use (or export if used externally).","fix":null},{"file":"bad_script.sh","line":9,"endLine":9,"column":6,"endColumn":11,"level":"info","code":2086,"message":"Double quote to prevent globbing and word splitting.","fix":null},{"file":"bad_script.sh","line":18,"endLine":18,"column":7,"endColumn":11,"level":"error","code":2154,"message":"var is referenced but not assigned.","fix":null}]
        """.trimIndent()

        val filePath = "bad_script.sh"
        val issues = ShellCheckLinter.parseShellCheckOutput(output, filePath)

        assertEquals(3, issues.size, "Should parse 3 issues")

        // Check first issue (warning)
        val firstIssue = issues[0]
        assertEquals(6, firstIssue.line)
        assertEquals(1, firstIssue.column)
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, firstIssue.severity)
        assertEquals("2034", firstIssue.rule)
        assertTrue(firstIssue.message.contains("UNUSED_VAR"))

        // Check second issue (info)
        val secondIssue = issues[1]
        assertEquals(9, secondIssue.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.INFO, secondIssue.severity)
        assertEquals("2086", secondIssue.rule)

        // Check third issue (error)
        val thirdIssue = issues[2]
        assertEquals(18, thirdIssue.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, thirdIssue.severity)
        assertEquals("2154", thirdIssue.rule)
    }

    @Test
    fun `should handle empty JSON array`() {
        val output = "[]"
        val issues = ShellCheckLinter.parseShellCheckOutput(output, "test.sh")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle non-JSON output`() {
        val output = "Some error message"
        val issues = ShellCheckLinter.parseShellCheckOutput(output, "test.sh")
        assertEquals(0, issues.size)
    }
}
