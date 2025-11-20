package cc.unitmesh.agent

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeReviewArtifactTest {
    
    @Test
    fun testReviewPlanArtifactMarkdown() {
        val artifact = ReviewPlanArtifact(
            scope = ReviewScope(
                fileCount = 5,
                linesOfCode = 1000,
                languages = listOf("Kotlin", "Java"),
                description = "Review 5 files for COMPREHENSIVE"
            ),
            focusAreas = listOf("Code quality", "Security"),
            estimatedDuration = "15-20 minutes",
            reviewObjectives = listOf("Identify issues", "Improve quality")
        )
        
        val markdown = artifact.toMarkdown()
        
        assertTrue(markdown.contains("## 📋 Review Plan"))
        assertTrue(markdown.contains("**Scope**: Review 5 files for COMPREHENSIVE"))
        assertTrue(markdown.contains("**Estimated Duration**: 15-20 minutes"))
        assertTrue(markdown.contains("### Focus Areas"))
        assertTrue(markdown.contains("- Code quality"))
        assertTrue(markdown.contains("### Review Objectives"))
        assertTrue(markdown.contains("- Identify issues"))
    }
    
    @Test
    fun testAnalysisSummaryArtifactMarkdown() {
        val findings = listOf(
            ReviewFinding(
                severity = Severity.CRITICAL,
                category = "Security",
                description = "SQL injection vulnerability",
                filePath = "src/User.kt",
                lineNumber = 42,
                suggestion = "Use parameterized queries"
            ),
            ReviewFinding(
                severity = Severity.HIGH,
                category = "Performance",
                description = "Inefficient algorithm",
                filePath = "src/Utils.kt",
                lineNumber = 100
            )
        )
        
        val artifact = AnalysisSummaryArtifact(
            overallScore = QualityScore(7.5, "Good"),
            findings = findings,
            criticalIssuesCount = 1,
            highIssuesCount = 1,
            mediumIssuesCount = 0,
            lowIssuesCount = 0,
            summary = "Review completed successfully"
        )
        
        val markdown = artifact.toMarkdown()
        
        assertTrue(markdown.contains("## 📊 Analysis Summary"))
        assertTrue(markdown.contains("**Overall Quality Score**: 7.5/10 (Good)"))
        assertTrue(markdown.contains("🔴 Critical: 1"))
        assertTrue(markdown.contains("🟠 High: 1"))
        assertTrue(markdown.contains("### Findings"))
        assertTrue(markdown.contains("#### 1. Security"))
        assertTrue(markdown.contains("SQL injection vulnerability"))
        assertTrue(markdown.contains("`src/User.kt:42`"))
    }
    
    @Test
    fun testVisualProofArtifactMarkdownBeforeAfter() {
        val artifact = VisualProofArtifact(
            title = "Fix null check",
            description = "Added null safety check",
            mode = ComparisonMode.BEFORE_AFTER,
            beforeCode = "val name = user.name",
            afterCode = "val name = user?.name ?: \"Unknown\"",
            diagram = null
        )
        
        val markdown = artifact.toMarkdown()
        
        assertTrue(markdown.contains("## 🔍 Visual Proof: Fix null check"))
        assertTrue(markdown.contains("### Before"))
        assertTrue(markdown.contains("val name = user.name"))
        assertTrue(markdown.contains("### After"))
        assertTrue(markdown.contains("val name = user?.name ?: \"Unknown\""))
    }
    
    @Test
    fun testVisualProofArtifactMarkdownDiagram() {
        val mermaidDiagram = """
            graph TD
                A[Start] --> B[Process]
                B --> C[End]
        """.trimIndent()
        
        val artifact = VisualProofArtifact(
            title = "Flow diagram",
            description = "Process flow visualization",
            mode = ComparisonMode.DIAGRAM,
            beforeCode = null,
            afterCode = null,
            diagram = mermaidDiagram
        )
        
        val markdown = artifact.toMarkdown()
        
        assertTrue(markdown.contains("## 🔍 Visual Proof: Flow diagram"))
        assertTrue(markdown.contains("```mermaid"))
        assertTrue(markdown.contains("graph TD"))
    }
    
    @Test
    fun testFixSuggestionArtifactMarkdown() {
        val artifact = FixSuggestionArtifact(
            findingId = "FINDING-1",
            severity = Severity.CRITICAL,
            filePath = "src/Database.kt",
            lineNumber = 25,
            problem = "SQL injection vulnerability in query",
            fixDescription = "Use parameterized query instead of string concatenation",
            diffPatch = """
                -val query = "SELECT * FROM users WHERE id = " + userId
                +val query = "SELECT * FROM users WHERE id = ?"
                +preparedStatement.setInt(1, userId)
            """.trimIndent(),
            confidence = ConfidenceLevel.HIGH
        )
        
        val markdown = artifact.toMarkdown()
        
        assertTrue(markdown.contains("## 🔧 Fix Suggestion"))
        assertTrue(markdown.contains("**Finding ID**: FINDING-1"))
        assertTrue(markdown.contains("**Severity**: CRITICAL"))
        assertTrue(markdown.contains("**Location**: `src/Database.kt:25`"))
        assertTrue(markdown.contains("**Confidence**: HIGH (80%)"))
        assertTrue(markdown.contains("### Problem"))
        assertTrue(markdown.contains("SQL injection vulnerability"))
        assertTrue(markdown.contains("### Fix Description"))
        assertTrue(markdown.contains("### Diff Patch"))
        assertTrue(markdown.contains("```diff"))
    }
    
    @Test
    fun testMetricsReportArtifactMarkdown() {
        val artifact = MetricsReportArtifact(
            metrics = CodeMetrics(
                linesOfCode = 1500,
                cyclomaticComplexity = 12.5,
                testCoverage = 85.0,
                maintainabilityIndex = 75.0,
                technicalDebtHours = 8.5
            ),
            trends = mapOf(
                "Coverage" to "↑ 5% from last review",
                "Complexity" to "→ Stable"
            ),
            benchmarks = mapOf(
                "Industry Average Coverage" to "80%",
                "Team Average Complexity" to "10.0"
            )
        )
        
        val markdown = artifact.toMarkdown()
        
        assertTrue(markdown.contains("## 📈 Metrics Report"))
        assertTrue(markdown.contains("| Metric | Value |"))
        assertTrue(markdown.contains("| Lines of Code | 1500 |"))
        assertTrue(markdown.contains("| Cyclomatic Complexity | 12.5 |"))
        assertTrue(markdown.contains("| Test Coverage | 85.0% |"))
        assertTrue(markdown.contains("### Trends"))
        assertTrue(markdown.contains("Coverage"))
        assertTrue(markdown.contains("### Benchmarks"))
    }
    
    @Test
    fun testIssueTrackingArtifactMarkdown() {
        val artifact = IssueTrackingArtifact(
            linkedIssues = listOf(
                LinkedIssue(
                    issueId = "ISSUE-123",
                    title = "Fix security vulnerability",
                    url = "https://github.com/repo/issues/123",
                    status = "Open"
                ),
                LinkedIssue(
                    issueId = "ISSUE-124",
                    title = "Improve performance",
                    url = "https://github.com/repo/issues/124",
                    status = "In Progress"
                )
            ),
            newIssuesCreated = listOf("ISSUE-125", "ISSUE-126"),
            summary = "Linked 2 existing issues and created 2 new issues"
        )
        
        val markdown = artifact.toMarkdown()
        
        assertTrue(markdown.contains("## 🎯 Issue Tracking"))
        assertTrue(markdown.contains("Linked 2 existing issues"))
        assertTrue(markdown.contains("### Linked Issues"))
        assertTrue(markdown.contains("[ISSUE-123](https://github.com/repo/issues/123)"))
        assertTrue(markdown.contains("### New Issues Created"))
        assertTrue(markdown.contains("- ISSUE-125"))
    }
    
    @Test
    fun testReviewArtifactCollectionMarkdown() {
        val collection = ReviewArtifactCollection(
            sessionId = "test-session-123",
            artifacts = listOf(
                ReviewPlanArtifact(
                    scope = ReviewScope(3, 500, listOf("Kotlin"), "Test review"),
                    focusAreas = listOf("Quality"),
                    estimatedDuration = "10 minutes",
                    reviewObjectives = listOf("Test")
                ),
                AnalysisSummaryArtifact(
                    overallScore = QualityScore(8.0, "Good"),
                    findings = emptyList(),
                    criticalIssuesCount = 0,
                    highIssuesCount = 0,
                    mediumIssuesCount = 1,
                    lowIssuesCount = 2,
                    summary = "Test summary"
                )
            )
        )
        
        val markdown = collection.toMarkdown()
        
        assertTrue(markdown.contains("# Code Review Report"))
        assertTrue(markdown.contains("**Session ID**: test-session-123"))
        assertTrue(markdown.contains("## 📋 Review Plan"))
        assertTrue(markdown.contains("## 📊 Analysis Summary"))
        assertTrue(markdown.contains("---"))
    }
    
    @Test
    fun testReviewArtifactCollectionGetArtifactsByType() {
        val collection = ReviewArtifactCollection(
            sessionId = "test-session",
            artifacts = listOf(
                ReviewPlanArtifact(
                    scope = ReviewScope(1, 100, listOf("Kotlin"), "Test"),
                    focusAreas = emptyList(),
                    estimatedDuration = "5 min",
                    reviewObjectives = emptyList()
                ),
                AnalysisSummaryArtifact(
                    overallScore = QualityScore(9.0, "Excellent"),
                    findings = emptyList(),
                    criticalIssuesCount = 0,
                    highIssuesCount = 0,
                    mediumIssuesCount = 0,
                    lowIssuesCount = 0,
                    summary = "Test"
                ),
                FixSuggestionArtifact(
                    findingId = "F1",
                    severity = Severity.HIGH,
                    filePath = "test.kt",
                    lineNumber = 1,
                    problem = "Test problem",
                    fixDescription = "Test fix",
                    diffPatch = null,
                    confidence = ConfidenceLevel.MEDIUM
                )
            )
        )
        
        val planArtifacts = collection.getArtifactsByType("plan")
        assertEquals(1, planArtifacts.size)
        assertTrue(planArtifacts[0] is ReviewPlanArtifact)
        
        val analysisArtifacts = collection.getArtifactsByType("analysis")
        assertEquals(1, analysisArtifacts.size)
        assertTrue(analysisArtifacts[0] is AnalysisSummaryArtifact)
        
        val fixArtifacts = collection.getArtifactsByType("fix")
        assertEquals(1, fixArtifacts.size)
        assertTrue(fixArtifacts[0] is FixSuggestionArtifact)
        
        val unknownArtifacts = collection.getArtifactsByType("unknown")
        assertEquals(0, unknownArtifacts.size)
    }
}
