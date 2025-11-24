package cc.unitmesh.agent

import cc.unitmesh.agent.codereview.ModifiedCodeRange
import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CodeReviewAgentPromptRendererTest {
    private val renderer = CodeReviewAgentPromptRenderer()

    @Test
    fun `should group warnings by function context`() {
        // Given: Multiple warnings in the same function
        val lintResults = listOf(
            LintFileResult(
                filePath = "mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt",
                linterName = "detekt",
                errorCount = 0,
                warningCount = 20,
                infoCount = 0,
                issues = listOf(
                    // Line 79 - Constructor warning (in CodeReviewAgent class)
                    LintIssue(
                        line = 79,
                        column = 22,
                        severity = LintSeverity.WARNING,
                        message = "The constructor has too many parameters.",
                        rule = "LongParameterList"
                    ),
                    // Line 88 - UnusedPrivateProperty (in CodeReviewAgent class)
                    LintIssue(
                        line = 88,
                        column = 17,
                        severity = LintSeverity.WARNING,
                        message = "Private property `issueTracker` is unused.",
                        rule = "UnusedPrivateProperty"
                    ),
                    // Line 95 - MaxLineLength (in CodeReviewAgent class)
                    LintIssue(
                        line = 95,
                        column = 1,
                        severity = LintSeverity.WARNING,
                        message = "Line detected, which is longer than the defined maximum line length.",
                        rule = "MaxLineLength"
                    ),
                    // Line 109 - UnusedPrivateProperty (in CodeReviewAgent class)
                    LintIssue(
                        line = 109,
                        column = 17,
                        severity = LintSeverity.WARNING,
                        message = "Private property `promptRenderer` is unused.",
                        rule = "UnusedPrivateProperty"
                    ),
                    // Line 197 - Multiple warnings in generateFixes function
                    LintIssue(
                        line = 197,
                        column = 30,
                        severity = LintSeverity.WARNING,
                        message = "The function generateFixes has too many parameters.",
                        rule = "LongParameterList"
                    ),
                    LintIssue(
                        line = 197,
                        column = 17,
                        severity = LintSeverity.WARNING,
                        message = "The function generateFixes is too long (120).",
                        rule = "LongMethod"
                    ),
                    LintIssue(
                        line = 197,
                        column = 17,
                        severity = LintSeverity.WARNING,
                        message = "The function generateFixes appears to be too complex.",
                        rule = "CyclomaticComplexMethod"
                    ),
                    LintIssue(
                        line = 197,
                        column = 17,
                        severity = LintSeverity.WARNING,
                        message = "Function generateFixes has 4 return statements.",
                        rule = "ReturnCount"
                    ),
                    // Line 234, 240 - MaxLineLength (in generateFixes function)
                    LintIssue(
                        line = 234,
                        column = 1,
                        severity = LintSeverity.WARNING,
                        message = "Line detected, which is longer than the defined maximum line length.",
                        rule = "MaxLineLength"
                    ),
                    LintIssue(
                        line = 240,
                        column = 1,
                        severity = LintSeverity.WARNING,
                        message = "Line detected, which is longer than the defined maximum line length.",
                        rule = "MaxLineLength"
                    )
                )
            )
        )

        val modifiedCodeRanges = mapOf(
            "mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt" to listOf(
                ModifiedCodeRange(
                    filePath = "mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt",
                    elementName = "CodeReviewAgent",
                    elementType = "CLASS",
                    startLine = 79,
                    endLine = 444,
                    modifiedLines = emptyList()
                ),
                ModifiedCodeRange(
                    filePath = "mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt",
                    elementName = "generateFixes",
                    elementType = "FUNCTION",
                    startLine = 197,
                    endLine = 317,
                    modifiedLines = emptyList()
                )
            )
        )

        // When: Generate modification plan prompt
        val prompt = renderer.renderModificationPlanPrompt(
            lintResults = lintResults,
            analysisOutput = "Test analysis output",
            modifiedCodeRanges = modifiedCodeRanges,
            language = "EN"
        )

        // Then: Should group warnings by function context
        // 1. Should contain class-level warnings grouped together
        assertContains(prompt, "In `CodeReviewAgent` (class)")
        
        // 2. Should contain function-level warnings grouped together
        assertContains(prompt, "In `generateFixes` (function)")
        
        // 3. Should show line numbers for grouped warnings
        assertContains(prompt, "79")  // Constructor line
        assertContains(prompt, "88")  // issueTracker line
        assertContains(prompt, "197") // generateFixes line
        
        // 4. Should indicate multiple warnings at different lines
        assertTrue(prompt.contains("warning(s) at lines") || prompt.contains("warnings"))
        
        println("\n=== Generated Prompt (Warnings Only) ===")
        println(prompt)
    }

    @Test
    fun `should group errors by function context`() {
        // Given: Multiple errors in the same function
        val lintResults = listOf(
            LintFileResult(
                filePath = "src/main/kotlin/Example.kt",
                linterName = "detekt",
                errorCount = 3,
                warningCount = 0,
                infoCount = 0,
                issues = listOf(
                    LintIssue(
                        line = 15,
                        column = 10,
                        severity = LintSeverity.ERROR,
                        message = "Null pointer exception possible",
                        rule = "NullSafety"
                    ),
                    LintIssue(
                        line = 20,
                        column = 5,
                        severity = LintSeverity.ERROR,
                        message = "Type mismatch",
                        rule = "TypeCheck"
                    ),
                    LintIssue(
                        line = 25,
                        column = 8,
                        severity = LintSeverity.ERROR,
                        message = "Unreachable code",
                        rule = "UnreachableCode"
                    )
                )
            )
        )

        val modifiedCodeRanges = mapOf(
            "src/main/kotlin/Example.kt" to listOf(
                ModifiedCodeRange(
                    filePath = "src/main/kotlin/Example.kt",
                    elementName = "processData",
                    elementType = "METHOD",
                    startLine = 10,
                    endLine = 50,
                    modifiedLines = emptyList()
                )
            )
        )

        // When: Generate modification plan prompt
        val prompt = renderer.renderModificationPlanPrompt(
            lintResults = lintResults,
            analysisOutput = "Test analysis",
            modifiedCodeRanges = modifiedCodeRanges,
            language = "EN"
        )

        // Then: Should group all errors under the same function
        assertContains(prompt, "In `processData` (method, lines 10-50)")
        assertContains(prompt, "Line 15: Null pointer exception possible")
        assertContains(prompt, "Line 20: Type mismatch")
        assertContains(prompt, "Line 25: Unreachable code")
        
        // Should have CRITICAL section
        assertContains(prompt, "CRITICAL")
        
        println("\n=== Generated Prompt (Errors) ===")
        println(prompt)
    }

    @Test
    fun `should handle mixed errors and warnings in same function`() {
        // Given: Errors and warnings in the same function
        val lintResults = listOf(
            LintFileResult(
                filePath = "src/main/kotlin/Example.kt",
                linterName = "detekt",
                errorCount = 2,
                warningCount = 3,
                infoCount = 0,
                issues = listOf(
                    LintIssue(
                        line = 15,
                        column = 10,
                        severity = LintSeverity.ERROR,
                        message = "Null pointer exception",
                        rule = "NullSafety"
                    ),
                    LintIssue(
                        line = 18,
                        column = 5,
                        severity = LintSeverity.WARNING,
                        message = "Unused variable",
                        rule = "UnusedVariable"
                    ),
                    LintIssue(
                        line = 20,
                        column = 8,
                        severity = LintSeverity.ERROR,
                        message = "Type mismatch",
                        rule = "TypeCheck"
                    ),
                    LintIssue(
                        line = 22,
                        column = 12,
                        severity = LintSeverity.WARNING,
                        message = "Magic number",
                        rule = "MagicNumber"
                    ),
                    LintIssue(
                        line = 25,
                        column = 15,
                        severity = LintSeverity.WARNING,
                        message = "Line too long",
                        rule = "MaxLineLength"
                    )
                )
            )
        )

        val modifiedCodeRanges = mapOf(
            "src/main/kotlin/Example.kt" to listOf(
                ModifiedCodeRange(
                    filePath = "src/main/kotlin/Example.kt",
                    elementName = "calculate",
                    elementType = "FUNCTION",
                    startLine = 10,
                    endLine = 30,
                    modifiedLines = emptyList()
                )
            )
        )

        // When
        val prompt = renderer.renderModificationPlanPrompt(
            lintResults = lintResults,
            analysisOutput = "Analysis",
            modifiedCodeRanges = modifiedCodeRanges,
            language = "EN"
        )

        // Then: Errors should be grouped together
        assertContains(prompt, "**Errors (grouped by function/class):**")
        assertContains(prompt, "In `calculate` (function, lines 10-30)")
        assertContains(prompt, "Line 15: Null pointer exception")
        assertContains(prompt, "Line 20: Type mismatch")
        
        // Warnings should also be grouped
        assertContains(prompt, "**Warnings**")
        assertContains(prompt, "In `calculate` (function)")
        assertContains(prompt, "18, 22, 25") // Should show all warning lines together
        
        println("\n=== Generated Prompt (Mixed) ===")
        println(prompt)
    }

    @Test
    fun `should handle issues without function context`() {
        // Given: Issues outside of any function (e.g., top-level issues)
        val lintResults = listOf(
            LintFileResult(
                filePath = "src/main/kotlin/Example.kt",
                linterName = "detekt",
                errorCount = 1,
                warningCount = 2,
                infoCount = 0,
                issues = listOf(
                    LintIssue(
                        line = 5,
                        column = 1,
                        severity = LintSeverity.ERROR,
                        message = "Import not used",
                        rule = "UnusedImport"
                    ),
                    LintIssue(
                        line = 8,
                        column = 1,
                        severity = LintSeverity.WARNING,
                        message = "Package name incorrect",
                        rule = "PackageNaming"
                    ),
                    LintIssue(
                        line = 10,
                        column = 1,
                        severity = LintSeverity.WARNING,
                        message = "File header missing",
                        rule = "FileHeader"
                    )
                )
            )
        )

        val modifiedCodeRanges = mapOf(
            "src/main/kotlin/Example.kt" to listOf(
                ModifiedCodeRange(
                    filePath = "src/main/kotlin/Example.kt",
                    elementName = "MyClass",
                    elementType = "CLASS",
                    startLine = 20,
                    endLine = 50,
                    modifiedLines = emptyList()
                )
            )
        )

        // When
        val prompt = renderer.renderModificationPlanPrompt(
            lintResults = lintResults,
            analysisOutput = "Analysis",
            modifiedCodeRanges = modifiedCodeRanges,
            language = "EN"
        )

        // Then: Should handle issues without context
        assertContains(prompt, "No specific function context")
        assertContains(prompt, "Line 5: Import not used")
        
        println("\n=== Generated Prompt (No Context) ===")
        println(prompt)
    }

    @Test
    fun `should handle real world detekt warnings`() {
        // Given: Real world example from the user
        val lintResults = listOf(
            LintFileResult(
                filePath = "mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt",
                linterName = "detekt",
                errorCount = 0,
                warningCount = 20,
                infoCount = 0,
                issues = listOf(
                    LintIssue(79, 22, LintSeverity.WARNING, "The constructor has too many parameters.", "LongParameterList"),
                    LintIssue(197, 30, LintSeverity.WARNING, "The function generateFixes has too many parameters.", "LongParameterList"),
                    LintIssue(197, 17, LintSeverity.WARNING, "The function generateFixes is too long (120).", "LongMethod"),
                    LintIssue(197, 17, LintSeverity.WARNING, "The function generateFixes appears to be too complex.", "CyclomaticComplexMethod"),
                    LintIssue(364, 58, LintSeverity.WARNING, "This empty block of code can be removed.", "EmptyFunctionBlock"),
                    LintIssue(341, 18, LintSeverity.WARNING, "The caught exception is too generic.", "TooGenericExceptionCaught"),
                    LintIssue(402, 22, LintSeverity.WARNING, "The caught exception is too generic.", "TooGenericExceptionCaught"),
                    LintIssue(421, 18, LintSeverity.WARNING, "The caught exception is too generic.", "TooGenericExceptionCaught"),
                    LintIssue(421, 18, LintSeverity.WARNING, "The caught exception is swallowed.", "SwallowedException"),
                    LintIssue(197, 17, LintSeverity.WARNING, "Function generateFixes has 4 return statements.", "ReturnCount"),
                    LintIssue(95, 1, LintSeverity.WARNING, "Line detected, which is longer than the defined maximum.", "MaxLineLength"),
                    LintIssue(234, 1, LintSeverity.WARNING, "Line detected, which is longer than the defined maximum.", "MaxLineLength"),
                    LintIssue(240, 1, LintSeverity.WARNING, "Line detected, which is longer than the defined maximum.", "MaxLineLength"),
                    LintIssue(280, 1, LintSeverity.WARNING, "Line detected, which is longer than the defined maximum.", "MaxLineLength"),
                    LintIssue(284, 1, LintSeverity.WARNING, "Line detected, which is longer than the defined maximum.", "MaxLineLength"),
                    LintIssue(291, 45, LintSeverity.WARNING, "This expression contains a magic number.", "MagicNumber"),
                    LintIssue(379, 52, LintSeverity.WARNING, "This expression contains a magic number.", "MagicNumber"),
                    LintIssue(364, 37, LintSeverity.WARNING, "Function parameter `projectPath` is unused.", "UnusedParameter"),
                    LintIssue(88, 17, LintSeverity.WARNING, "Private property `issueTracker` is unused.", "UnusedPrivateProperty"),
                    LintIssue(109, 17, LintSeverity.WARNING, "Private property `promptRenderer` is unused.", "UnusedPrivateProperty")
                )
            )
        )

        val modifiedCodeRanges = mapOf(
            "mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt" to listOf(
                ModifiedCodeRange(
                    filePath = "mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt",
                    elementName = "CodeReviewAgent",
                    elementType = "CLASS",
                    startLine = 79,
                    endLine = 444,
                    modifiedLines = emptyList()
                )
            )
        )

        // When
        val prompt = renderer.renderModificationPlanPrompt(
            lintResults = lintResults,
            analysisOutput = "Test",
            modifiedCodeRanges = modifiedCodeRanges,
            language = "EN"
        )

        // Then: All 20 warnings should be grouped under CodeReviewAgent class
        assertContains(prompt, "20 warning(s)")
        assertContains(prompt, "In `CodeReviewAgent` (class)")
        
        // Should show lines in grouped format
        assertTrue(prompt.contains("warning(s) at lines"))
        
        // Should mention it's lower priority
        assertContains(prompt, "Lower Priority")
        
        println("\n=== Generated Prompt (Real World) ===")
        println(prompt)
    }
}
