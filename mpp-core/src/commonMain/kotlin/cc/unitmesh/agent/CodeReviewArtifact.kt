package cc.unitmesh.agent

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Artifact system for Code Review Agent outputs
 * Inspired by Google Antigravity's artifact concept for structured, verifiable outputs
 */

/**
 * Base interface for all code review artifacts
 */
sealed interface CodeReviewArtifact {
    val id: String
    val timestamp: Instant
    val type: ArtifactType
    val title: String
}

/**
 * Types of artifacts that can be generated during code review
 */
enum class ArtifactType {
    REVIEW_PLAN,           // Pre-review implementation plan
    ANALYSIS_SUMMARY,      // Main review findings
    VISUAL_PROOF,          // Screenshot/visual evidence
    FIX_SUGGESTION,        // Code fix with unified diff
    METRICS_REPORT,        // Code quality metrics
    ISSUE_TRACKING         // Linked issues and their analysis
}

/**
 * Review Plan Artifact - Strategic planning before detailed review
 * Similar to Antigravity's implementation plans
 */
@Serializable
data class ReviewPlanArtifact(
    override val id: String = generateId(),
    override val timestamp: Instant = Clock.System.now(),
    override val type: ArtifactType = ArtifactType.REVIEW_PLAN,
    override val title: String,
    val scope: ReviewScope,
    val estimatedDuration: String,
    val approachSteps: List<ReviewStep>,
    val focusAreas: List<String>,
    val toolsToUse: List<String>,
    val approved: Boolean = false
) : CodeReviewArtifact {
    @Serializable
    data class ReviewScope(
        val filesCount: Int,
        val linesOfCode: Int,
        val reviewType: String,
        val complexity: ComplexityLevel
    )

    @Serializable
    data class ReviewStep(
        val order: Int,
        val description: String,
        val expectedOutput: String,
        val tools: List<String> = emptyList()
    )

    enum class ComplexityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    fun toMarkdown(): String = buildString {
        appendLine("# $title")
        appendLine()
        appendLine("**Generated**: ${timestamp}")
        appendLine("**Status**: ${if (approved) "✅ Approved" else "⏳ Pending Approval"}")
        appendLine()
        appendLine("## Scope")
        appendLine("- Files: ${scope.filesCount}")
        appendLine("- Lines of Code: ${scope.linesOfCode}")
        appendLine("- Review Type: ${scope.reviewType}")
        appendLine("- Complexity: ${scope.complexity}")
        appendLine()
        appendLine("## Estimated Duration")
        appendLine(estimatedDuration)
        appendLine()
        appendLine("## Approach")
        approachSteps.forEach { step ->
            appendLine("${step.order}. **${step.description}**")
            appendLine("   - Expected: ${step.expectedOutput}")
            if (step.tools.isNotEmpty()) {
                appendLine("   - Tools: ${step.tools.joinToString(", ")}")
            }
        }
        appendLine()
        appendLine("## Focus Areas")
        focusAreas.forEach { area ->
            appendLine("- $area")
        }
        appendLine()
        appendLine("## Tools")
        toolsToUse.forEach { tool ->
            appendLine("- `$tool`")
        }
    }
}

/**
 * Analysis Summary Artifact - Main review findings
 * Enhanced with structured severity classification
 */
