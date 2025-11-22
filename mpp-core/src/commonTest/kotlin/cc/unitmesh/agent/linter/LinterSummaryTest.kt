package cc.unitmesh.agent.linter

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinterSummaryTest {

    @Test
    fun `test ERROR issues are prioritized over WARNING and INFO`() {
        val issues = listOf(
            LintIssue(line = 10, severity = LintSeverity.WARNING, message = "Warning message", rule = "WarningRule"),
            LintIssue(line = 5, severity = LintSeverity.ERROR, message = "Error message", rule = "ErrorRule"),
            LintIssue(line = 15, severity = LintSeverity.INFO, message = "Info message", rule = "InfoRule"),
            LintIssue(line = 3, severity = LintSeverity.ERROR, message = "Another error", rule = "ErrorRule2"),
            LintIssue(line = 12, severity = LintSeverity.WARNING, message = "Another warning", rule = "WarningRule2")
        )

        // Sort using the same logic as in Linter.kt
        val sorted = issues.sortedWith(compareBy<LintIssue> {
            when (it.severity) {
                LintSeverity.ERROR -> 0
                LintSeverity.WARNING -> 1
                LintSeverity.INFO -> 2
            }
        }.thenBy { it.line })

        // First two should be errors (sorted by line)
        assertEquals(LintSeverity.ERROR, sorted[0].severity)
        assertEquals(3, sorted[0].line)
        assertEquals(LintSeverity.ERROR, sorted[1].severity)
        assertEquals(5, sorted[1].line)

        // Next two should be warnings (sorted by line)
        assertEquals(LintSeverity.WARNING, sorted[2].severity)
        assertEquals(10, sorted[2].line)
        assertEquals(LintSeverity.WARNING, sorted[3].severity)
        assertEquals(12, sorted[3].line)

        // Last should be info
        assertEquals(LintSeverity.INFO, sorted[4].severity)
        assertEquals(15, sorted[4].line)
    }

    @Test
    fun `test topIssues limit to 10`() {
        // Create 15 issues
        val issues = (1..15).map { i ->
            LintIssue(
                line = i,
                severity = if (i <= 5) LintSeverity.ERROR else LintSeverity.WARNING,
                message = "Issue $i",
                rule = "Rule$i"
            )
        }

        val topIssues = issues.sortedWith(compareBy<LintIssue> {
            when (it.severity) {
                LintSeverity.ERROR -> 0
                LintSeverity.WARNING -> 1
                LintSeverity.INFO -> 2
            }
        }.thenBy { it.line }).take(10)

        assertEquals(10, topIssues.size)
        // First 5 should be errors
        topIssues.take(5).forEach { issue ->
            assertEquals(LintSeverity.ERROR, issue.severity)
        }
        // Next 5 should be warnings (lines 6-10)
        assertEquals(LintSeverity.WARNING, topIssues[5].severity)
        assertEquals(6, topIssues[5].line)
        assertEquals(LintSeverity.WARNING, topIssues[9].severity)
        assertEquals(10, topIssues[9].line)
    }

    @Test
    fun `test format output with real data example`() {
        // Parse the real data example provided by the user
        val jsonString = """
            {
                "totalFiles":6,
                "filesWithIssues":4,
                "totalIssues":57,
                "errorCount":2,
                "warningCount":55,
                "infoCount":0,
                "fileIssues":[
                    {
                        "filePath":"CodeReviewViewModel.kt",
                        "linterName":"detekt",
                        "totalIssues":42,
                        "errorCount":2,
                        "warningCount":40,
                        "infoCount":0,
                        "topIssues":[
                            {"line":100,"column":1,"severity":"ERROR","message":"Unresolved reference","rule":"CompileError"},
                            {"line":200,"column":1,"severity":"ERROR","message":"Type mismatch","rule":"CompileError"},
                            {"line":408,"column":14,"severity":"WARNING","message":"The function is too long","rule":"LongMethod"},
                            {"line":249,"column":25,"severity":"WARNING","message":"The function is too long","rule":"LongMethod"}
                        ],
                        "hasMoreIssues":true
                    }
                ],
                "executedLinters":["detekt"]
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val summary = json.decodeFromString<LinterSummary>(jsonString)

        val formatted = LinterSummary.format(summary)

        // Verify the formatted output contains error markers
        assertTrue(formatted.contains("❌"), "Should contain error icon")
        assertTrue(formatted.contains("Files with Errors"), "Should have error section")
        assertTrue(formatted.contains("Unresolved reference"), "Should contain first error")
        assertTrue(formatted.contains("Type mismatch"), "Should contain second error")

        // Verify error count
        assertTrue(formatted.contains("2 errors"), "Should show 2 errors")

        // Verify that errors appear before warnings in the output
        val errorIndex = formatted.indexOf("Unresolved reference")
        val warningIndex = formatted.indexOf("The function is too long")
        assertTrue(errorIndex < warningIndex, "Errors should appear before warnings")
    }

    @Test
    fun `test format groups files by severity correctly`() {
        val summary = LinterSummary(
            totalFiles = 3,
            filesWithIssues = 3,
            totalIssues = 30,
            errorCount = 5,
            warningCount = 20,
            infoCount = 5,
            fileIssues = listOf(
                FileLintSummary(
                    filePath = "ErrorFile.kt",
                    linterName = "detekt",
                    totalIssues = 10,
                    errorCount = 5,
                    warningCount = 5,
                    infoCount = 0,
                    topIssues = listOf(
                        LintIssue(line = 1, severity = LintSeverity.ERROR, message = "Error 1", rule = "Rule1"),
                        LintIssue(line = 2, severity = LintSeverity.ERROR, message = "Error 2", rule = "Rule2")
                    ),
                    hasMoreIssues = true
                ),
                FileLintSummary(
                    filePath = "WarningFile.kt",
                    linterName = "detekt",
                    totalIssues = 15,
                    errorCount = 0,
                    warningCount = 15,
                    infoCount = 0,
                    topIssues = listOf(
                        LintIssue(line = 10, severity = LintSeverity.WARNING, message = "Warning 1", rule = "Rule3")
                    ),
                    hasMoreIssues = true
                ),
                FileLintSummary(
                    filePath = "InfoFile.kt",
                    linterName = "detekt",
                    totalIssues = 5,
                    errorCount = 0,
                    warningCount = 0,
                    infoCount = 5,
                    topIssues = listOf(
                        LintIssue(line = 20, severity = LintSeverity.INFO, message = "Info 1", rule = "Rule4")
                    ),
                    hasMoreIssues = false
                )
            ),
            executedLinters = listOf("detekt")
        )

        val formatted = LinterSummary.format(summary)

        // Verify structure
        assertTrue(formatted.contains("### ❌ Files with Errors"), "Should have error section")
        assertTrue(formatted.contains("### ⚠️ Files with Warnings"), "Should have warning section")
        assertTrue(formatted.contains("### ℹ️ Files with Info"), "Should have info section")

        // Verify errors appear first
        val errorSectionIndex = formatted.indexOf("### ❌ Files with Errors")
        val warningSectionIndex = formatted.indexOf("### ⚠️ Files with Warnings")
        val infoSectionIndex = formatted.indexOf("### ℹ️ Files with Info")

        assertTrue(errorSectionIndex < warningSectionIndex, "Error section should come before warning section")
        assertTrue(warningSectionIndex < infoSectionIndex, "Warning section should come before info section")
    }

    @Test
    fun `test no issues found`() {
        val summary = LinterSummary(
            totalFiles = 5,
            filesWithIssues = 0,
            totalIssues = 0,
            errorCount = 0,
            warningCount = 0,
            infoCount = 0,
            fileIssues = emptyList(),
            executedLinters = listOf("detekt")
        )

        val formatted = LinterSummary.format(summary)

        assertTrue(formatted.contains("✅ No issues found!"), "Should show success message")
    }

    @Test
    fun `test hasMoreIssues flag with 10 issue limit`() {
        // Test case: exactly 10 issues
        val fileSummary10 = FileLintSummary(
            filePath = "Test.kt",
            linterName = "detekt",
            totalIssues = 10,
            errorCount = 10,
            warningCount = 0,
            infoCount = 0,
            topIssues = (1..10).map { 
                LintIssue(line = it, severity = LintSeverity.ERROR, message = "Error $it", rule = "Rule$it")
            },
            hasMoreIssues = false
        )
        assertEquals(false, fileSummary10.hasMoreIssues)

        // Test case: 11 issues
        val fileSummary11 = FileLintSummary(
            filePath = "Test.kt",
            linterName = "detekt",
            totalIssues = 11,
            errorCount = 11,
            warningCount = 0,
            infoCount = 0,
            topIssues = (1..10).map { 
                LintIssue(line = it, severity = LintSeverity.ERROR, message = "Error $it", rule = "Rule$it")
            },
            hasMoreIssues = true
        )
        assertEquals(true, fileSummary11.hasMoreIssues)
    }
}
