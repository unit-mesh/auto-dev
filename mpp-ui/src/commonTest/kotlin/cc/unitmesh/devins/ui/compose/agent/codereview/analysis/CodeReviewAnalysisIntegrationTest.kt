package cc.unitmesh.devins.ui.compose.agent.codereview.analysis

import cc.unitmesh.linter.LintIssue
import cc.unitmesh.linter.LintResult
import cc.unitmesh.linter.LintSeverity
import cc.unitmesh.linter.Linter
import cc.unitmesh.linter.LinterRegistry
import cc.unitmesh.devins.ui.compose.agent.codereview.ModifiedCodeRange
import cc.unitmesh.devins.ui.compose.sketch.DiffHunk
import cc.unitmesh.devins.ui.compose.sketch.DiffLine
import cc.unitmesh.devins.ui.compose.sketch.DiffLineType
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for Code Review Lint filtering logic.
 * Tests the full flow: Linter -> LintExecutor -> LintResultFormatter
 */
class CodeReviewAnalysisIntegrationTest {

    private lateinit var lintExecutor: LintExecutor
    private lateinit var lintResultFormatter: LintResultFormatter
    private lateinit var testLinter: TestLinter

    @BeforeTest
    fun setup() {
        lintExecutor = LintExecutor()
        lintResultFormatter = LintResultFormatter()
        testLinter = TestLinter()

        // Register test linter
        val registry = LinterRegistry.getInstance()
        registry.register(testLinter)
    }

    @Test
    fun testLintExecutor_withModifiedCodeRanges_filtersIssuesCorrectly() = runTest {
        // Given: A file with lint issues
        val filePath = "/project/src/main/Example.kt"
        val projectPath = "/project"

        // Modified code ranges: lines 10-20 and 50-60
        val modifiedCodeRanges = mapOf(
            filePath to listOf(
                ModifiedCodeRange(
                    filePath = filePath,
                    elementName = "functionA",
                    elementType = "FUNCTION",
                    startLine = 10,
                    endLine = 20,
                    modifiedLines = listOf(12, 15, 18)
                ),
                ModifiedCodeRange(
                    filePath = filePath,
                    elementName = "functionB",
                    elementType = "FUNCTION",
                    startLine = 50,
                    endLine = 60,
                    modifiedLines = listOf(52, 55)
                )
            )
        )

        // TestLinter will return issues at various lines
        testLinter.setTestIssues(
            filePath,
            listOf(
                LintIssue(line = 5, severity = LintSeverity.ERROR, message = "Error at line 5 (outside range)"),
                LintIssue(line = 12, severity = LintSeverity.WARNING, message = "Warning at line 12 (in range)"),
                LintIssue(line = 18, severity = LintSeverity.ERROR, message = "Error at line 18 (in range)"),
                LintIssue(line = 25, severity = LintSeverity.INFO, message = "Info at line 25 (outside range)"),
                LintIssue(line = 55, severity = LintSeverity.WARNING, message = "Warning at line 55 (in range)"),
                LintIssue(line = 70, severity = LintSeverity.ERROR, message = "Error at line 70 (outside range)")
            )
        )

        // When: Running lint with modified code ranges
        val results = lintExecutor.runLint(
            filePaths = listOf(filePath),
            projectPath = projectPath,
            modifiedCodeRanges = modifiedCodeRanges
        )

        // Then: Only issues in modified ranges should be included
        assertEquals(1, results.size, "Should have results for 1 file")

        val fileResult = results.first()
        assertEquals(filePath, fileResult.filePath)
        assertEquals(3, fileResult.issues.size, "Should filter to 3 issues (lines 12, 18, 55)")
        assertEquals(1, fileResult.errorCount, "Should have 1 error (line 18)")
        assertEquals(2, fileResult.warningCount, "Should have 2 warnings (lines 12, 55)")
        assertEquals(0, fileResult.infoCount, "Should have 0 info")

        // Verify the specific issues
        val issueLines = fileResult.issues.map { it.line }.sorted()
        assertEquals(listOf(12, 18, 55), issueLines)
    }

