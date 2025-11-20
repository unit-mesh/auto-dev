package cc.unitmesh.agent

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeReviewArtifactTest {

    @Test
    fun `ReviewPlanArtifact should generate valid markdown`() {
        // Given
        val plan = ReviewPlanArtifact(
            title = "Security Review Plan",
            scope = ReviewPlanArtifact.ReviewScope(
                filesCount = 10,
                linesOfCode = 500,
                reviewType = "SECURITY",
                complexity = ReviewPlanArtifact.ComplexityLevel.MEDIUM
            ),
            estimatedDuration = "10-15 minutes",
            approachSteps = listOf(
                ReviewPlanArtifact.ReviewStep(
                    order = 1,
                    description = "Analyze authentication logic",
                    expectedOutput = "List of auth-related issues",
                    tools = listOf("read-file", "grep-search")
                ),
                ReviewPlanArtifact.ReviewStep(
                    order = 2,
                    description = "Check input validation",
                    expectedOutput = "Validation gaps identified",
                    tools = listOf("read-file")
                )
            ),
            focusAreas = listOf("SQL Injection", "XSS", "CSRF"),
            toolsToUse = listOf("read-file", "grep-search")
        )

        // When
        val markdown = plan.toMarkdown()

        // Then
        assertTrue(markdown.contains("# Security Review Plan"))
        assertTrue(markdown.contains("Files: 10"))
        assertTrue(markdown.contains("Lines of Code: 500"))
        assertTrue(markdown.contains("Complexity: MEDIUM"))
        assertTrue(markdown.contains("1. **Analyze authentication logic**"))
        assertTrue(markdown.contains("2. **Check input validation**"))
        assertTrue(markdown.contains("- SQL Injection"))
    }

    @Test
    fun `AnalysisSummaryArtifact should calculate metrics correctly`() {
        // Given
        val findings = listOf(
            ReviewFinding(
                severity = Severity.CRITICAL,
                category = "Security",
                description = "SQL injection vulnerability",
                filePath = "src/Database.kt",
                lineNumber = 42
            ),
            ReviewFinding(
                severity = Severity.HIGH,
                category = "Performance",
                description = "N+1 query problem",
                filePath = "src/Repository.kt",
                lineNumber = 15
            ),
            ReviewFinding(
                severity = Severity.MEDIUM,
                category = "Maintainability",
                description = "Complex method needs refactoring",
                filePath = "src/Service.kt",
                lineNumber = 100
            )
        )

        val summary = AnalysisSummaryArtifact(
            title = "Comprehensive Review Summary",
            overallQuality = AnalysisSummaryArtifact.QualityScore(
                overall = 75,
                maintainability = 70,
                security = 60,
                performance = 80,
                testability = 85
            ),
            findings = findings,
            metrics = AnalysisSummaryArtifact.CodeMetrics(
                filesAnalyzed = 5,
                linesAnalyzed = 1000,
                issuesFound = 3,
                criticalIssues = 1,
                highIssues = 1,
                mediumIssues = 1,
                lowIssues = 0
            ),
            recommendations = listOf("Add input validation", "Use prepared statements")
        )

        // When
        val markdown = summary.toMarkdown()

        // Then
        assertTrue(markdown.contains("Overall**: 75/100"))
        assertTrue(markdown.contains("Security: 60/100"))
        assertTrue(markdown.contains("🔴 Critical: 1"))
        assertTrue(markdown.contains("🟠 High: 1"))
        assertTrue(markdown.contains("SQL injection vulnerability"))
        assertTrue(markdown.contains("Use prepared statements"))
    }

    @Test
    fun `FixSuggestionArtifact should format unified diff correctly`() {
        // Given
        val finding = ReviewFinding(
            severity = Severity.HIGH,
            category = "Security",
            description = "Missing input validation",
            filePath = "src/Controller.kt",
            lineNumber = 25
        )

        val diff = """
            |--- a/src/Controller.kt
            |+++ b/src/Controller.kt
            |@@ -23,7 +23,10 @@ class UserController {
            |     fun createUser(name: String) {
            |-        database.insert(name)
            |+        if (name.isBlank()) {
            |+            throw IllegalArgumentException("Name cannot be blank")
            |+        }
            |+        database.insert(name)
            |     }
            | }
        """.trimMargin()

        val fix = FixSuggestionArtifact(
            title = "Add input validation for user creation",
            issue = finding,
            unifiedDiff = diff,
            explanation = "Input validation prevents invalid data from entering the database",
            confidence = FixSuggestionArtifact.ConfidenceLevel.HIGH
        )

        // When
        val markdown = fix.toMarkdown()

        // Then
        assertTrue(markdown.contains("# Add input validation"))
        assertTrue(markdown.contains("**Confidence**: HIGH"))
        assertTrue(markdown.contains("**Severity**: HIGH"))
        assertTrue(markdown.contains("```diff"))
        assertTrue(markdown.contains("--- a/src/Controller.kt"))
        assertTrue(markdown.contains("Input validation prevents"))
    }

    @Test
    fun `ReviewArtifactCollection should aggregate all artifacts`() {
        // Given
        val sessionId = "test-session-123"
        val startTime = Clock.System.now()
        
        val plan = ReviewPlanArtifact(
            title = "Review Plan",
            scope = ReviewPlanArtifact.ReviewScope(
                filesCount = 5,
                linesOfCode = 200,
                reviewType = "COMPREHENSIVE",
                complexity = ReviewPlanArtifact.ComplexityLevel.LOW
            ),
            estimatedDuration = "5 minutes",
            approachSteps = listOf(),
            focusAreas = listOf(),
            toolsToUse = listOf()
        )

        val summary = AnalysisSummaryArtifact(
            title = "Review Summary",
            overallQuality = AnalysisSummaryArtifact.QualityScore(85, 80, 90, 85, 80),
            findings = listOf(),
            metrics = AnalysisSummaryArtifact.CodeMetrics(5, 200, 0, 0, 0, 0, 0),
            recommendations = listOf()
        )

        val collection = ReviewArtifactCollection(
            sessionId = sessionId,
            startTime = startTime,
            plan = plan,
            summary = summary
        )

        // When
        val markdown = collection.toMarkdown()

        // Then
        assertTrue(markdown.contains("# Code Review Session: $sessionId"))
        assertTrue(markdown.contains("**Started**:"))
        assertTrue(markdown.contains("# Review Plan"))
        assertTrue(markdown.contains("# Review Summary"))
    }

    @Test
    fun `VisualProofArtifact should support different comparison modes`() {
        // Given - Single mode
        val single = VisualProofArtifact(
            title = "UI Screenshot",
            description = "Login page layout",
            imageUrl = "https://example.com/screenshot.png",
            comparisonMode = VisualProofArtifact.ComparisonMode.SINGLE
        )

        // When
        val singleMarkdown = single.toMarkdown()

        // Then
        assertTrue(singleMarkdown.contains("![Screenshot](https://example.com/screenshot.png)"))

        // Given - Before/After mode
        val comparison = VisualProofArtifact(
            title = "Button Alignment Fix",
            description = "Fixed button alignment issue",
            comparisonMode = VisualProofArtifact.ComparisonMode.BEFORE_AFTER,
            beforeImageUrl = "https://example.com/before.png",
            afterImageUrl = "https://example.com/after.png"
        )

        // When
        val comparisonMarkdown = comparison.toMarkdown()

        // Then
        assertTrue(comparisonMarkdown.contains("## Before"))
        assertTrue(comparisonMarkdown.contains("## After"))
        assertTrue(comparisonMarkdown.contains("![Before](https://example.com/before.png)"))
        assertTrue(comparisonMarkdown.contains("![After](https://example.com/after.png)"))
    }

    @Test
    fun `IssueTrackingArtifact should link external issues`() {
        // Given
        val artifact = IssueTrackingArtifact(
            title = "Related GitHub Issues",
            relatedIssues = listOf(
                IssueTrackingArtifact.LinkedIssue(
                    issueId = "GH-123",
                    issueUrl = "https://github.com/org/repo/issues/123",
                    issueTitle = "Security vulnerability in auth",
                    status = "OPEN",
                    relevance = "Directly related to authentication findings"
                ),
                IssueTrackingArtifact.LinkedIssue(
                    issueId = "GH-124",
                    issueUrl = "https://github.com/org/repo/issues/124",
                    issueTitle = "Performance improvement needed",
                    status = "CLOSED",
                    relevance = "Similar performance issue already addressed"
                )
            ),
            newIssuesCreated = listOf(
                IssueTrackingArtifact.LinkedIssue(
                    issueId = "GH-125",
                    issueUrl = "https://github.com/org/repo/issues/125",
                    issueTitle = "Fix SQL injection in user query",
                    status = "OPEN",
                    relevance = "Critical finding from this review"
                )
            )
        )

        // When
        val markdown = artifact.toMarkdown()

        // Then
        assertTrue(markdown.contains("## Related Issues"))
        assertTrue(markdown.contains("[GH-123](https://github.com/org/repo/issues/123)"))
        assertTrue(markdown.contains("Security vulnerability in auth"))
        assertTrue(markdown.contains("## New Issues Created"))
        assertTrue(markdown.contains("[GH-125]"))
        assertTrue(markdown.contains("Fix SQL injection"))
    }

    @Test
    fun `MetricsReportArtifact should format code metrics`() {
        // Given
        val metrics = MetricsReportArtifact(
            title = "Code Quality Metrics",
            cyclomatic = mapOf(
                "UserService.kt" to 15,
                "AuthService.kt" to 8,
                "PaymentService.kt" to 22
            ),
            coverage = mapOf(
                "UserService.kt" to 85.5,
                "AuthService.kt" to 92.0,
                "PaymentService.kt" to 45.3
            ),
            duplication = mapOf(
                "UserService.kt" to 5,
                "AuthService.kt" to 0
            ),
            maintainabilityIndex = mapOf(
                "UserService.kt" to 75,
                "AuthService.kt" to 90
            )
        )

        // When
        val markdown = metrics.toMarkdown()

        // Then
        assertTrue(markdown.contains("## Cyclomatic Complexity"))
        assertTrue(markdown.contains("`UserService.kt`: 15"))
        assertTrue(markdown.contains("## Test Coverage"))
        assertTrue(markdown.contains("`UserService.kt`: 85.5%"))
    }
}
