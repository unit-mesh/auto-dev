package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.devins.workspace.Workspace
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class CodeReviewViewModel(private val workspace: Workspace, private val codeReviewAgent: CodeReviewAgent? = null) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // State
    private val _state = MutableStateFlow(CodeReviewState())
    val state: StateFlow<CodeReviewState> = _state.asStateFlow()

    var currentState by mutableStateOf(CodeReviewState())
        internal set

    // Control execution
    private var currentJob: Job? = null

    init {
        // Auto-load diff when ViewModel is created
        scope.launch {
            loadDiff()
        }
    }

    /**
     * Load git diff from workspace
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

            updateState {
                it.copy(
                    isLoading = false,
                    diffFiles = diffFiles,
                    error = null
                )
            }

            // Auto-start analysis if agent is available
            if (codeReviewAgent != null && diffFiles.isNotEmpty()) {
                startAnalysis()
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

    /**
     * Start AI analysis and fix generation
     */
    open fun startAnalysis() {
        if (currentState.diffFiles.isEmpty()) {
            updateState { it.copy(error = "No files to analyze") }
            return
        }

        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                updateState {
                    it.copy(
                        aiProgress = AIAnalysisProgress(stage = AnalysisStage.RUNNING_LINT),
                        error = null
                    )
                }

                // Step 1: Run lint on changed files
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
            loadDiff()
        }
    }

    // Private helper methods

    private suspend fun runLint(filePaths: List<String>) {
        // TODO: Integrate with actual lint tools (eslint, pylint, etc.)
        // For now, simulate lint output

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
        // TODO: Call CodeReviewAgent to analyze lint output
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
        // TODO: Call CodeReviewAgent to generate fixes
        val fixes = listOf(
            FixResult(
                filePath = "src/main/Example.kt",
                line = 42,
                lintIssue = "Unused variable 'temp'",
                lintValid = true,
                risk = RiskLevel.LOW,
                aiFix = "Remove the unused variable",
                fixedCode = "// Variable removed",
                status = FixStatus.FIXED
            ),
            FixResult(
                filePath = "src/main/Example.kt",
                line = 58,
                lintIssue = "Missing null check",
                lintValid = true,
                risk = RiskLevel.MEDIUM,
                aiFix = "Add null check before accessing property",
                fixedCode = "if (value != null) { value.property }",
                status = FixStatus.FIXED
            ),
            FixResult(
                filePath = "src/main/Example.kt",
                line = 73,
                lintIssue = "Inefficient loop",
                lintValid = true,
                risk = RiskLevel.INFO,
                aiFix = "Replace with forEach for better readability",
                fixedCode = "list.forEach { item -> process(item) }",
                status = FixStatus.FIXED
            )
        )

        updateState {
            it.copy(
                fixResults = fixes,
                aiProgress = it.aiProgress.copy(
                    fixOutput = "Generated ${fixes.size} fixes"
                )
            )
        }
    }

    private fun parseDiffHunks(diff: String): List<DiffHunk> {
        // TODO: Parse unified diff format
        // For now, return empty list
        return emptyList()
    }

    private fun detectLanguage(filePath: String): String? {
        return when (filePath.substringAfterLast('.', "")) {
            "kt" -> "kotlin"
            "java" -> "java"
            "js", "ts" -> "javascript"
            "py" -> "python"
            "go" -> "go"
            "rs" -> "rust"
            else -> null
        }
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