    @Test
    fun testLintExecutor_withoutModifiedCodeRanges_includesAllIssues() = runTest {
        // Given: A file with lint issues
        val filePath = "/project/src/main/Example.kt"
        val projectPath = "/project"

        testLinter.setTestIssues(
            filePath,
            listOf(
                LintIssue(line = 5, severity = LintSeverity.ERROR, message = "Error at line 5"),
                LintIssue(line = 10, severity = LintSeverity.WARNING, message = "Warning at line 10"),
                LintIssue(line = 15, severity = LintSeverity.INFO, message = "Info at line 15")
            )
        )

        // When: Running lint without modified code ranges
        val results = lintExecutor.runLint(
            filePaths = listOf(filePath),
            projectPath = projectPath,
            modifiedCodeRanges = emptyMap()
        )

        // Then: All issues should be included
        assertEquals(1, results.size)
        val fileResult = results.first()
        assertEquals(3, fileResult.issues.size, "Should include all 3 issues")
        assertEquals(1, fileResult.errorCount)
        assertEquals(1, fileResult.warningCount)
        assertEquals(1, fileResult.infoCount)
    }

    @Test
    fun testLintExecutor_withEmptyModifiedRangesForFile_returnsNoIssues() = runTest {
        // Given: A file with lint issues but no modified ranges for it
        val filePath = "/project/src/main/Example.kt"
        val otherFilePath = "/project/src/main/Other.kt"
        val projectPath = "/project"

        val modifiedCodeRanges = mapOf(
            otherFilePath to listOf(
                ModifiedCodeRange(
                    filePath = otherFilePath,
                    elementName = "otherFunction",
                    elementType = "FUNCTION",
                    startLine = 10,
                    endLine = 20,
                    modifiedLines = listOf(15)
                )
            )
        )

        testLinter.setTestIssues(
            filePath,
            listOf(
                LintIssue(line = 5, severity = LintSeverity.ERROR, message = "Error at line 5"),
                LintIssue(line = 10, severity = LintSeverity.WARNING, message = "Warning at line 10")
            )
        )

        // When: Running lint with modified ranges that don't include this file
        val results = lintExecutor.runLint(
            filePaths = listOf(filePath),
            projectPath = projectPath,
            modifiedCodeRanges = modifiedCodeRanges
        )

        // Then: No issues should be returned
        assertEquals(0, results.size, "Should return no results for file with no modified ranges")
    }

    @Test
    fun testLintResultFormatter_formatsCorrectly() {
        // Given: Lint results with various issues
        val lintResults = listOf(
            cc.unitmesh.devins.ui.compose.agent.codereview.FileLintResult(
                filePath = "/project/src/Example.kt",
                linterName = "TestLinter",
                errorCount = 2,
                warningCount = 1,
                infoCount = 1,
                issues = listOf(
                    cc.unitmesh.devins.ui.compose.agent.codereview.LintIssueUI(
                        line = 10,
                        column = 5,
                        severity = cc.unitmesh.devins.ui.compose.agent.codereview.LintSeverityUI.ERROR,
                        message = "Test error message",
                        rule = "test-rule-1"
                    ),
                    cc.unitmesh.devins.ui.compose.agent.codereview.LintIssueUI(
                        line = 15,
                        column = 8,
                        severity = cc.unitmesh.devins.ui.compose.agent.codereview.LintSeverityUI.WARNING,
                        message = "Test warning message",
                        rule = "test-rule-2"
                    )
                )
            )
        )

        // When: Formatting the results
        val formatted = lintResultFormatter.formatLintResults(lintResults)

        // Then: Should return formatted map with file path as key
        assertEquals(1, formatted.size)
        assertTrue(formatted.containsKey("/project/src/Example.kt"))

        val formattedText = formatted["/project/src/Example.kt"]!!
        assertTrue(formattedText.contains("File: /project/src/Example.kt"))
        assertTrue(formattedText.contains("Total Issues: 4"))
        assertTrue(formattedText.contains("Errors: 2"))
        assertTrue(formattedText.contains("Warnings: 1"))
        assertTrue(formattedText.contains("Info: 1"))
        assertTrue(formattedText.contains("[ERROR] Line 10: Test error message"))
        assertTrue(formattedText.contains("Rule: test-rule-1"))
    }

    @Test
    fun testLintResultFormatter_createsSummary() {
        // Given: Multiple file results
        val lintResults = listOf(
            cc.unitmesh.devins.ui.compose.agent.codereview.FileLintResult(
                filePath = "/project/src/File1.kt",
                linterName = "TestLinter",
                errorCount = 2,
                warningCount = 1,
                infoCount = 0,
                issues = emptyList()
            ),
            cc.unitmesh.devins.ui.compose.agent.codereview.FileLintResult(
                filePath = "/project/src/File2.kt",
                linterName = "TestLinter",
                errorCount = 1,
                warningCount = 3,
                infoCount = 2,
                issues = emptyList()
            )
        )

        // When: Creating summary
        val summary = lintResultFormatter.formatSummary(lintResults)

        // Then: Should summarize all results
        assertTrue(summary.contains("Files analyzed: 2"))
        assertTrue(summary.contains("Total errors: 3"))
        assertTrue(summary.contains("Total warnings: 4"))
        assertTrue(summary.contains("Total info: 2"))
    }

