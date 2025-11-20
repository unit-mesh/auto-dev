package cc.unitmesh.agent

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Sealed interface for all code review artifacts
 */
sealed interface CodeReviewArtifact {
    val timestamp: Instant
    fun toMarkdown(): String
}

/**
 * Review plan artifact - Strategic planning phase
 */
@Serializable
data class ReviewPlanArtifact(
    val scope: ReviewScope,
    val focusAreas: List<String>,
    val estimatedDuration: String,
    val reviewObjectives: List<String>,
    override val timestamp: Instant = Clock.System.now()
) : CodeReviewArtifact {
    override fun toMarkdown(): String = buildString {
        appendLine("## 📋 Review Plan")
        appendLine()
        appendLine("**Scope**: ${scope.description}")
        appendLine("**Estimated Duration**: $estimatedDuration")
        appendLine()
        appendLine("### Focus Areas")
        focusAreas.forEach { appendLine("- $it") }
        appendLine()
        appendLine("### Review Objectives")
        reviewObjectives.forEach { appendLine("- $it") }
    }
}

@Serializable
data class ReviewScope(
    val fileCount: Int,
    val linesOfCode: Int,
    val languages: List<String>,
    val description: String
)

/**
 * Analysis summary artifact - Comprehensive findings
 */
@Serializable
data class AnalysisSummaryArtifact(
    val overallScore: QualityScore,
    val findings: List<ReviewFinding>,
    val criticalIssuesCount: Int,
    val highIssuesCount: Int,
    val mediumIssuesCount: Int,
    val lowIssuesCount: Int,
    val summary: String,
    override val timestamp: Instant = Clock.System.now()
) : CodeReviewArtifact {
    override fun toMarkdown(): String = buildString {
        appendLine("## 📊 Analysis Summary")
        appendLine()
        appendLine("**Overall Quality Score**: ${overallScore.score}/10 (${overallScore.rating})")
        appendLine()
        appendLine("### Issue Distribution")
        appendLine("- 🔴 Critical: $criticalIssuesCount")
        appendLine("- 🟠 High: $highIssuesCount")
        appendLine("- 🟡 Medium: $mediumIssuesCount")
        appendLine("- 🟢 Low: $lowIssuesCount")
        appendLine()
        appendLine("### Summary")
        appendLine(summary)
        appendLine()
        appendLine("### Findings")
        findings.forEachIndexed { index, finding ->
            appendLine()
            appendLine("#### ${index + 1}. ${finding.category}")
            appendLine("**Severity**: ${finding.severity}")
            finding.filePath?.let { appendLine("**Location**: `$it${finding.lineNumber?.let { ":$it" } ?: ""}`") }
            appendLine("**Description**: ${finding.description}")
            finding.suggestion?.let { appendLine("**Suggestion**: $it") }
        }
    }
}

@Serializable
data class QualityScore(
    val score: Double, // 0-10
    val rating: String // Excellent, Good, Fair, Poor
)

/**
 * Visual proof artifact - Code comparisons and visualizations
 */
@Serializable
data class VisualProofArtifact(
    val title: String,
    val description: String,
    val mode: ComparisonMode,
    val beforeCode: String?,
    val afterCode: String?,
    val diagram: String?, // Mermaid diagram
    override val timestamp: Instant = Clock.System.now()
) : CodeReviewArtifact {
    override fun toMarkdown(): String = buildString {
        appendLine("## 🔍 Visual Proof: $title")
        appendLine()
        appendLine(description)
        appendLine()
        
        when (mode) {
            ComparisonMode.BEFORE_AFTER -> {
                if (beforeCode != null) {
                    appendLine("### Before")
                    appendLine("```")
                    appendLine(beforeCode)
                    appendLine("```")
                    appendLine()
                }
                if (afterCode != null) {
                    appendLine("### After")
                    appendLine("```")
                    appendLine(afterCode)
                    appendLine("```")
                }
            }
            ComparisonMode.DIAGRAM -> {
                if (diagram != null) {
                    appendLine("```mermaid")
                    appendLine(diagram)
                    appendLine("```")
                }
            }
            ComparisonMode.HYBRID -> {
                if (beforeCode != null) {
                    appendLine("### Code")
                    appendLine("```")
                    appendLine(beforeCode)
                    appendLine("```")
                    appendLine()
                }
                if (diagram != null) {
                    appendLine("### Visualization")
                    appendLine("```mermaid")
                    appendLine(diagram)
                    appendLine("```")
                }
            }
        }
    }
}

@Serializable
enum class ComparisonMode {
    BEFORE_AFTER,
    DIAGRAM,
    HYBRID
}

/**
 * Fix suggestion artifact - Actionable recommendations
 */
