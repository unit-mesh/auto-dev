package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamllintLinterTest {
    @Test
    fun `should parse yamllint output correctly`() {
        val output = """
bad.yaml
  4:25      error    trailing spaces  (trailing-spaces)
  16:81     error    line too long (130 > 80 characters)  (line-length)
  19:10     warning  truthy value should be one of [false, true]  (truthy)
  20:11     warning  truthy value should be one of [false, true]  (truthy)
  27:1      error    duplication of key "duplicate" in mapping  (key-duplicates)
        """.trimIndent()

        val filePath = "bad.yaml"
        val issues = YamllintLinter.parseYamllintOutput(output, filePath)

        assertEquals(5, issues.size, "Should parse 5 issues")

        // Check error issue
        val trailingIssue = issues[0]
        assertEquals(4, trailingIssue.line)
        assertEquals(25, trailingIssue.column)
        assertEquals("trailing-spaces", trailingIssue.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, trailingIssue.severity)
        assertTrue(trailingIssue.message.contains("trailing spaces"))

        // Check line length error
        val lineLengthIssue = issues[1]
        assertEquals(16, lineLengthIssue.line)
        assertEquals(81, lineLengthIssue.column)
        assertEquals("line-length", lineLengthIssue.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, lineLengthIssue.severity)

        // Check warning issue
        val truthyIssue = issues.find { it.rule == "truthy" }
        assertEquals(19, truthyIssue?.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, truthyIssue?.severity)
        assertTrue(truthyIssue?.message?.contains("truthy value") == true)

        // Check duplicate key error
        val duplicateIssue = issues.find { it.rule == "key-duplicates" }
        assertEquals(27, duplicateIssue?.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, duplicateIssue?.severity)
        assertTrue(duplicateIssue?.message?.contains("duplication") == true)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = YamllintLinter.parseYamllintOutput(output, "test.yaml")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean yaml`() {
        val output = """
test.yaml
        """.trimIndent()
        val issues = YamllintLinter.parseYamllintOutput(output, "test.yaml")
        assertEquals(0, issues.size)
    }
}

