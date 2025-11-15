package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SQLFluffLinterTest {
    @Test
    fun `should parse sqlfluff output correctly`() {
        val output = """
== [bad_sql.sql] FAIL
L:   3 | P:   1 | AM04 | Query produces an unknown number of result columns.
L:   3 | P:  21 | LT14 | The 'WHERE' keyword should always start a new line.
L:   3 | P:  29 | LT01 | Expected single whitespace between naked identifier and raw comparison operator '='.
L:   5 | P:   1 | CP01 | Keywords must be consistently upper case.
L:   8 | P:   1 |  PRS | Line 8, Position 1: Found unparsable section
L:  14 | P:   1 | LT05 | Line is too long (134 > 80).
All Finished!
        """.trimIndent()

        val filePath = "bad_sql.sql"
        val issues = SQLFluffLinter.parseSQLFluffOutput(output, filePath)

        assertEquals(6, issues.size, "Should parse 6 issues")

        // Check first issue
        val firstIssue = issues[0]
        assertEquals(3, firstIssue.line)
        assertEquals(1, firstIssue.column)
        assertEquals("AM04", firstIssue.rule)
        assertTrue(firstIssue.message.contains("Query produces"))

        // Check parse error issue (should be ERROR severity)
        val parseError = issues.find { it.rule == "PRS" }
        assertEquals(8, parseError?.line)
        assertTrue(parseError?.message?.contains("unparsable") == true)

        // Check layout issue
        val layoutIssue = issues.find { it.rule == "LT05" }
        assertEquals(14, layoutIssue?.line)
        assertTrue(layoutIssue?.message?.contains("Line is too long") == true)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = SQLFluffLinter.parseSQLFluffOutput(output, "test.sql")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle output with only summary line`() {
        val output = "All Finished!"
        val issues = SQLFluffLinter.parseSQLFluffOutput(output, "test.sql")
        assertEquals(0, issues.size)
    }
}

