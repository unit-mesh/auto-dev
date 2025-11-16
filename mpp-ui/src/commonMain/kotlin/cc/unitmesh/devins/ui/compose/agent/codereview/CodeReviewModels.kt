package cc.unitmesh.devins.ui.compose.agent.codereview

import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.tool.tracking.ChangeType
import cc.unitmesh.devins.ui.compose.sketch.DiffHunk
import kotlinx.serialization.Serializable

/**
 * Complete state for Code Review side-by-side UI
 */
data class CodeReviewState(
    val isLoading: Boolean = false,
    val isLoadingDiff: Boolean = false, // Loading diff for a specific commit (not blocking UI)
    val error: String? = null,
    val commitHistory: List<CommitInfo> = emptyList(),
    val selectedCommitIndex: Int = 0,
    val diffFiles: List<DiffFileInfo> = emptyList(),
    val selectedFileIndex: Int = 0,
    val aiProgress: AIAnalysisProgress = AIAnalysisProgress(),
    val fixResults: List<FixResult> = emptyList(),
    // Infinite scroll support
    val hasMoreCommits: Boolean = false,
    val isLoadingMore: Boolean = false,
    val totalCommitCount: Int? = null // Total available commits (if known)
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
    val language: String? = null
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
    val fixOutput: String = ""
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

/**
 * Response containing diff information
 */
data class DiffResponse(
    val success: Boolean,
    val error: String? = null,
    val files: List<DiffFileInfo> = emptyList(),
    val commitInfo: CommitInfo? = null
)

/**
 * Information about a commit
 */
data class CommitInfo(
    val hash: String,
    val shortHash: String,
    val author: String,
    val timestamp: Long,
    val date: String,
    val message: String
)
