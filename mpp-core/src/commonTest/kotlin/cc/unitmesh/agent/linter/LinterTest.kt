package cc.unitmesh.agent.linter

import cc.unitmesh.agent.language.LanguageDetector
import cc.unitmesh.linter.LintSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LinterTest {
    
    @Test
    fun testLanguageDetector() {
        assertEquals("Kotlin", LanguageDetector.detectLanguage("Test.kt"))
        assertEquals("Java", LanguageDetector.detectLanguage("Test.java"))
        assertEquals("JavaScript", LanguageDetector.detectLanguage("test.js"))
        assertEquals("TypeScript", LanguageDetector.detectLanguage("test.ts"))
        assertEquals("Python", LanguageDetector.detectLanguage("test.py"))
        assertEquals("Rust", LanguageDetector.detectLanguage("test.rs"))
    }
    
    @Test
    fun testGetLinterNamesForLanguage() {
        val kotlinLinters = LanguageDetector.getLinterNamesForLanguage("Kotlin")
        assertTrue(kotlinLinters.contains("detekt"))
        
        val pythonLinters = LanguageDetector.getLinterNamesForLanguage("Python")
        assertTrue(pythonLinters.contains("ruff"))
        assertTrue(pythonLinters.contains("pylint"))
        
        val jsLinters = LanguageDetector.getLinterNamesForLanguage("JavaScript")
        assertTrue(jsLinters.contains("biome"))
    }
    
    @Test
    fun testLinterRegistry() {
        val registry = LinterRegistry.getInstance()
        assertNotNull(registry)
        
        // Test finding linters for files
        val kotlinFiles = listOf("Test.kt", "Main.kt")
        val linters = registry.findLintersForFiles(kotlinFiles)
        
        // Should find linters that support .kt extension
        // Note: Actual linters need to be registered first
        assertTrue(linters.isEmpty() || linters.all { it.supportedExtensions.contains("kt") })
    }
    
    @Test
    fun testLintIssue() {
        val issue = LintIssue(
            line = 10,
            column = 5,
            severity = cc.unitmesh.linter.LintSeverity.ERROR,
            message = "Test error",
            rule = "test-rule",
            filePath = "Test.kt"
        )

        assertEquals(10, issue.line)
        assertEquals(5, issue.column)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, issue.severity)
        assertEquals("Test error", issue.message)
        assertEquals("test-rule", issue.rule)
    }
    
    @Test
    fun testLintResult() {
        val issues = listOf(
            LintIssue(1, 0, cc.unitmesh.linter.LintSeverity.ERROR, "Error 1"),
            LintIssue(2, 0, cc.unitmesh.linter.LintSeverity.WARNING, "Warning 1"),
            LintIssue(3, 0, LintSeverity.ERROR, "Error 2")
        )
        
        val result = LintResult(
            filePath = "Test.kt",
            issues = issues,
            success = true,
            linterName = "test-linter"
        )
        
        assertTrue(result.hasIssues)
        assertEquals(2, result.errorCount)
        assertEquals(1, result.warningCount)
    }
    
    @Test
    fun testFileLintSummary() {
        val issues = listOf(
            LintIssue(10, 5, LintSeverity.ERROR, "Unused variable", "unused-var"),
            LintIssue(15, 8, LintSeverity.WARNING, "Consider const", "const-suggestion"),
            LintIssue(20, 0, LintSeverity.ERROR, "Type mismatch", "type-error")
        )
        
        val summary = FileLintSummary(
            filePath = "src/Main.kt",
            linterName = "detekt",
            totalIssues = 3,
            errorCount = 2,
            warningCount = 1,
            infoCount = 0,
            topIssues = issues,
            hasMoreIssues = false
        )
        
        assertEquals("src/Main.kt", summary.filePath)
        assertEquals(3, summary.totalIssues)
        assertEquals(2, summary.errorCount)
        assertEquals(1, summary.warningCount)
    }
    
    @Test
    fun testLinterSummaryEmpty() = runTest {
        val registry = LinterRegistry.getInstance()
        
        // Test with empty file list
        val summary = registry.getLinterSummaryForFiles(emptyList(), ".")
        
        assertEquals(0, summary.totalFiles)
        assertEquals(0, summary.filesWithIssues)
        assertEquals(0, summary.totalIssues)
        assertEquals(0, summary.errorCount)
        assertEquals(0, summary.warningCount)
        assertEquals(0, summary.infoCount)
        assertTrue(summary.fileIssues.isEmpty())
        assertTrue(summary.executedLinters.isEmpty())
    }
    
    @Test
    fun testLinterSummaryWithMockLinter() = runTest {
        // Create a mock linter with issues
        val mockLinter = MockLinter(
            name = "mock-linter",
            extensions = listOf("kt"),
            mockIssues = listOf(
                LintIssue(10, 0, LintSeverity.ERROR, "Mock error 1", "mock-rule-1"),
                LintIssue(15, 5, LintSeverity.ERROR, "Mock error 2", "mock-rule-2"),
                LintIssue(20, 0, LintSeverity.WARNING, "Mock warning", "mock-rule-3"),
                LintIssue(25, 0, LintSeverity.INFO, "Mock info", "mock-rule-4")
            )
        )
        
        val registry = LinterRegistry()
        registry.register(mockLinter)
        
        val files = listOf("Test.kt", "Main.kt")
        val summary = registry.getLinterSummaryForFiles(files, ".")
        
        assertEquals(2, summary.totalFiles)
        assertEquals(2, summary.filesWithIssues)
        assertEquals(8, summary.totalIssues) // 4 issues per file * 2 files
        assertEquals(4, summary.errorCount) // 2 errors per file * 2 files
        assertEquals(2, summary.warningCount)
        assertEquals(2, summary.infoCount)
        assertEquals(2, summary.fileIssues.size)
        assertTrue(summary.executedLinters.contains("mock-linter"))
    }
    
    @Test
    fun testLinterSummaryFormat() = runTest {
        val fileIssues = listOf(
            FileLintSummary(
                filePath = "src/Main.kt",
                linterName = "detekt",
                totalIssues = 3,
                errorCount = 2,
                warningCount = 1,
                infoCount = 0,
                topIssues = listOf(
                    LintIssue(10, 0, LintSeverity.ERROR, "Unused import", "unused-import"),
                    LintIssue(15, 5, LintSeverity.ERROR, "Type mismatch", "type-error"),
                    LintIssue(20, 0, LintSeverity.WARNING, "Consider const", "const-suggestion")
                ),
                hasMoreIssues = false
            ),
            FileLintSummary(
                filePath = "src/Utils.kt",
                linterName = "detekt",
                totalIssues = 1,
                errorCount = 0,
                warningCount = 1,
                infoCount = 0,
                topIssues = listOf(
                    LintIssue(5, 0, LintSeverity.WARNING, "Magic number", "magic-number")
                ),
                hasMoreIssues = false
            )
        )
        
        val summary = LinterSummary(
            totalFiles = 2,
            filesWithIssues = 2,
            totalIssues = 4,
            errorCount = 2,
            warningCount = 2,
            infoCount = 0,
            fileIssues = fileIssues,
            executedLinters = listOf("detekt")
        )
        
        val formatted = LinterSummary.format(summary)
        
        println("\n=== Formatted Linter Summary ===")
        println(formatted)
        println("=== End of Summary ===\n")
        
        // Validate the format includes key information
        assertTrue(formatted.contains("Lint Results Summary"))
        assertTrue(formatted.contains("Files analyzed: 2"))
        assertTrue(formatted.contains("Files with issues: 2"))
        assertTrue(formatted.contains("Total issues: 4"))
        assertTrue(formatted.contains("2 errors"))
        assertTrue(formatted.contains("2 warnings"))
        assertTrue(formatted.contains("detekt"))
        assertTrue(formatted.contains("src/Main.kt"))
        assertTrue(formatted.contains("Unused import"))
        assertTrue(formatted.contains("Type mismatch"))
        
        // Verify prioritization: errors should appear before warnings
        val errorIndex = formatted.indexOf("Files with Errors")
        val warningIndex = formatted.indexOf("Files with Warnings")
        assertTrue(errorIndex < warningIndex, "Errors should appear before warnings")
    }
    
    @Test
    fun testLinterSummaryNoIssues() = runTest {
        // Mock linter with no issues
        val mockLinter = MockLinter(
            name = "clean-linter",
            extensions = listOf("kt"),
            mockIssues = emptyList()
        )
        
        val registry = LinterRegistry()
        registry.register(mockLinter)
        
        val summary = registry.getLinterSummaryForFiles(listOf("Clean.kt"), ".")
        
        assertEquals(1, summary.totalFiles)
        assertEquals(0, summary.filesWithIssues)
        assertEquals(0, summary.totalIssues)
        
        val formatted = LinterSummary.format(summary)
        println("\n=== Clean Summary ===")
        println(formatted)
        println("=== End ===\n")
        
        assertTrue(formatted.contains("No issues found"))
    }
    
    @Test
    fun testLinterSummaryTruncation() = runTest {
        // Mock linter with many issues to test truncation
        val manyIssues = (1..10).map { i ->
            LintIssue(i * 5, 0, LintSeverity.WARNING, "Warning $i", "rule-$i")
        }
        
        val mockLinter = MockLinter(
            name = "verbose-linter",
            extensions = listOf("kt"),
            mockIssues = manyIssues
        )
        
        val registry = LinterRegistry()
        registry.register(mockLinter)
        
        val summary = registry.getLinterSummaryForFiles(listOf("Verbose.kt"), ".")
        
        assertEquals(1, summary.filesWithIssues)
        assertEquals(10, summary.totalIssues)
        
        // Should only show top 5 issues
        val fileIssue = summary.fileIssues.first()
        assertEquals(5, fileIssue.topIssues.size)
        assertTrue(fileIssue.hasMoreIssues)
        
        val formatted = LinterSummary.format(summary)
        println("\n=== Truncated Summary ===")
        println(formatted)
        println("=== End ===\n")
        
        assertTrue(formatted.contains("and 5 more issues"))
    }
}

/**
 * Mock linter for testing purposes
 */
private class MockLinter(
    override val name: String,
    private val extensions: List<String>,
    private val mockIssues: List<LintIssue>,
    private val available: Boolean = true
) : Linter {
    override val description: String = "Mock linter for testing"
    override val supportedExtensions: List<String> = extensions
    
    override suspend fun isAvailable(): Boolean = available
    
    override suspend fun lintFile(filePath: String, projectPath: String): LintResult {
        return LintResult(
            filePath = filePath,
            issues = mockIssues,
            success = true,
            linterName = name
        )
    }
    
    override fun getInstallationInstructions(): String {
        return "Mock installation instructions"
    }
}


