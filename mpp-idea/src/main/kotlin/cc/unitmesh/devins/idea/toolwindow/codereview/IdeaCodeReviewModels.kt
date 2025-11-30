package cc.unitmesh.devins.idea.toolwindow.codereview

import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.DiffHunk

/**
 * State for Code Review UI in IntelliJ IDEA plugin.
 * Adapted from mpp-ui's CodeReviewState.
 */
data class IdeaCodeReviewState(
    val isLoading: Boolean = false,
    val isLoadingDiff: Boolean = false,
    val error: String? = null,
    val commitHistory: List<IdeaCommitInfo> = emptyList(),
    val selectedCommitIndices: Set<Int> = emptySet(),
    val diffFiles: List<IdeaDiffFileInfo> = emptyList(),
    val selectedFileIndex: Int = 0,
    val aiProgress: IdeaAIAnalysisProgress = IdeaAIAnalysisProgress(),
    val hasMoreCommits: Boolean = false,
    val isLoadingMore: Boolean = false,
    val totalCommitCount: Int? = null,
    val originDiff: String? = null
)

/**
 * Information about a commit.
 */
data class IdeaCommitInfo(
    val hash: String,
    val shortHash: String,
    val author: String,
    val timestamp: Long,
    val date: String,
    val message: String
)

/**
 * Information about a file in the diff.
 */
data class IdeaDiffFileInfo(
    val path: String,
    val oldPath: String? = null,
    val changeType: ChangeType = ChangeType.EDIT,
    val hunks: List<DiffHunk> = emptyList(),
    val language: String? = null
)

/**
 * AI analysis progress for streaming display.
 */
data class IdeaAIAnalysisProgress(
    val stage: IdeaAnalysisStage = IdeaAnalysisStage.IDLE,
    val currentFile: String? = null,
    val analysisOutput: String = "",
    val planOutput: String = "",
    val fixOutput: String = ""
)

/**
 * Stages of AI analysis.
 */
enum class IdeaAnalysisStage {
    IDLE,
    RUNNING_LINT,
    ANALYZING,
    GENERATING_PLAN,
    GENERATING_FIX,
    COMPLETED,
    ERROR
}

