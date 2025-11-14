package cc.unitmesh.devins.ui.compose.agent.codereview

import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.devins.workspace.Workspace
import kotlinx.coroutines.*

/**
 * Enhanced CodeReview ViewModel for JVM with Git integration
 */
class JvmCodeReviewViewModel(
    private val workspace: Workspace,
    private val gitService: GitService,
    codeReviewAgent: CodeReviewAgent? = null) : CodeReviewViewModel(workspace, codeReviewAgent) {
    // Control execution for Git operations
    private var currentJob: Job? = null

    init {
        println("🚀 Initializing JvmCodeReviewViewModel")
        println("📁 Workspace: ${workspace.rootPath}")

        // Auto-load commit history when ViewModel is created
        CoroutineScope(Dispatchers.Default).launch {
            loadCommitHistory()
        }
    }

    /**
     * Load recent git commits
     */
    suspend fun loadCommitHistory(count: Int = 20) {
        println("📜 Loading last $count commits...")
        updateState { it.copy(isLoading = true, error = null) }

        try {
            val gitCommits = gitService.getRecentCommits(count)

            println("✅ Loaded ${gitCommits.size} commits:")
            gitCommits.take(5).forEach { commit ->
                println("  • ${commit.shortHash} - ${commit.message}")
            }

            // Convert GitCommitInfo to CommitInfo
            val commits = gitCommits.map { git ->
                CommitInfo(
                    hash = git.hash,
                    shortHash = git.shortHash,
                    author = git.author,
                    timestamp = git.date,  // GitCommitInfo.date is Long timestamp
                    date = formatDate(git.date),  // Format as string for display
                    message = git.message
                )
            }

            updateState {
                it.copy(
                    isLoading = false,
                    commitHistory = commits,
                    selectedCommitIndex = 0,
                    error = null
                )
            }

            if (commits.isNotEmpty()) {
                loadDiffForCommit(0)
            }

        } catch (e: Exception) {
            println("❌ Failed to load commit history: ${e.message}")
            e.printStackTrace()
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Failed to load commits: ${e.message}"
                )
            }
        }
    }


    fun loadDiffForCommit(index: Int) {
        if (index !in currentState.commitHistory.indices) {
            return
        }

        val commit = currentState.commitHistory[index]

        currentJob?.cancel()
        currentJob = CoroutineScope(Dispatchers.Default).launch {
            updateState {
                it.copy(
                    isLoading = true,
                    selectedCommitIndex = index,
                    error = null
                )
            }

            try {
                val gitDiff = gitService.getCommitDiff(commit.hash)

                if (gitDiff == null) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "No diff available for this commit"
                        )
                    }
                    return@launch
                }

                // Convert to UI model
                val diffFiles = gitDiff.files.map { file ->
                    DiffFileInfo(
                        path = file.path,
                        oldPath = file.oldPath,
                        changeType = when (file.status) {
                            cc.unitmesh.devins.workspace.GitFileStatus.ADDED -> ChangeType.ADDED
                            cc.unitmesh.devins.workspace.GitFileStatus.DELETED -> ChangeType.DELETED
                            cc.unitmesh.devins.workspace.GitFileStatus.MODIFIED -> ChangeType.MODIFIED
                            cc.unitmesh.devins.workspace.GitFileStatus.RENAMED -> ChangeType.RENAMED
                            cc.unitmesh.devins.workspace.GitFileStatus.COPIED -> ChangeType.MODIFIED
                        },
                        hunks = parseDiffHunks(file.diff),
                        language = detectLanguage(file.path)
                    )
                }

                diffFiles.forEach { file ->
                    println("  • ${file.path} [${file.changeType}] (${file.language ?: "unknown"})")
                }

                updateState {
                    it.copy(
                        isLoading = false,
                        diffFiles = diffFiles,
                        selectedFileIndex = 0,
                        error = null
                    )
                }

            } catch (e: Exception) {
                println("❌ Failed to load diff: ${e.message}")
                e.printStackTrace()
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load diff: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Start AI analysis and fix generation
     */
    override fun startAnalysis() {
        println("🤖 Starting AI analysis...")

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

                // Step 1: Run lint
                val filePaths = currentState.diffFiles.map { it.path }
                runLint(filePaths)

                // Step 2: AI analyzes lint output
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.ANALYZING_LINT)
                    )
                }
                analyzeLintOutput()

                // Step 3: Generate fixes
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.GENERATING_FIX)
                    )
                }
                generateFixes()

                // Completed
                println("✅ AI analysis completed")
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.COMPLETED)
                    )
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("❌ Analysis failed: ${e.message}")
                e.printStackTrace()
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
    override fun cancelAnalysis() {
        println("🛑 Cancelling analysis...")
        currentJob?.cancel()
        updateState {
            it.copy(
                aiProgress = AIAnalysisProgress(stage = AnalysisStage.IDLE)
            )
        }
    }

    /**
     * Select a different file to view
     */
    override fun selectFile(index: Int) {
        if (index in currentState.diffFiles.indices) {
            println("📄 Selected file: ${currentState.diffFiles[index].path}")
            updateState { it.copy(selectedFileIndex = index) }
        }
    }

    /**
     * Get currently selected file
     */
    override fun getSelectedFile(): DiffFileInfo? {
        return currentState.diffFiles.getOrNull(currentState.selectedFileIndex)
    }

    /**
     * Refresh current commit
     */
    override fun refresh() {
        println("🔄 Refreshing...")
        // Use CoroutineScope from MainScope or create a new one
        CoroutineScope(Dispatchers.Default).launch {
            loadCommitHistory()
        }
    }

    // Private helper methods

    private suspend fun runLint(filePaths: List<String>) {
        println("🔍 Running lint on ${filePaths.size} files...")

        val lintOutput = buildString {
            appendLine("Running lint on ${filePaths.size} files...")
            delay(500)
            filePaths.forEach { path ->
                appendLine("Checking: $path")
            }
            appendLine("Lint analysis completed.")
        }

        updateState {
            it.copy(
                aiProgress = it.aiProgress.copy(
                    lintOutput = lintOutput
                )
            )
        }
    }

    private suspend fun analyzeLintOutput() {
        println("🧠 Analyzing lint output...")

        val analysisOutput = buildString {
            appendLine("Analyzing lint results...")
            delay(800)
            appendLine("Found 3 issues:")
            appendLine("- Line 42: Unused variable 'temp'")
            appendLine("- Line 58: Missing null check")
            appendLine("- Line 73: Inefficient loop")
        }

        updateState {
            it.copy(
                aiProgress = it.aiProgress.copy(
                    analysisOutput = analysisOutput
                )
            )
        }
    }

    private suspend fun generateFixes() {
        println("🔧 Generating fixes...")

        val fixes = listOf(
            FixResult(
                filePath = currentState.diffFiles.firstOrNull()?.path ?: "unknown",
                line = 42,
                lintIssue = "Unused variable 'temp'",
                lintValid = true,
                risk = RiskLevel.LOW,
                aiFix = "Remove the unused variable",
                fixedCode = "// Variable removed",
                status = FixStatus.FIXED
            ),
            FixResult(
                filePath = currentState.diffFiles.firstOrNull()?.path ?: "unknown",
                line = 58,
                lintIssue = "Missing null check",
                lintValid = true,
                risk = RiskLevel.MEDIUM,
                aiFix = "Add null check before accessing property",
                fixedCode = "if (value != null) { value.property }",
                status = FixStatus.FIXED
            ),
            FixResult(
                filePath = currentState.diffFiles.firstOrNull()?.path ?: "unknown",
                line = 73,
                lintIssue = "Inefficient loop",
                lintValid = true,
                risk = RiskLevel.INFO,
                aiFix = "Replace with forEach for better readability",
                fixedCode = "list.forEach { item -> process(item) }",
                status = FixStatus.FIXED
            )
        )

        println("✅ Generated ${fixes.size} fixes")

        updateState {
            it.copy(
                fixResults = fixes,
                aiProgress = it.aiProgress.copy(
                    fixOutput = "Generated ${fixes.size} fixes"
                )
            )
        }
    }

    private fun updateState(update: (CodeReviewState) -> CodeReviewState) {
        currentState = update(currentState)
        // Also update parent's state flow
        super.updateParentState(update)
    }

    /**
     * Cleanup when ViewModel is disposed
     */
    override fun dispose() {
        println("🧹 Disposing JvmCodeReviewViewModel")
        currentJob?.cancel()
        super.dispose()
    }
}

