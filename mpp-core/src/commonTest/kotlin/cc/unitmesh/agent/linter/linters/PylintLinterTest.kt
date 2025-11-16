package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PylintLinterTest {
    @Test
    fun `should parse pylint output correctly`() {
        val output = """
************* Module bad
bad.py:18:0: C0303: Trailing whitespace (trailing-whitespace)
bad.py:24:0: C0301: Line too long (146/100) (line-too-long)
bad.py:12:4: W0612: Unused variable 'y' (unused-variable)
bad.py:19:21: W0613: Unused argument 'arg1' (unused-argument)
bad.py:15:0: R0903: Too few public methods (1/2) (too-few-public-methods)
bad.py:35:0: C0103: Constant name "InvalidName" doesn't conform to UPPER_CASE naming style (invalid-name)
bad.py:4:0: W0611: Unused import os (unused-import)

-----------------------------------
Your code has been rated at 1.88/10
        """.trimIndent()

        val filePath = "bad.py"
        val issues = PylintLinter.parsePylintOutput(output, filePath)

        assertEquals(7, issues.size, "Should parse 7 issues")

        // Check convention issue (INFO)
        val conventionIssue = issues[0]
        assertEquals(18, conventionIssue.line)
        assertEquals(0, conventionIssue.column)
        assertEquals("trailing-whitespace", conventionIssue.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.INFO, conventionIssue.severity)
        assertTrue(conventionIssue.message.contains("Trailing whitespace"))

        // Check warning issue
        val warningIssue = issues.find { it.rule == "unused-variable" }
        assertEquals(12, warningIssue?.line)
        assertEquals(4, warningIssue?.column)
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, warningIssue?.severity)
        assertTrue(warningIssue?.message?.contains("Unused variable") == true)

        // Check refactor issue (INFO)
        val refactorIssue = issues.find { it.rule == "too-few-public-methods" }
        assertEquals(15, refactorIssue?.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.INFO, refactorIssue?.severity)

        // Check naming issue
        val namingIssue = issues.find { it.rule == "invalid-name" }
        assertEquals(35, namingIssue?.line)
        assertTrue(namingIssue?.message?.contains("InvalidName") == true)

        // Check unused import
        val importIssue = issues.find { it.rule == "unused-import" }
        assertEquals(4, importIssue?.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, importIssue?.severity)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = PylintLinter.parsePylintOutput(output, "test.py")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle perfect score`() {
        val output = """
--------------------------------------------------------------------
Your code has been rated at 10.00/10 (previous run: 10.00/10, +0.00)
        """.trimIndent()
        val issues = PylintLinter.parsePylintOutput(output, "test.py")
        assertEquals(0, issues.size)
    }
}

