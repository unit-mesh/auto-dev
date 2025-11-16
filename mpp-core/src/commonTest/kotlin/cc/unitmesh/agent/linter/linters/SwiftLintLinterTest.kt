package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwiftLintLinterTest {
    @Test
    fun `should parse swiftlint output correctly`() {
        val output = """
Linting Swift files at paths bad.swift
Linting 'bad.swift' (1/1)
/path/to/bad.swift:26:23: error: Force Cast Violation: Force casts should be avoided (force_cast)
/path/to/bad.swift:42:9: warning: Implicit Getter Violation: Computed read-only properties should avoid using the get keyword (implicit_getter)
/path/to/bad.swift:4:1: warning: Line Length Violation: Line should be 120 characters or less; currently it has 124 characters (line_length)
/path/to/bad.swift:35:20: warning: Opening Brace Spacing Violation: Opening braces should be preceded by a single space and on the same line as the declaration (opening_brace)
/path/to/bad.swift:47:1: warning: Trailing Newline Violation: Files should have a single trailing newline (trailing_newline)
Done linting! Found 5 violations, 1 serious in 1 file.
        """.trimIndent()

        val filePath = "bad.swift"
        val issues = SwiftLintLinter.parseSwiftLintOutput(output, filePath)

        assertEquals(5, issues.size, "Should parse 5 issues")

        // Check force cast error
        val forceCastIssue = issues[0]
        assertEquals(26, forceCastIssue.line)
        assertEquals(23, forceCastIssue.column)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, forceCastIssue.severity)
        assertEquals("force_cast", forceCastIssue.rule)
        assertTrue(forceCastIssue.message.contains("Force casts should be avoided"))

        // Check warning
        val getterIssue = issues[1]
        assertEquals(42, getterIssue.line)
        assertEquals(9, getterIssue.column)
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, getterIssue.severity)
        assertEquals("implicit_getter", getterIssue.rule)

        // Check line length warning
        val lineLengthIssue = issues[2]
        assertEquals(4, lineLengthIssue.line)
        assertEquals("line_length", lineLengthIssue.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.WARNING, lineLengthIssue.severity)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = SwiftLintLinter.parseSwiftLintOutput(output, "test.swift")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean swift file`() {
        val output = """
Linting Swift files at paths good.swift
Linting 'good.swift' (1/1)
Done linting! Found 0 violations, 0 serious in 1 file.
        """.trimIndent()
        val issues = SwiftLintLinter.parseSwiftLintOutput(output, "good.swift")
        assertEquals(0, issues.size)
    }
}

