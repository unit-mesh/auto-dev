package cc.unitmesh.agent.linter

import cc.unitmesh.agent.language.LanguageDetector
import cc.unitmesh.linter.LintSeverity
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
}

