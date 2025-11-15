package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ESLintLinterTest {
    @Test
    fun `should parse eslint output correctly`() {
        val output = """
/Volumes/source/ai/autocrud/docs/test-scripts/linter-tests/bad.js
   3:1   error    Unexpected var, use let or const instead     no-var
   4:15  error    Missing semicolon                            semi
   9:13  warning  'z' is assigned a value but never used       no-unused-vars
  18:7   warning  'unused' is assigned a value but never used  no-unused-vars
  21:7   error    Expected '===' and instead saw '=='          eqeqeq
  33:1   warning  Unexpected console statement                 no-console

✖ 6 problems (3 errors, 3 warnings)
        """.trimIndent()

        val filePath = "bad.js"
        val issues = ESLintLinter.parseESLintOutput(output, filePath)

        assertEquals(6, issues.size, "Should parse 6 issues")

        // Check error issue
        val varIssue = issues[0]
        assertEquals(3, varIssue.line)
        assertEquals(1, varIssue.column)
        assertEquals("no-var", varIssue.rule)
        assertEquals(LintSeverity.ERROR, varIssue.severity)
        assertTrue(varIssue.message.contains("Unexpected var"))

        // Check semicolon error
        val semiIssue = issues[1]
        assertEquals(4, semiIssue.line)
        assertEquals(15, semiIssue.column)
        assertEquals("semi", semiIssue.rule)
        assertEquals(LintSeverity.ERROR, semiIssue.severity)

        // Check warning issue
        val unusedIssue = issues.find { it.line == 9 }
        assertEquals(LintSeverity.WARNING, unusedIssue?.severity)
        assertEquals("no-unused-vars", unusedIssue?.rule)

        // Check eqeqeq issue
        val eqIssue = issues.find { it.rule == "eqeqeq" }
        assertEquals(21, eqIssue?.line)
        assertEquals(LintSeverity.ERROR, eqIssue?.severity)

        // Check console warning
        val consoleIssue = issues.find { it.rule == "no-console" }
        assertEquals(33, consoleIssue?.line)
        assertEquals(LintSeverity.WARNING, consoleIssue?.severity)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = ESLintLinter.parseESLintOutput(output, "test.js")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean code`() {
        val output = """
test.js

✔ 0 problems
        """.trimIndent()
        val issues = ESLintLinter.parseESLintOutput(output, "test.js")
        assertEquals(0, issues.size)
    }
}