@Serializable
data class AnalysisSummaryArtifact(
    override val id: String = generateId(),
    override val timestamp: Instant = Clock.System.now(),
    override val type: ArtifactType = ArtifactType.ANALYSIS_SUMMARY,
    override val title: String,
    val overallQuality: QualityScore,
    val findings: List<ReviewFinding>,
    val metrics: CodeMetrics,
    val recommendations: List<String>
) : CodeReviewArtifact {
    @Serializable
    data class QualityScore(
        val overall: Int, // 0-100
        val maintainability: Int,
        val security: Int,
        val performance: Int,
        val testability: Int
    )

    @Serializable
    data class CodeMetrics(
        val filesAnalyzed: Int,
        val linesAnalyzed: Int,
        val issuesFound: Int,
        val criticalIssues: Int,
        val highIssues: Int,
        val mediumIssues: Int,
        val lowIssues: Int
    )

    fun toMarkdown(): String = buildString {
        appendLine("# $title")
        appendLine()
        appendLine("**Generated**: ${timestamp}")
        appendLine()
        appendLine("## 📊 Quality Score")
        appendLine("- **Overall**: ${overallQuality.overall}/100")
        appendLine("- Maintainability: ${overallQuality.maintainability}/100")
        appendLine("- Security: ${overallQuality.security}/100")
        appendLine("- Performance: ${overallQuality.performance}/100")
        appendLine("- Testability: ${overallQuality.testability}/100")
        appendLine()
        appendLine("## 📈 Metrics")
        appendLine("- Files Analyzed: ${metrics.filesAnalyzed}")
        appendLine("- Lines Analyzed: ${metrics.linesAnalyzed}")
        appendLine("- Total Issues: ${metrics.issuesFound}")
        appendLine("  - 🔴 Critical: ${metrics.criticalIssues}")
        appendLine("  - 🟠 High: ${metrics.highIssues}")
        appendLine("  - 🟡 Medium: ${metrics.mediumIssues}")
        appendLine("  - 🟢 Low: ${metrics.lowIssues}")
        appendLine()
        appendLine("## ⚠️ Findings")
        findings.sortedByDescending { it.severity.ordinal }.forEachIndexed { index, finding ->
            appendLine("### ${index + 1}. ${finding.description}")
            appendLine("**Severity**: ${finding.severity}")
            appendLine("**Category**: ${finding.category}")
            finding.filePath?.let { appendLine("**Location**: `$it:${finding.lineNumber ?: "?"}`") }
            finding.suggestion?.let { 
                appendLine()
                appendLine("**Suggestion**: $it")
            }
            appendLine()
        }
        appendLine("## 💡 Recommendations")
        recommendations.forEachIndexed { index, rec ->
            appendLine("${index + 1}. $rec")
        }
    }
}

/**
 * Visual Proof Artifact - Screenshot or visual evidence
 * For UI-related reviews or visual verification
 */
@Serializable
data class VisualProofArtifact(
    override val id: String = generateId(),
    override val timestamp: Instant = Clock.System.now(),
    override val type: ArtifactType = ArtifactType.VISUAL_PROOF,
    override val title: String,
    val description: String,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val videoUrl: String? = null,
    val comparisonMode: ComparisonMode = ComparisonMode.SINGLE,
    val beforeImageUrl: String? = null,
    val afterImageUrl: String? = null
) : CodeReviewArtifact {
    enum class ComparisonMode {
        SINGLE,           // Single image
        BEFORE_AFTER,     // Side-by-side comparison
        VIDEO             // Video recording
    }

    fun toMarkdown(): String = buildString {
        appendLine("# $title")
        appendLine()
        appendLine("**Generated**: ${timestamp}")
        appendLine()
        appendLine(description)
        appendLine()
        when (comparisonMode) {
            ComparisonMode.SINGLE -> {
                imageUrl?.let { appendLine("![Screenshot]($it)") }
            }
            ComparisonMode.BEFORE_AFTER -> {
                appendLine("## Before")
                beforeImageUrl?.let { appendLine("![Before]($it)") }
                appendLine()
                appendLine("## After")
                afterImageUrl?.let { appendLine("![After]($it)") }
            }
            ComparisonMode.VIDEO -> {
                videoUrl?.let { appendLine("[View Recording]($it)") }
            }
        }
    }
}

/**
 * Fix Suggestion Artifact - Actionable code fixes with unified diff
 */
@Serializable
data class FixSuggestionArtifact(
    override val id: String = generateId(),
    override val timestamp: Instant = Clock.System.now(),
    override val type: ArtifactType = ArtifactType.FIX_SUGGESTION,
    override val title: String,
    val issue: ReviewFinding,
    val unifiedDiff: String,
    val explanation: String,
    val applied: Boolean = false,
    val confidence: ConfidenceLevel
) : CodeReviewArtifact {
    enum class ConfidenceLevel {
        HIGH,      // Safe to auto-apply
        MEDIUM,    // Recommended, but review first
        LOW        // Requires careful review
    }

    fun toMarkdown(): String = buildString {
        appendLine("# $title")
        appendLine()
        appendLine("**Generated**: ${timestamp}")
        appendLine("**Confidence**: $confidence")
        appendLine("**Status**: ${if (applied) "✅ Applied" else "⏳ Pending"}")
        appendLine()
        appendLine("## Issue")
        appendLine("**Severity**: ${issue.severity}")
        appendLine("**Location**: `${issue.filePath}:${issue.lineNumber}`")
        appendLine()
        appendLine(issue.description)
        appendLine()
        appendLine("## Explanation")
        appendLine(explanation)
        appendLine()
        appendLine("## Proposed Fix")
        appendLine("```diff")
        appendLine(unifiedDiff)
        appendLine("```")
    }
}

