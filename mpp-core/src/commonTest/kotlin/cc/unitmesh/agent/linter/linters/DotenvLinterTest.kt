package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotenvLinterTest {
    @Test
    fun `should parse dotenv-linter output correctly`() {
        val output = """
Checking bad.env
bad.env:5 DuplicatedKey: The API_KEY key is duplicated
bad.env:9 UnorderedKey: The APPLE_KEY key should go before the ZEBRA_KEY key
bad.env:12 LowercaseKey: The database_url key should be in uppercase
bad.env:15 SpaceCharacter: The line has spaces around equal sign
bad.env:18 SpaceCharacter: The line has spaces around equal sign
bad.env:18 TrailingWhitespace: Trailing whitespace detected
bad.env:26 ExtraBlankLine: Extra blank line detected

Found 7 problems
        """.trimIndent()

        val filePath = "bad.env"
        val issues = DotenvLinter.parseDotenvLinterOutput(output, filePath)

        assertEquals(7, issues.size, "Should parse 7 issues")

        // Check duplicate key issue (should be ERROR)
        val duplicateIssue = issues.find { it.rule == "DuplicatedKey" }
        assertEquals(5, duplicateIssue?.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, duplicateIssue?.severity)
        assertTrue(duplicateIssue?.message?.contains("API_KEY") == true)

        // Check lowercase key issue (should be ERROR)
        val lowercaseIssue = issues.find { it.rule == "LowercaseKey" }
        assertEquals(12, lowercaseIssue?.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, lowercaseIssue?.severity)
        assertTrue(lowercaseIssue?.message?.contains("uppercase") == true)

        // Check space character issue (should be WARNING)
        val spaceIssue = issues.find { it.rule == "SpaceCharacter" }
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, spaceIssue?.severity)

        // Check trailing whitespace issue
        val trailingIssue = issues.find { it.rule == "TrailingWhitespace" }
        assertEquals(18, trailingIssue?.line)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = DotenvLinter.parseDotenvLinterOutput(output, "test.env")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle no problems output`() {
        val output = """
Checking good.env
No problems found
        """.trimIndent()

        val issues = DotenvLinter.parseDotenvLinterOutput(output, "good.env")
        assertEquals(0, issues.size)
    }
}

