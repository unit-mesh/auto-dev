package cc.unitmesh.devins.ui.compose.agent.codereview

import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.DiffHunk
import cc.unitmesh.agent.tracker.IssueInfo
import kotlinx.serialization.Serializable

/**
 * Complete state for Code Review side-by-side UI
 */
data class CodeReviewState(
    val isLoading: Boolean = false,
    val isLoadingDiff: Boolean = false, // Loading diff for a specific commit (not blocking UI)
    val error: String? = null,
    val commitHistory: List<CommitInfo> = emptyList(),
    val selectedCommitIndices: Set<Int> = emptySet(),
    val diffFiles: List<DiffFileInfo> = emptyList(),
    val selectedFileIndex: Int = 0,
    val aiProgress: AIAnalysisProgress = AIAnalysisProgress(),
    val fixResults: List<FixResult> = emptyList(),
    // Infinite scroll support
    val hasMoreCommits: Boolean = false,
    val isLoadingMore: Boolean = false,
    val totalCommitCount: Int? = null,
    val originDiff: String? = null,
    // Test coverage
    val relatedTests: Map<String, List<TestFileInfo>> = emptyMap(),
    val isLoadingTests: Boolean = false,
    // File viewer
    val fileViewerPath: String? = null,
    val fileViewerStartLine: Int? = null,
    val fileViewerEndLine: Int? = null
)

/**
 * Information about a file in the diff
 * Extends FileDiff from sketch package with CodeReview-specific fields
 */
data class DiffFileInfo(
    val path: String,
    val oldPath: String? = null, // For renamed files
    val changeType: ChangeType = ChangeType.EDIT,
    val hunks: List<DiffHunk> = emptyList(),
    val language: String? = null,
    val Ω: String? = null
)

/**
 * AI analysis progress for streaming display
 */
data class AIAnalysisProgress(
    val stage: AnalysisStage = AnalysisStage.IDLE,
    val currentFile: String? = null,
    val currentLine: Int? = null,
    val lintOutput: String = "",
    val lintResults: List<LintFileResult> = emptyList(),
    val modifiedCodeRanges: Map<String, List<ModifiedCodeRange>> = emptyMap(),
    val analysisOutput: String = "",
    val reviewFindings: List<cc.unitmesh.agent.ReviewFinding> = emptyList(),
    val planOutput: String = "",
    val fixOutput: String = "",
    val userFeedback: String = "",
    // ComposeRenderer for streaming fix generation (null if not started)
    val fixRenderer: cc.unitmesh.devins.ui.compose.agent.ComposeRenderer? = null
)

/**
 * Represents a modified code range (function, class, etc.) in a file
 */
@Serializable
data class ModifiedCodeRange(
    val filePath: String,
    val elementName: String,
    val elementType: String, // "CLASS", "METHOD", "FUNCTION", etc.
    val startLine: Int,
    val endLine: Int,
    val modifiedLines: List<Int> // Lines that were actually modified within this range
)

/**
 * Stages of AI analysis
 */
enum class AnalysisStage {
    IDLE,
    RUNNING_LINT,
    ANALYZING_LINT,
    GENERATING_PLAN,
    WAITING_FOR_USER_INPUT,
    GENERATING_FIX,
    COMPLETED,
    ERROR
}

/**
 * Result of a single fix for a specific issue
 */
@Serializable
data class FixResult(
    val filePath: String,
    val line: Int,
    val lintIssue: String,
    val lintValid: Boolean,
    val risk: RiskLevel,
    val aiFix: String,
    val fixedCode: String? = null,
    val status: FixStatus
)

/**
 * Risk level of the issue
 */
@Serializable
enum class RiskLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

/**
 * Status of the fix
 */
@Serializable
enum class FixStatus {
    FIXED,       // AI has fixed the issue
    NO_ISSUE,    // No issue found
    SKIPPED,     // User chose to skip
    FAILED       // Fix failed
}

/**
 * Request to get diff from workspace
 */
data class DiffRequest(
    val commitHash: String? = null,  // null = get last commit
    val baseBranch: String? = null,
    val compareWith: String? = null
)

data class CommitInfo(
    val hash: String,
    val shortHash: String,
    val author: String,
    val timestamp: Long,
    val date: String,
    val message: String,
    val issueInfo: IssueInfo? = null, // Issue information extracted from commit message
    val isLoadingIssue: Boolean = false, // Whether issue info is being loaded
    val issueLoadError: String? = null // Error message if issue loading failed (e.g., needs token)
)
