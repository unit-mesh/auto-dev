package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HadolintLinterTest {
    @Test
    fun `should parse hadolint output correctly`() {
        val output = """
Dockerfile.bad:3 DL3006 warning: Always tag the version of an image explicitly
Dockerfile.bad:4 DL4000 error: MAINTAINER is deprecated
Dockerfile.bad:7 DL3008 warning: Pin versions in apt get install. Instead of `apt-get install <package>` use `apt-get install <package>=<version>`
Dockerfile.bad:7 DL3015 info: Avoid additional packages by specifying `--no-install-recommends`
Dockerfile.bad:10 DL3004 error: Do not use sudo as it leads to unpredictable behavior. Use a tool like gosu to enforce root
Dockerfile.bad:26 DL3020 error: Use COPY instead of ADD for files and folders
        """.trimIndent()

        val filePath = "Dockerfile.bad"
        val issues = HadolintLinter.parseHadolintOutput(output, filePath)

        assertEquals(6, issues.size, "Should parse 6 issues")

        // Check warning issue
        val warningIssue = issues[0]
        assertEquals(3, warningIssue.line)
        assertEquals("DL3006", warningIssue.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, warningIssue.severity)
        assertTrue(warningIssue.message.contains("tag the version"))

        // Check error issue
        val errorIssue = issues[1]
        assertEquals(4, errorIssue.line)
        assertEquals("DL4000", errorIssue.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, errorIssue.severity)
        assertTrue(errorIssue.message.contains("MAINTAINER"))

        // Check info issue
        val infoIssue = issues.find { it.rule == "DL3015" }
        assertEquals(7, infoIssue?.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.INFO, infoIssue?.severity)

        // Check sudo error
        val sudoIssue = issues.find { it.rule == "DL3004" }
        assertEquals(10, sudoIssue?.line)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, sudoIssue?.severity)
        assertTrue(sudoIssue?.message?.contains("sudo") == true)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = HadolintLinter.parseHadolintOutput(output, "Dockerfile")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean dockerfile`() {
        val output = "No issues found"
        val issues = HadolintLinter.parseHadolintOutput(output, "Dockerfile")
        assertEquals(0, issues.size)
    }
}

