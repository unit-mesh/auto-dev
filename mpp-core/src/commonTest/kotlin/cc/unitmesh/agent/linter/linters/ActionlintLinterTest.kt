package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionlintLinterTest {
    @Test
    fun `should parse actionlint output correctly`() {
        val output = """
bad-workflow.yml:5:5: unexpected key "branch" for "push" section. expected one of "branches", "branches-ignore", "paths", "paths-ignore", "tags", "tags-ignore", "types", "workflows" [syntax-check]
bad-workflow.yml:8:3: "steps" section is missing in job "test" [syntax-check]
bad-workflow.yml:10:5: unexpected key "step" for "job" section. expected one of "concurrency", "container", "continue-on-error", "defaults", "env", "environment", "if", "name", "needs", "outputs", "permissions", "runs-on", "secrets", "services", "steps", "strategy", "timeout-minutes", "uses", "with" [syntax-check]
bad-workflow.yml:19:15: the runner of "actions/checkout@v3" action is too old to run on GitHub Actions. update the action's version to fix this issue [action]
        """.trimIndent()

        val filePath = "bad-workflow.yml"
        val issues = ActionlintLinter.parseActionlintOutput(output, filePath)

        assertEquals(4, issues.size, "Should parse 4 issues")

        // Check syntax error
        val syntaxIssue = issues[0]
        assertEquals(5, syntaxIssue.line)
        assertEquals(5, syntaxIssue.column)
        assertEquals("syntax-check", syntaxIssue.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, syntaxIssue.severity)
        assertTrue(syntaxIssue.message.contains("unexpected key"))

        // Check missing steps error
        val stepsIssue = issues[1]
        assertEquals(8, stepsIssue.line)
        assertEquals(3, stepsIssue.column)
        assertTrue(stepsIssue.message.contains("steps"))

        // Check warning for old action
        val warningIssue = issues.find { it.message.contains("too old") }
        assertEquals(19, warningIssue?.line)
        assertEquals(15, warningIssue?.column)
        assertEquals("action", warningIssue?.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, warningIssue?.severity)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = ActionlintLinter.parseActionlintOutput(output, "workflow.yml")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean workflow`() {
        val output = "No issues found"
        val issues = ActionlintLinter.parseActionlintOutput(output, "workflow.yml")
        assertEquals(0, issues.size)
    }
}

