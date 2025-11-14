package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetektLinterTest {
    @Test
    fun `should parse detekt output correctly`() {
        val output = """
/Volumes/source/ai/autocrud/docs/test-scripts/linter-tests/BadKotlin.kt:1:1: The package declaration does not match the actual file location. [InvalidPackageDeclaration]
/Volumes/source/ai/autocrud/docs/test-scripts/linter-tests/BadKotlin.kt:7:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
/Volumes/source/ai/autocrud/docs/test-scripts/linter-tests/BadKotlin.kt:17:13: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
/Volumes/source/ai/autocrud/docs/test-scripts/linter-tests/BadKotlin.kt:10:41: Function parameter `param2` is unused. [UnusedParameter]
Analysis failed with 4 weighted issues.
        """.trimIndent()

        val filePath = "BadKotlin.kt"
        val issues = DetektLinter.parseDetektOutput(output, filePath)

        assertEquals(4, issues.size, "Should parse 4 issues")

        // Check first issue
        val firstIssue = issues[0]
        assertEquals(1, firstIssue.line)
        assertEquals(1, firstIssue.column)
        assertEquals("InvalidPackageDeclaration", firstIssue.rule)
        assertTrue(firstIssue.message.contains("package declaration"))

        // Check second issue
        val secondIssue = issues[1]
        assertEquals(7, secondIssue.line)
        assertEquals(1, secondIssue.column)
        assertEquals("MaxLineLength", secondIssue.rule)

        // Check third issue
        val thirdIssue = issues[2]
        assertEquals(17, thirdIssue.line)
        assertEquals(13, thirdIssue.column)
        assertEquals("MagicNumber", thirdIssue.rule)

        // Check fourth issue
        val fourthIssue = issues[3]
        assertEquals(10, fourthIssue.line)
        assertEquals(41, fourthIssue.column)
        assertEquals("UnusedParameter", fourthIssue.rule)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = DetektLinter.parseDetektOutput(output, "test.kt")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle output with only summary line`() {
        val output = "Analysis failed with 0 weighted issues."
        val issues = DetektLinter.parseDetektOutput(output, "test.kt")
        assertEquals(0, issues.size)
    }
}
