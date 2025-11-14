package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuffLinterTest {
//    @Test
    fun `should parse ruff JSON output correctly`() {
        val output = """
[
  {
    "cell": null,
    "code": "F841",
    "end_location": {
      "column": 6,
      "row": 2
    },
    "filename": "bad_python.py",
    "fix": null,
    "location": {
      "column": 5,
      "row": 2
    },
    "message": "Local variable `x` is assigned to but never used",
    "noqa_row": 2,
    "url": "https://docs.astral.sh/ruff/rules/unused-variable"
  },
  {
    "cell": null,
    "code": "F841",
    "end_location": {
      "column": 6,
      "row": 3
    },
    "filename": "bad_python.py",
    "fix": null,
    "location": {
      "column": 5,
      "row": 3
    },
    "message": "Local variable `y` is assigned to but never used",
    "noqa_row": 3,
    "url": "https://docs.astral.sh/ruff/rules/unused-variable"
  }
]
        """.trimIndent()

        val filePath = "bad_python.py"
        val issues = RuffLinter.parseRuffOutput(output, filePath)

        assertEquals(2, issues.size, "Should parse 2 issues")

        // Check first issue
        val firstIssue = issues[0]
        assertEquals(2, firstIssue.line)
        assertEquals(5, firstIssue.column)
        assertEquals("F841", firstIssue.rule)
        assertTrue(firstIssue.message.contains("Local variable"))

        // Check second issue
        val secondIssue = issues[1]
        assertEquals(3, secondIssue.line)
        assertEquals(5, secondIssue.column)
        assertEquals("F841", secondIssue.rule)
    }

    @Test
    fun `should handle empty JSON array`() {
        val output = "[]"
        val issues = RuffLinter.parseRuffOutput(output, "test.py")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle non-JSON output`() {
        val output = "Some error message"
        val issues = RuffLinter.parseRuffOutput(output, "test.py")
        assertEquals(0, issues.size)
    }
}