/**
 * Metrics Report Artifact - Quantitative analysis results
 */
@Serializable
data class MetricsReportArtifact(
    override val id: String = generateId(),
    override val timestamp: Instant = Clock.System.now(),
    override val type: ArtifactType = ArtifactType.METRICS_REPORT,
    override val title: String,
    val cyclomatic: Map<String, Int>,
    val coverage: Map<String, Double>,
    val duplication: Map<String, Int>,
    val maintainabilityIndex: Map<String, Int>
) : CodeReviewArtifact {
    fun toMarkdown(): String = buildString {
        appendLine("# $title")
        appendLine()
        appendLine("**Generated**: ${timestamp}")
        appendLine()
        if (cyclomatic.isNotEmpty()) {
            appendLine("## Cyclomatic Complexity")
            cyclomatic.forEach { (file, complexity) ->
                appendLine("- `$file`: $complexity")
            }
            appendLine()
        }
        if (coverage.isNotEmpty()) {
            appendLine("## Test Coverage")
            coverage.forEach { (file, pct) ->
                appendLine("- `$file`: ${"%.1f".format(pct)}%")
            }
            appendLine()
        }
    }
}

/**
 * Issue Tracking Artifact - Links to external issue trackers
 */
@Serializable
data class IssueTrackingArtifact(
    override val id: String = generateId(),
    override val timestamp: Instant = Clock.System.now(),
    override val type: ArtifactType = ArtifactType.ISSUE_TRACKING,
    override val title: String,
    val relatedIssues: List<LinkedIssue>,
    val newIssuesCreated: List<LinkedIssue> = emptyList()
) : CodeReviewArtifact {
    @Serializable
    data class LinkedIssue(
        val issueId: String,
        val issueUrl: String,
        val issueTitle: String,
        val status: String,
        val relevance: String
    )

    fun toMarkdown(): String = buildString {
        appendLine("# $title")
        appendLine()
        appendLine("**Generated**: ${timestamp}")
        appendLine()
        if (relatedIssues.isNotEmpty()) {
            appendLine("## Related Issues")
            relatedIssues.forEach { issue ->
                appendLine("- [${issue.issueId}](${issue.issueUrl}): ${issue.issueTitle}")
                appendLine("  - Status: ${issue.status}")
                appendLine("  - Relevance: ${issue.relevance}")
            }
            appendLine()
        }
        if (newIssuesCreated.isNotEmpty()) {
            appendLine("## New Issues Created")
            newIssuesCreated.forEach { issue ->
                appendLine("- [${issue.issueId}](${issue.issueUrl}): ${issue.issueTitle}")
            }
        }
    }
}

/**
 * Container for all artifacts generated during a review session
 */
@Serializable
data class ReviewArtifactCollection(
    val sessionId: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val plan: ReviewPlanArtifact? = null,
    val summary: AnalysisSummaryArtifact? = null,
    val visualProofs: List<VisualProofArtifact> = emptyList(),
    val fixSuggestions: List<FixSuggestionArtifact> = emptyList(),
    val metricsReport: MetricsReportArtifact? = null,
    val issueTracking: IssueTrackingArtifact? = null
) {
    fun toMarkdown(): String = buildString {
        appendLine("# Code Review Session: $sessionId")
        appendLine()
        appendLine("**Started**: $startTime")
        endTime?.let { appendLine("**Ended**: $it") }
        appendLine()
        appendLine("---")
        appendLine()
        
        plan?.let {
            appendLine(it.toMarkdown())
            appendLine()
            appendLine("---")
            appendLine()
        }
        
        summary?.let {
            appendLine(it.toMarkdown())
            appendLine()
            appendLine("---")
            appendLine()
        }
        
        if (fixSuggestions.isNotEmpty()) {
            appendLine("# Fix Suggestions")
            appendLine()
            fixSuggestions.forEach { fix ->
                appendLine(fix.toMarkdown())
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
        
        metricsReport?.let {
            appendLine(it.toMarkdown())
            appendLine()
            appendLine("---")
            appendLine()
        }
    }
}

private fun generateId(): String {
    val timestamp = Clock.System.now().toEpochMilliseconds()
    return "artifact-$timestamp-${(0..999).random()}"
}
