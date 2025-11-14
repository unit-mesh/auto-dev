package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.agent.platform.GitOperations
import cc.unitmesh.agent.tool.tracking.ChangeType
import cc.unitmesh.devins.ui.compose.sketch.DiffParser
import cc.unitmesh.devins.workspace.Workspace
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class CodeReviewViewModel(
    val workspace: Workspace,
    private val codeReviewAgent: CodeReviewAgent? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gitOps = GitOperations(workspace.rootPath ?: "")

    // State
    private val _state = MutableStateFlow(CodeReviewState())
    val state: StateFlow<CodeReviewState> = _state.asStateFlow()

    var currentState by mutableStateOf(CodeReviewState())
        internal set

    // Control execution
    private var currentJob: Job? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            if (gitOps.isSupported()) {
                loadCommitHistory()
            } else {
                // Fallback to loading diff for platforms without git support
                loadDiff()
            }
        }
    }

    /**
     * Load recent git commits (initial load)
     */
    suspend fun loadCommitHistory(count: Int = 50) {
        updateState { it.copy(isLoading = true, error = null) }

        try {
            // Get total commit count
            val totalCount = gitOps.getTotalCommitCount()

            // Get recent commits
            val gitCommits = gitOps.getRecentCommits(count)

            val hasMore = totalCount?.let { it > gitCommits.size } ?: false

            // Convert GitCommitInfo to CommitInfo
            val commits = gitCommits.map { git ->
                CommitInfo(
                    hash = git.hash,
                    shortHash = git.shortHash,
                    author = git.author,
                    timestamp = git.date,
                    date = formatDate(git.date),
                    message = git.message
                )
            }

            updateState {
                it.copy(
                    isLoading = false,
                    commitHistory = commits,
                    selectedCommitIndex = 0,
                    hasMoreCommits = hasMore,
                    totalCommitCount = totalCount,
                    error = null
                )
            }

            if (commits.isNotEmpty()) {
                loadCommitDiffInternal(commits[0].hash)
            }

        } catch (e: Exception) {
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Failed to load commits: ${e.message}"
                )
            }
        }
    }

    /**
     * Load more commits for infinite scroll
     */
    suspend fun loadMoreCommits(batchSize: Int = 50) {
        if (currentState.isLoadingMore || !currentState.hasMoreCommits) {
            return
        }

        updateState { it.copy(isLoadingMore = true) }

        try {
            val currentCount = currentState.commitHistory.size
            val gitCommits = gitOps.getRecentCommits(currentCount + batchSize + 1)

            // Skip already loaded commits
            val newCommits = gitCommits.drop(currentCount)
            val hasMore = newCommits.size > batchSize
            val commitsToAdd = if (hasMore) newCommits.take(batchSize) else newCommits

            val additionalCommits = commitsToAdd.map { git ->
                CommitInfo(
                    hash = git.hash,
                    shortHash = git.shortHash,
                    author = git.author,
                    timestamp = git.date,
                    date = formatDate(git.date),
                    message = git.message
                )
            }

            updateState {
                it.copy(
                    commitHistory = it.commitHistory + additionalCommits,
                    hasMoreCommits = hasMore,
                    isLoadingMore = false
                )
            }

        } catch (e: Exception) {
            updateState {
                it.copy(
                    isLoadingMore = false,
                    error = "Failed to load more commits: ${e.message}"
                )
            }
        }
    }

    /**
     * Load diff for a specific commit by hash
     */
    private suspend fun loadCommitDiffInternal(commitHash: String) {
        // Find the commit index by hash
        val index = currentState.commitHistory.indexOfFirst { it.hash == commitHash }
        if (index < 0) {
            updateState {
                it.copy(
                    isLoadingDiff = false,
                    error = "Commit not found: $commitHash"
                )
            }
            return
        }

        val commit = currentState.commitHistory[index]

        updateState {
            it.copy(
                isLoadingDiff = true,
                selectedCommitIndex = index,
                error = null
            )
        }

        try {
            val gitDiff = gitOps.getCommitDiff(commit.hash)

            if (gitDiff == null) {
                updateState {
                    it.copy(
                        isLoadingDiff = false,
                        error = "No diff available for this commit"
                    )
                }
                return
            }

            // Convert to UI model using DiffParser
            val diffFiles = gitDiff.files.map { file ->
                val parsedDiff = DiffParser.parse(file.diff)
                val hunks = parsedDiff.firstOrNull()?.hunks ?: emptyList()

                DiffFileInfo(
                    path = file.path,
                    oldPath = file.oldPath,
                    changeType = when (file.status) {
                        cc.unitmesh.devins.workspace.GitFileStatus.ADDED -> cc.unitmesh.agent.tool.tracking.ChangeType.CREATE
                        cc.unitmesh.devins.workspace.GitFileStatus.DELETED -> cc.unitmesh.agent.tool.tracking.ChangeType.DELETE
                        cc.unitmesh.devins.workspace.GitFileStatus.MODIFIED -> cc.unitmesh.agent.tool.tracking.ChangeType.EDIT
                        cc.unitmesh.devins.workspace.GitFileStatus.RENAMED -> cc.unitmesh.agent.tool.tracking.ChangeType.RENAME
                        cc.unitmesh.devins.workspace.GitFileStatus.COPIED -> cc.unitmesh.agent.tool.tracking.ChangeType.EDIT
                    },
                    hunks = hunks,
                    language = detectLanguage(file.path)
                )
            }

            updateState {
                it.copy(
                    isLoadingDiff = false,
                    diffFiles = diffFiles,
                    selectedFileIndex = 0,
                    error = null
                )
            }

            // Auto-start analysis if agent is available (for automatic testing)
            if (codeReviewAgent != null && diffFiles.isNotEmpty()) {
                AutoDevLogger.info("CodeReviewViewModel") {
                    "🤖 Auto-starting analysis with ${diffFiles.size} files"
                }
                startAnalysis()
            }

        } catch (e: Exception) {
            updateState {
                it.copy(
                    isLoadingDiff = false,
                    error = "Failed to load diff: ${e.message}"
                )
            }
        }
    }

    /**
     * Load git diff from workspace (fallback for platforms without git support)
     */
    suspend fun loadDiff(request: DiffRequest = DiffRequest()) {
        updateState { it.copy(isLoading = true, error = null) }

        try {
            // Get diff from workspace
            val gitDiff = workspace.getGitDiff(request.baseBranch, request.compareWith)

            if (gitDiff == null) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "No git diff available. Make sure you have uncommitted changes or specify a commit."
                    )
                }
                return
            }

            // Convert to UI model using DiffParser
            val diffFiles = gitDiff.files.map { file ->
                val parsedDiff = DiffParser.parse(file.diff)
                val hunks = parsedDiff.firstOrNull()?.hunks ?: emptyList()

                DiffFileInfo(
                    path = file.path,
                    oldPath = file.oldPath,
                    changeType = when (file.status) {
                        cc.unitmesh.devins.workspace.GitFileStatus.ADDED -> cc.unitmesh.agent.tool.tracking.ChangeType.CREATE
                        cc.unitmesh.devins.workspace.GitFileStatus.DELETED -> cc.unitmesh.agent.tool.tracking.ChangeType.DELETE
                        cc.unitmesh.devins.workspace.GitFileStatus.MODIFIED -> cc.unitmesh.agent.tool.tracking.ChangeType.EDIT
                        cc.unitmesh.devins.workspace.GitFileStatus.RENAMED -> cc.unitmesh.agent.tool.tracking.ChangeType.RENAME
                        cc.unitmesh.devins.workspace.GitFileStatus.COPIED -> ChangeType.EDIT
                    },
                    hunks = hunks,
                    language = detectLanguage(file.path)
                )
            }

            updateState {
                it.copy(
                    isLoading = false,
                    diffFiles = diffFiles,
                    error = null
                )
            }
        } catch (e: Exception) {
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Failed to load diff: ${e.message}"
                )
            }
        }
    }

    open fun startAnalysis() {
        if (currentState.diffFiles.isEmpty()) {
            updateState { it.copy(error = "No files to analyze") }
            return
        }

        currentJob?.cancel()
        currentJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                updateState {
                    it.copy(
                        aiProgress = AIAnalysisProgress(stage = AnalysisStage.RUNNING_LINT),
                        error = null
                    )
                }

                val filePaths = currentState.diffFiles.map { it.path }
                runLint(filePaths)

                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.ANALYZING_LINT)
                    )
                }
                analyzeLintOutput()

                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.GENERATING_FIX)
                    )
                }

                generateFixes()

                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.COMPLETED)
                    )
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.ERROR),
                        error = "Analysis failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Cancel current analysis
     */
    open fun cancelAnalysis() {
        currentJob?.cancel()
        updateState {
            it.copy(
                aiProgress = AIAnalysisProgress(stage = AnalysisStage.IDLE)
            )
        }
    }

    /**
     * Select a different commit to view by hash
     * @param commitHash The git commit hash
     */
    open fun selectCommit(commitHash: String) {
        // Cancel previous loading job if any
        currentJob?.cancel()
        currentJob = CoroutineScope(Dispatchers.Default).launch {
            loadCommitDiffInternal(commitHash)
        }
    }

    /**
     * Select a different commit to view by index (deprecated, use selectCommit(hash) instead)
     * @param index The index in the commit history list
     */
    @Deprecated("Use selectCommit(commitHash: String) instead", ReplaceWith("selectCommit(commitHistory[index].hash)"))
    open fun selectCommitByIndex(index: Int) {
        if (index in currentState.commitHistory.indices) {
            selectCommit(currentState.commitHistory[index].hash)
        }
    }

    /**
     * Select a different file to view
     */
    open fun selectFile(index: Int) {
        if (index in currentState.diffFiles.indices) {
            updateState { it.copy(selectedFileIndex = index) }
        }
    }

    /**
     * Get currently selected file
     */
    open fun getSelectedFile(): DiffFileInfo? {
        return currentState.diffFiles.getOrNull(currentState.selectedFileIndex)
    }

    /**
     * Refresh diff
     */
    open fun refresh() {
        scope.launch {
            if (gitOps.isSupported() && currentState.commitHistory.isNotEmpty()) {
                loadCommitHistory()
            } else {
                loadDiff()
            }
        }
    }

    suspend fun runLint(filePaths: List<String>) {

    }

    suspend fun analyzeLintOutput() {

    }

    suspend fun generateFixes() {

    }

    private fun updateState(update: (CodeReviewState) -> CodeReviewState) {
        currentState = update(currentState)
        _state.value = currentState
    }

    /**
     * Protected method for subclasses to update parent state
     */
    protected fun updateParentState(update: (CodeReviewState) -> CodeReviewState) {
        updateState(update)
    }

    /**
     * Cleanup when ViewModel is disposed
     */
    open fun dispose() {
        currentJob?.cancel()
        scope.cancel()
    }
}
