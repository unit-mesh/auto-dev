package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.diff.DiffLineType
import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.language.LanguageDetector
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.agent.platform.GitOperations
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.compose.agent.codereview.analysis.CodeAnalyzer
import cc.unitmesh.devins.ui.compose.agent.codereview.analysis.LintExecutor
import cc.unitmesh.agent.diff.DiffParser
import cc.unitmesh.agent.diff.FileDiff
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.wasm.WasmGitManager
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class CodeReviewViewModel(
    val workspace: Workspace,
    private var codeReviewAgent: CodeReviewAgent? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 在 WebAssembly 平台使用共享的 GitOperations 实例
    private val gitOps = if (Platform.name == "WebAssembly") {
        WasmGitManager.getInstance()
    } else {
        GitOperations(workspace.rootPath ?: "")
    }

    private val analysisRepository = cc.unitmesh.devins.db.CodeReviewAnalysisRepository.getInstance()

    // Non-AI analysis components (extracted for testability)
    private val codeAnalyzer = CodeAnalyzer(workspace)
    private val lintExecutor = LintExecutor()

    // State
    private val _state = MutableStateFlow(CodeReviewState())
    val state: StateFlow<CodeReviewState> = _state.asStateFlow()

    var currentState by mutableStateOf(CodeReviewState())
        internal set

    // Control execution
    private var currentJob: Job? = null

    // Performance optimization: Cache code content to avoid re-reading
    private var codeContentCache: Map<String, String>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_VALIDITY_MS = 30_000L // 30 seconds

    // Performance tracking
    private data class PerformanceMetrics(
        val startTime: Long,
        val phase: String
    )

    private var currentMetrics: PerformanceMetrics? = null

    init {
        // Validate workspace has a valid root path
        if (workspace.rootPath.isNullOrEmpty()) {
            AutoDevLogger.warn("CodeReviewViewModel") {
                "Workspace root path is null or empty, skipping agent initialization"
            }
            updateState {
                it.copy(error = "No workspace path configured. Please open a project first.")
            }
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    codeReviewAgent = initializeCodingAgent()
                    if (gitOps.isSupported()) {
                        loadCommitHistory()
                    } else {
                        loadDiff()
                    }
                } catch (e: Exception) {
                    AutoDevLogger.error("CodeReviewViewModel") {
                        "Failed to initialize: ${e.message}"
                    }
                    e.printStackTrace()
                    updateState {
                        it.copy(error = "Initialization failed: ${e.message}")
                    }
                }
            }
        }
    }

    suspend fun initializeCodingAgent(): CodeReviewAgent {
        codeReviewAgent?.let { return it }

        val projectPath = workspace.rootPath
        if (projectPath.isNullOrEmpty()) {
            throw IllegalStateException("Cannot initialize coding agent: workspace root path is null or empty")
        }

        return createCodeReviewAgent(projectPath)
    }


    /**
     * Load recent git commits (initial load)
     */
    suspend fun loadCommitHistory(count: Int = 50) {
        updateState { it.copy(isLoading = true, error = null) }

        try {
            val totalCount = gitOps.getTotalCommitCount()
            val gitCommits = gitOps.getRecentCommits(count)

            val hasMore = totalCount?.let { it > gitCommits.size } ?: false
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

            val diffFiles = gitDiff.files.map { file ->
                val parsedDiff = DiffParser.parse(file.diff)
                val hunks = parsedDiff.firstOrNull()?.hunks ?: emptyList()

                DiffFileInfo(
                    path = file.path,
                    oldPath = file.oldPath,
                    changeType = when (file.status) {
                        cc.unitmesh.devins.workspace.GitFileStatus.ADDED -> ChangeType.CREATE
                        cc.unitmesh.devins.workspace.GitFileStatus.DELETED -> ChangeType.DELETE
                        cc.unitmesh.devins.workspace.GitFileStatus.MODIFIED -> ChangeType.EDIT
                        cc.unitmesh.devins.workspace.GitFileStatus.RENAMED -> ChangeType.RENAME
                        cc.unitmesh.devins.workspace.GitFileStatus.COPIED -> ChangeType.EDIT
                    },
                    hunks = hunks,
                    language = LanguageDetector.detectLanguage(file.path),
                )
            }

            updateState {
                it.copy(
                    isLoadingDiff = false,
                    diffFiles = diffFiles,
                    selectedFileIndex = 0,
                    error = null,
                    originDiff = gitDiff.originDiff
                )
            }

            // Auto-start analysis if agent is available (for automatic testing)
            if (codeReviewAgent != null && diffFiles.isNotEmpty()) {
                AutoDevLogger.info("CodeReviewViewModel") {
                    "🤖 Auto-starting analysis with ${diffFiles.size} files"
                }
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

        invalidateCodeCache()

        try {
            val gitDiff = gitOps.getDiff(request.baseBranch ?: "master", request.compareWith ?: "HEAD")
            if (gitDiff == null) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "No git diff available. Make sure you have uncommitted changes or specify a commit."
                    )
                }
                return
            }

            val diffFiles = gitDiff.files.map { file ->
                val parsedDiff = DiffParser.parse(file.diff)
                val hunks = parsedDiff.firstOrNull()?.hunks ?: emptyList()

                DiffFileInfo(
                    path = file.path,
                    oldPath = file.oldPath,
                    changeType = when (file.status) {
                        cc.unitmesh.devins.workspace.GitFileStatus.ADDED -> ChangeType.CREATE
                        cc.unitmesh.devins.workspace.GitFileStatus.DELETED -> ChangeType.DELETE
                        cc.unitmesh.devins.workspace.GitFileStatus.MODIFIED -> ChangeType.EDIT
                        cc.unitmesh.devins.workspace.GitFileStatus.RENAMED -> ChangeType.RENAME
                        cc.unitmesh.devins.workspace.GitFileStatus.COPIED -> ChangeType.EDIT
                    },
                    hunks = hunks,
                    language = LanguageDetector.detectLanguage(file.path)
                )
            }

            updateState {
                it.copy(
                    isLoading = false,
                    diffFiles = diffFiles,
                    error = null,
                    originDiff = gitDiff.originDiff
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

                // Step 1: Analyze modified code structure
                val modifiedCodeRanges = analyzeModifiedCode()

                // Step 2: Run lint on modified files
                val filePaths = currentState.diffFiles.map { it.path }
                runLint(filePaths, modifiedCodeRanges)

                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.ANALYZING_LINT)
                    )
                }

                val agent = initializeCodingAgent()
                val reviewTask = cc.unitmesh.agent.ReviewTask(
                    filePaths = filePaths,
                    reviewType = cc.unitmesh.agent.ReviewType.COMPREHENSIVE,
                    projectPath = workspace.rootPath ?: "",
                    patch = currentState.originDiff,
                    lintResults = currentState.aiProgress.lintResults
                )

                val analysisOutputBuilder = StringBuilder()
                updateState {
                    it.copy(aiProgress = it.aiProgress.copy(analysisOutput = "🧠 Starting code review analysis...\n"))
                }

                try {
                    analysisOutputBuilder.appendLine()
                    val agentResult = agent.execute(reviewTask) { progressMessage ->
                        analysisOutputBuilder.append(progressMessage)
                        updateState {
                            it.copy(
                                aiProgress = it.aiProgress.copy(
                                    analysisOutput = analysisOutputBuilder.toString()
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    AutoDevLogger.error("CodeReviewViewModel") { "Failed to execute review task: ${e.message}" }
                    analysisOutputBuilder.append("\n❌ Error: ${e.message}")
                    updateState {
                        it.copy(
                            aiProgress = it.aiProgress.copy(
                                analysisOutput = analysisOutputBuilder.toString()
                            )
                        )
                    }
                }

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

                saveCurrentAnalysisResults()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.ERROR),
                        error = "Analysis failed: ${e.message}"
                    )
                }

                saveCurrentAnalysisResults()
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
        currentJob?.cancel()
        saveCurrentAnalysisResults()

        currentJob = CoroutineScope(Dispatchers.Default).launch {
            updateState {
                it.copy(
                    aiProgress = AIAnalysisProgress(stage = AnalysisStage.IDLE)
                )
            }

            loadCommitDiffInternal(commitHash)
            restoreAnalysisResultsForCommit(commitHash)
        }
    }

    open fun selectFile(index: Int) {
        if (index in currentState.diffFiles.indices) {
            updateState { it.copy(selectedFileIndex = index) }
        }
    }

    open fun getSelectedFile(): DiffFileInfo? {
        return currentState.diffFiles.getOrNull(currentState.selectedFileIndex)
    }

    open fun refresh() {
        println("refresh() called, gitOps.isSupported() = ${gitOps.isSupported()}")
        scope.launch {
            if (gitOps.isSupported()) {
                // 如果支持 Git，就加载提交历史（无论当前是否有数据）
                loadCommitHistory()
            } else {
                loadDiff()
            }
        }
    }

    /**
     * Save current analysis results to database
     */
    private fun saveCurrentAnalysisResults() {
        val currentCommit = currentState.commitHistory.getOrNull(currentState.selectedCommitIndex)
        val projectPath = workspace.rootPath ?: return

        if (currentCommit != null && currentState.aiProgress.stage != AnalysisStage.IDLE) {
            // Only save if there's actual analysis data
            if (currentState.aiProgress.lintResults.isNotEmpty() ||
                currentState.aiProgress.analysisOutput.isNotBlank() ||
                currentState.aiProgress.fixOutput.isNotBlank()
            ) {
                try {
                    analysisRepository.saveAnalysisResult(
                        projectPath = projectPath,
                        commitHash = currentCommit.hash,
                        progress = currentState.aiProgress
                    )

                    AutoDevLogger.info("CodeReviewViewModel") {
                        "Saved analysis results to database for commit ${currentCommit.shortHash}"
                    }
                } catch (e: Exception) {
                    AutoDevLogger.error("CodeReviewViewModel") {
                        "Failed to save analysis results: ${e.message}"
                    }
                }
            }
        }
    }

    /**
     * Restore analysis results from database for a specific commit
     */
    private fun restoreAnalysisResultsForCommit(commitHash: String) {
        val projectPath = workspace.rootPath ?: return

        try {
            val cachedProgress = analysisRepository.getAnalysisResult(projectPath, commitHash)
            if (cachedProgress != null) {
                updateState {
                    it.copy(aiProgress = cachedProgress)
                }

                AutoDevLogger.info("CodeReviewViewModel") {
                    "Restored analysis results from database for commit ${commitHash.take(7)}"
                }
            }
        } catch (e: Exception) {
            AutoDevLogger.error("CodeReviewViewModel") {
                "Failed to restore analysis results: ${e.message}"
            }
        }
    }

    /**
     * Analyze modified code to find which functions/classes were changed.
     * Delegates to CodeAnalyzer for the actual analysis logic.
     */
    suspend fun analyzeModifiedCode(): Map<String, List<ModifiedCodeRange>> {
        val projectPath = workspace.rootPath ?: return emptyMap()
        val modifiedRanges = codeAnalyzer.analyzeModifiedCode(
            diffFiles = currentState.diffFiles,
            projectPath = projectPath,
            progressCallback = { progress ->
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(
                            lintOutput = it.aiProgress.lintOutput + progress
                        )
                    )
                }
            }
        )

        // Update state with results
        updateState {
            it.copy(
                aiProgress = it.aiProgress.copy(
                    modifiedCodeRanges = modifiedRanges
                )
            )
        }

        return modifiedRanges
    }

    /**
     * Detect programming language from file path.
     * Delegates to CodeAnalyzer.
     */
    fun detectLanguageFromPath(filePath: String) = codeAnalyzer.detectLanguageFromPath(filePath)

    suspend fun runLint(
        filePaths: List<String>,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>> = emptyMap()
    ) {
        val projectPath = workspace.rootPath ?: return

        // Delegate to LintExecutor with progress callback
        val lintResults = lintExecutor.runLint(
            filePaths = filePaths,
            projectPath = projectPath,
            modifiedCodeRanges = modifiedCodeRanges,
            progressCallback = { progress ->
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(
                            lintOutput = it.aiProgress.lintOutput + progress
                        )
                    )
                }
            }
        )

        // Update state with results
        updateState {
            it.copy(
                aiProgress = it.aiProgress.copy(
                    lintResults = lintResults
                )
            )
        }
    }



    /**
     * Collect code content for all changed files with caching
     * Cache is valid for 30 seconds to avoid re-reading during analysis stages
     */
    suspend fun collectCodeContent(): Map<String, String> {
        // Check if cache is still valid
        val currentTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (codeContentCache != null && (currentTime - cacheTimestamp) < CACHE_VALIDITY_MS) {
            AutoDevLogger.info("CodeReviewViewModel") {
                "Using cached code content (${codeContentCache!!.size} files)"
            }
            return codeContentCache!!
        }

        val startTime = currentTime
        val codeContent = mutableMapOf<String, String>()

        for (diffFile in currentState.diffFiles) {
            if (diffFile.changeType == ChangeType.DELETE) continue

            try {
                val content = workspace.fileSystem.readFile(diffFile.path)
                if (content != null) {
                    codeContent[diffFile.path] = content
                }
            } catch (e: Exception) {
                AutoDevLogger.warn("CodeReviewViewModel") {
                    "Failed to read ${diffFile.path}: ${e.message}"
                }
            }
        }

        // Update cache
        codeContentCache = codeContent
        cacheTimestamp = currentTime

        val duration = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startTime
        AutoDevLogger.info("CodeReviewViewModel") {
            "Collected ${codeContent.size} files in ${duration}ms"
        }

        return codeContent
    }

    /**
     * Invalidate code content cache (call when files might have changed)
     */
    private fun invalidateCodeCache() {
        codeContentCache = null
        cacheTimestamp = 0
    }

    /**
     * Generate fixes with structured context
     * Delegates to CodeReviewAgent for fix generation
     */
    suspend fun generateFixes() {
        try {
            val fixOutputBuilder = StringBuilder()
            fixOutputBuilder.appendLine("🔧 Generating actionable fixes...")
            fixOutputBuilder.appendLine()

            updateState {
                it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
            }

            // Collect code snippets around issues for better context
            fixOutputBuilder.appendLine("📖 Collecting code context...")
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
            }

            val codeContent = collectCodeContent()

            fixOutputBuilder.appendLine("✅ Generating fixes with AI...")
            fixOutputBuilder.appendLine()
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
            }

            // Initialize agent if needed
            val agent = initializeCodingAgent()

            // Delegate to CodeReviewAgent
            val result = agent.generateFixes(
                codeContent = codeContent,
                lintResults = currentState.aiProgress.lintResults,
                analysisOutput = currentState.aiProgress.analysisOutput,
                language = "ZH",
                onProgress = { chunk ->
                    fixOutputBuilder.append(chunk)
                    updateState {
                        it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
                    }
                }
            )

            if (!result.success) {
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(
                            fixOutput = fixOutputBuilder.toString() + "\n" + result.content
                        )
                    )
                }
            }

        } catch (e: Exception) {
            AutoDevLogger.error("CodeReviewViewModel") { "Failed to generate fixes: ${e.message}" }
            updateState {
                it.copy(
                    aiProgress = it.aiProgress.copy(
                        fixOutput = "Error generating fixes: ${e.message}\n"
                    )
                )
            }
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
     * Apply a diff patch to the workspace
     */
    open fun applyDiffPatch(diffPatch: String) {
        scope.launch {
            try {
                AutoDevLogger.info("CodeReviewViewModel") {
                    "Applying diff patch..."
                }

                // Parse the diff patch to extract file path and changes
                val fileDiffs = DiffParser.parse(diffPatch)

                if (fileDiffs.isEmpty()) {
                    AutoDevLogger.warn("CodeReviewViewModel") {
                        "Failed to parse diff patch"
                    }
                    updateState {
                        it.copy(error = "Failed to parse diff patch")
                    }
                    return@launch
                }

                var appliedCount = 0
                var failedCount = 0

                fileDiffs.forEach { fileDiff ->
                    val targetPath = fileDiff.newPath ?: fileDiff.oldPath
                    if (targetPath == null || targetPath == "/dev/null") {
                        AutoDevLogger.warn("CodeReviewViewModel") {
                            "Skipping invalid file path"
                        }
                        failedCount++
                        return@forEach
                    }

                    try {
                        // Apply the diff patch using the applyDiffPatchToFile helper
                        val success = applyDiffPatchToFile(targetPath, fileDiff)
                        if (success) {
                            appliedCount++
                            AutoDevLogger.info("CodeReviewViewModel") {
                                "Successfully applied patch to $targetPath"
                            }
                        } else {
                            failedCount++
                            AutoDevLogger.warn("CodeReviewViewModel") {
                                "Failed to apply patch to $targetPath"
                            }
                        }
                    } catch (e: Exception) {
                        failedCount++
                        AutoDevLogger.error("CodeReviewViewModel") {
                            "Error applying patch to $targetPath: ${e.message}"
                        }
                    }
                }

                // Show result message
                val message = buildString {
                    if (appliedCount > 0) {
                        append("✅ Applied $appliedCount patch${if (appliedCount > 1) "es" else ""}")
                    }
                    if (failedCount > 0) {
                        if (appliedCount > 0) append(", ")
                        append("❌ $failedCount failed")
                    }
                }

                AutoDevLogger.info("CodeReviewViewModel") {
                    message
                }

            } catch (e: Exception) {
                AutoDevLogger.error("CodeReviewViewModel") {
                    "Failed to apply diff patch: ${e.message}"
                }
                updateState {
                    it.copy(error = "Failed to apply diff patch: ${e.message}")
                }
            }
        }
    }

    /**
     * Apply a single file diff patch to the workspace
     */
    private suspend fun applyDiffPatchToFile(
        filePath: String,
        fileDiff: FileDiff
    ): Boolean {
        try {
            // Read the current file content
            val currentContent = workspace.fileSystem.readFile(filePath) ?: ""
            // Handle empty content properly - String.lines() returns [""] for empty string
            val currentLines = if (currentContent.isEmpty()) {
                mutableListOf()
            } else {
                currentContent.lines().toMutableList()
            }

            // Track the offset between original line numbers and current line numbers
            // This is needed because insertions/deletions shift subsequent line numbers
            var lineOffset = 0

            // Apply each hunk
            fileDiff.hunks.forEach { hunk ->
                // Convert hunk's old line numbers to current line numbers using offset
                // For new files, oldStartLine is 0, so we start at index 0
                var currentLineIndex = maxOf(0, hunk.oldStartLine - 1) + lineOffset
                var oldLineNum = maxOf(1, hunk.oldStartLine)

                hunk.lines.forEach { diffLine ->
                    when (diffLine.type) {
                        DiffLineType.CONTEXT -> {
                            // Context line - verify it matches
                            if (currentLineIndex < currentLines.size) {
                                if (currentLines[currentLineIndex].trim() != diffLine.content.trim()) {
                                    AutoDevLogger.warn("CodeReviewViewModel") {
                                        "Context mismatch at line ${oldLineNum}: expected '${diffLine.content}', got '${currentLines[currentLineIndex]}'"
                                    }
                                }
                                currentLineIndex++
                                oldLineNum++
                            }
                        }
                        DiffLineType.DELETED -> {
                            // Delete line
                            if (currentLineIndex < currentLines.size) {
                                currentLines.removeAt(currentLineIndex)
                                lineOffset-- // Deletion shifts subsequent lines up
                                oldLineNum++
                                // Don't increment currentLineIndex - the next line is now at this index
                            }
                        }
                        DiffLineType.ADDED -> {
                            // Add line
                            if (currentLineIndex <= currentLines.size) {
                                currentLines.add(currentLineIndex, diffLine.content)
                                lineOffset++ // Insertion shifts subsequent lines down
                                currentLineIndex++
                                // Don't increment oldLineNum - added lines aren't in the old file
                            }
                        }
                        DiffLineType.HEADER -> {
                            // Skip header lines
                        }
                    }
                }
            }

            // Write the modified content back to the file
            val newContent = currentLines.joinToString("\n")
            workspace.fileSystem.writeFile(filePath, newContent)

            return true
        } catch (e: Exception) {
            AutoDevLogger.error("CodeReviewViewModel") {
                "Error applying patch to $filePath: ${e.message}"
            }
            return false
        }
    }

    /**
     * Reject a diff patch (user decided not to apply it)
     */
    open fun rejectDiffPatch(diffPatch: String) {
        AutoDevLogger.info("CodeReviewViewModel") {
            "User rejected diff patch"
        }

        // Parse the diff to log which files were rejected
        val fileDiffs = DiffParser.parse(diffPatch)
        fileDiffs.forEach { fileDiff ->
            val targetPath = fileDiff.newPath ?: fileDiff.oldPath
            AutoDevLogger.info("CodeReviewViewModel") {
                "Rejected patch for: $targetPath"
            }
        }
    }

    /**
     * Cleanup when ViewModel is disposed
     */
    open fun dispose() {
        currentJob?.cancel()
        scope.cancel()
    }

    companion object {
        suspend fun createCodeReviewAgent(projectPath: String): CodeReviewAgent {
            // Validate project path
            if (projectPath.isEmpty()) {
                throw IllegalArgumentException("Project path cannot be empty")
            }

            try {
                val toolConfig = ToolConfigFile.default()

                val configWrapper = ConfigManager.load()
                val modelConfig = configWrapper.getActiveModelConfig()
                    ?: throw IllegalStateException("No active model configuration found. Please configure a model in settings.")

                val llmService = KoogLLMService.create(modelConfig)

                val mcpToolConfigService = McpToolConfigService(toolConfig)
                // Create renderer
                val renderer = ComposeRenderer()
                val agent = CodeReviewAgent(
                    projectPath = projectPath,
                    llmService = llmService,
                    maxIterations = 50,
                    renderer = renderer,
                    mcpToolConfigService = mcpToolConfigService,
                    enableLLMStreaming = true
                )

                return agent
            } catch (e: Exception) {
                AutoDevLogger.error("CodeReviewViewModel") {
                    "Failed to create CodeReviewAgent: ${e.message}"
                }
                throw IllegalStateException("Failed to create CodeReviewAgent: ${e.message}", e)
            }
        }
    }
}