    @Test
    fun testDiffModels_areConstructedCorrectly() {
        // Test that DiffHunk and DiffLine are constructed with correct parameters
        val diffLine1 = DiffLine(
            type = DiffLineType.ADDED,
            content = "+    val newVariable = 42",
            oldLineNumber = null,
            newLineNumber = 15
        )

        val diffLine2 = DiffLine(
            type = DiffLineType.DELETED,
            content = "-    val oldVariable = 10",
            oldLineNumber = 14,
            newLineNumber = null
        )

        val diffLine3 = DiffLine(
            type = DiffLineType.CONTEXT,
            content = "     val existing = 20",
            oldLineNumber = 15,
            newLineNumber = 16
        )

        val diffHunk = DiffHunk(
            oldStartLine = 14,
            oldLineCount = 5,
            newStartLine = 15,
            newLineCount = 6,
            lines = listOf(diffLine1, diffLine2, diffLine3),
            header = "@@ -14,5 +15,6 @@"
        )

        // Verify construction
        assertEquals(DiffLineType.ADDED, diffLine1.type)
        assertEquals(15, diffLine1.newLineNumber)
        assertEquals(null, diffLine1.oldLineNumber)

        assertEquals(DiffLineType.DELETED, diffLine2.type)
        assertEquals(14, diffLine2.oldLineNumber)
        assertEquals(null, diffLine2.newLineNumber)

        assertEquals(3, diffHunk.lines.size)
        assertEquals(14, diffHunk.oldStartLine)
        assertEquals(15, diffHunk.newStartLine)
        assertEquals("@@ -14,5 +15,6 @@", diffHunk.header)
    }

    @Test
    fun testModifiedCodeRange_isDataClass() {
        // Test that ModifiedCodeRange is a data class and works correctly
        val range1 = ModifiedCodeRange(
            filePath = "/project/src/Example.kt",
            elementName = "myFunction",
            elementType = "FUNCTION",
            startLine = 10,
            endLine = 20,
            modifiedLines = listOf(12, 15)
        )

        val range2 = ModifiedCodeRange(
            filePath = "/project/src/Example.kt",
            elementName = "myFunction",
            elementType = "FUNCTION",
            startLine = 10,
            endLine = 20,
            modifiedLines = listOf(12, 15)
        )

        val range3 = ModifiedCodeRange(
            filePath = "/project/src/Example.kt",
            elementName = "otherFunction",
            elementType = "FUNCTION",
            startLine = 30,
            endLine = 40,
            modifiedLines = listOf(35)
        )

        // Data class equality
        assertEquals(range1, range2, "Identical ModifiedCodeRange should be equal")
        assertTrue(range1 != range3, "Different ModifiedCodeRange should not be equal")

        // Data class copy
        val range4 = range1.copy(elementName = "renamedFunction")
        assertEquals("renamedFunction", range4.elementName)
        assertEquals(range1.startLine, range4.startLine)
    }
}

/**
 * Test implementation of Linter for testing purposes
 */
class TestLinter : Linter {
    override val name: String = "TestLinter"
    override val description: String = "A test linter for integration tests"
    override val supportedExtensions: List<String> = listOf("kt", "kts", "java")

    private var testIssues: Map<String, List<LintIssue>> = emptyMap()
    private var available: Boolean = true

    /**
     * Set test issues for a specific file
     */
    fun setTestIssues(filePath: String, issues: List<LintIssue>) {
        testIssues = mapOf(filePath to issues)
    }

    /**
     * Set linter availability
     */
    fun setAvailable(available: Boolean) {
        this.available = available
    }

    override suspend fun isAvailable(): Boolean = available

    override suspend fun lintFile(filePath: String, projectPath: String): LintResult {
        val issues = testIssues[filePath] ?: emptyList()
        return LintResult(
            filePath = filePath,
            issues = issues,
            success = true,
            errorMessage = null,
            linterName = name
        )
    }

    override fun getInstallationInstructions(): String {
        return "TestLinter is a test implementation. No installation required."
    }
}
