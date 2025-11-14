package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PMDLinterTest {
    @Test
    fun `should parse PMD output correctly`() {
        val output = """
[WARN] Progressbar rendering conflicts with reporting to STDOUT.
BadJava.java:7: UnusedLocalVariable:    Avoid unused local variables such as 'x'.
BadJava.java:8: UnusedLocalVariable:    Avoid unused local variables such as 'y'.
BadJava.java:13:        UnconditionalIfStatement:       Do not use if statements that are always true or always false
BadJava.java:13:        EmptyControlStatement:  Empty if statement
        """.trimIndent()

        val filePath = "BadJava.java"
        val issues = PMDLinter.parsePMDOutput(output, filePath)

        assertEquals(4, issues.size, "Should parse 4 issues")

        // Check first issue
        val firstIssue = issues[0]
        assertEquals(7, firstIssue.line)
        assertEquals(0, firstIssue.column)
        assertEquals("UnusedLocalVariable", firstIssue.rule)
        assertTrue(firstIssue.message.contains("unused local variables"))

        // Check second issue
        val secondIssue = issues[1]
        assertEquals(8, secondIssue.line)
        assertEquals(0, secondIssue.column)
        assertEquals("UnusedLocalVariable", secondIssue.rule)

        // Check third issue
        val thirdIssue = issues[2]
        assertEquals(13, thirdIssue.line)
        assertEquals(0, thirdIssue.column)
        assertEquals("UnconditionalIfStatement", thirdIssue.rule)

        // Check fourth issue
        val fourthIssue = issues[3]
        assertEquals(13, fourthIssue.line)
        assertEquals(0, fourthIssue.column)
        assertEquals("EmptyControlStatement", fourthIssue.rule)
    }

    @Test
    fun `should skip warning lines`() {
        val output = """
[WARN] Some warning message
[INFO] Some info message
BadJava.java:7: UnusedLocalVariable:    Avoid unused local variables
        """.trimIndent()

        val issues = PMDLinter.parsePMDOutput(output, "test.java")
        assertEquals(1, issues.size)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = PMDLinter.parsePMDOutput(output, "test.java")
        assertEquals(0, issues.size)
    }
}