@Serializable
data class FixSuggestionArtifact(
    val findingId: String,
    val severity: Severity,
    val filePath: String,
    val lineNumber: Int?,
    val problem: String,
    val fixDescription: String,
    val diffPatch: String?,
    val confidence: ConfidenceLevel,
    override val timestamp: Instant = Clock.System.now()
) : CodeReviewArtifact {
    override fun toMarkdown(): String = buildString {
        appendLine("## 🔧 Fix Suggestion")
        appendLine()
        appendLine("**Finding ID**: $findingId")
        appendLine("**Severity**: $severity")
        appendLine("**Location**: `$filePath${lineNumber?.let { ":$it" } ?: ""}`")
        appendLine("**Confidence**: ${confidence.name} (${confidence.percentage}%)")
        appendLine()
        appendLine("### Problem")
        appendLine(problem)
        appendLine()
        appendLine("### Fix Description")
        appendLine(fixDescription)
        appendLine()
        
        if (diffPatch != null) {
            appendLine("### Diff Patch")
            appendLine("```diff")
            appendLine(diffPatch)
            appendLine("```")
        }
    }
}

@Serializable
enum class ConfidenceLevel(val percentage: Int) {
    VERY_HIGH(95),
    HIGH(80),
    MEDIUM(60),
    LOW(40),
    VERY_LOW(20)
}

/**
 * Metrics report artifact - Quantitative analysis
 */
@Serializable
data class MetricsReportArtifact(
    val metrics: CodeMetrics,
    val trends: Map<String, String>,
    val benchmarks: Map<String, String>,
    override val timestamp: Instant = Clock.System.now()
) : CodeReviewArtifact {
    override fun toMarkdown(): String = buildString {
        appendLine("## 📈 Metrics Report")
        appendLine()
        appendLine("### Code Metrics")
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Lines of Code | ${metrics.linesOfCode} |")
        appendLine("| Cyclomatic Complexity | ${metrics.cyclomaticComplexity} |")
        appendLine("| Test Coverage | ${metrics.testCoverage}% |")
        appendLine("| Maintainability Index | ${metrics.maintainabilityIndex} |")
        appendLine("| Technical Debt (hours) | ${metrics.technicalDebtHours} |")
        appendLine()
        
        if (trends.isNotEmpty()) {
            appendLine("### Trends")
            trends.forEach { (key, value) ->
                appendLine("- **$key**: $value")
            }
            appendLine()
        }
        
        if (benchmarks.isNotEmpty()) {
            appendLine("### Benchmarks")
            benchmarks.forEach { (key, value) ->
                appendLine("- **$key**: $value")
            }
        }
    }
}

@Serializable
data class CodeMetrics(
    val linesOfCode: Int,
    val cyclomaticComplexity: Double,
    val testCoverage: Double,
    val maintainabilityIndex: Double,
    val technicalDebtHours: Double
)

/**
 * Issue tracking artifact - Integration with issue trackers
 */
@Serializable
data class IssueTrackingArtifact(
    val linkedIssues: List<LinkedIssue>,
    val newIssuesCreated: List<String>,
    val summary: String,
    override val timestamp: Instant = Clock.System.now()
) : CodeReviewArtifact {
    override fun toMarkdown(): String = buildString {
        appendLine("## 🎯 Issue Tracking")
        appendLine()
        appendLine(summary)
        appendLine()
        
        if (linkedIssues.isNotEmpty()) {
            appendLine("### Linked Issues")
            linkedIssues.forEach { issue ->
                appendLine("- **[${issue.issueId}](${issue.url})**: ${issue.title} (${issue.status})")
            }
            appendLine()
        }
        
        if (newIssuesCreated.isNotEmpty()) {
            appendLine("### New Issues Created")
            newIssuesCreated.forEach { appendLine("- $it") }
        }
    }
}

@Serializable
data class LinkedIssue(
    val issueId: String,
    val title: String,
    val url: String,
    val status: String
)

/**
 * Collection of review artifacts with session aggregation
 */
@Serializable
data class ReviewArtifactCollection(
    val sessionId: String,
    val artifacts: List<CodeReviewArtifact>,
    val createdAt: Instant = Clock.System.now()
) {
    fun toMarkdown(): String = buildString {
        appendLine("# Code Review Report")
        appendLine()
        appendLine("**Session ID**: $sessionId")
        appendLine("**Generated**: $createdAt")
        appendLine()
        appendLine("---")
        appendLine()
        
        artifacts.forEach { artifact ->
            appendLine(artifact.toMarkdown())
            appendLine()
            appendLine("---")
            appendLine()
        }
    }
    
    fun getArtifactsByType(type: String): List<CodeReviewArtifact> {
        return when (type) {
            "plan" -> artifacts.filterIsInstance<ReviewPlanArtifact>()
            "analysis" -> artifacts.filterIsInstance<AnalysisSummaryArtifact>()
            "visual" -> artifacts.filterIsInstance<VisualProofArtifact>()
            "fix" -> artifacts.filterIsInstance<FixSuggestionArtifact>()
            "metrics" -> artifacts.filterIsInstance<MetricsReportArtifact>()
            "issues" -> artifacts.filterIsInstance<IssueTrackingArtifact>()
            else -> emptyList()
        }
    }
}
