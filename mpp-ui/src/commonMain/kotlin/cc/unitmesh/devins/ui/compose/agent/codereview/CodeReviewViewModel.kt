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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

    // Cache remote URL for repository operations
    private var cachedRemoteUrl: String? = null

    // Non-AI analysis components (extracted for testability)
    private val codeAnalyzer = CodeAnalyzer(workspace)
    private val lintExecutor = LintExecutor()

    // Issue tracker service
    private val issueService = IssueService(workspace.rootPath ?: "")

    // State
    private val _state = MutableStateFlow(CodeReviewState())
    val state: StateFlow<CodeReviewState> = _state.asStateFlow()

    var currentState by mutableStateOf(CodeReviewState())
        internal set

    // Notification events
    private val _notificationEvent = kotlinx.coroutines.flow.MutableSharedFlow<Pair<String, String>>()
    val notificationEvent = _notificationEvent.asSharedFlow()

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
                    // Fetch and cache remote URL
                    cachedRemoteUrl = try {
                        if (gitOps.isSupported()) {
                            gitOps.getRemoteUrl("origin")
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        AutoDevLogger.warn("CodeReviewViewModel") {
                            "Failed to get remote URL: ${e.message}"
                        }
                        null
                    }
                    AutoDevLogger.info("CodeReviewViewModel") {
                        "Cached remote URL: ${cachedRemoteUrl ?: "(not available)"}"
                    }

                    codeReviewAgent = initializeCodingAgent()
                    // Initialize issue service
                    issueService.initialize(if (gitOps.isSupported()) gitOps else null)
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
                    selectedCommitIndices = if (commits.isNotEmpty()) setOf(0) else emptySet(),
                    hasMoreCommits = hasMore,
                    totalCommitCount = totalCount,
                    error = null
                )
            }

            if (commits.isNotEmpty()) {
                loadCommitDiffInternal(setOf(0))
                // Load issue info for first commit asynchronously
                loadIssueForCommit(0)
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

            // Load issue info for newly loaded commits asynchronously
            val startIndex = currentCount
            additionalCommits.forEachIndexed { offset, _ ->
                loadIssueForCommit(startIndex + offset)
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
     * Load diff for selected commits (supports range selection)
     */
    private suspend fun loadCommitDiffInternal(selectedIndices: Set<Int>) {
        if (selectedIndices.isEmpty()) {
            updateState {
                it.copy(
                    isLoadingDiff = false,
                    selectedCommitIndices = emptySet(),
                    diffFiles = emptyList(),
                    error = null
                )
            }
            return
        }

        updateState {
            it.copy(
                isLoadingDiff = true,
                selectedCommitIndices = selectedIndices,
                error = null
            )
        }

        try {
            // Determine range: oldest to newest
            val sortedIndices = selectedIndices.sorted()
            val newestIndex = sortedIndices.first() // 0 is newest
            val oldestIndex = sortedIndices.last()

            val newestCommit = currentState.commitHistory[newestIndex]
            val oldestCommit = currentState.commitHistory[oldestIndex]

            val gitDiff = if (newestIndex == oldestIndex) {
                // Single commit
                gitOps.getCommitDiff(newestCommit.hash)
            } else {
                // Range diff: oldest^..newest
                // Check if oldest commit has a parent
                val hasParent = gitOps.hasParent(oldestCommit.hash)
                if (hasParent) {
                    // Use parent notation for commits with parents
                    gitOps.getDiff("${oldestCommit.hash}^", newestCommit.hash)
                } else {
                    // For root commit, get diff from empty tree
                    gitOps.getDiff("4b825dc642cb6eb9a060e54bf8d69288fbee4904", newestCommit.hash)
                }
            }

            if (gitDiff == null) {
                updateState {
                    it.copy(
                        isLoadingDiff = false,
                        error = "No diff available for the selected range"
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
                
                // Build additional context including issue information if available
                // Build additional context including issue information if available
                val additionalContext = buildString {
                    val selectedCommits = currentState.selectedCommitIndices.mapNotNull { currentState.commitHistory.getOrNull(it) }
                    
                    if (selectedCommits.isNotEmpty()) {
                        appendLine("## Selected Commits")
                        selectedCommits.forEach { commit -> 
                            appendLine("- ${commit.shortHash}: ${commit.message.lines().firstOrNull()}")
                        }
                        appendLine()
                    }

                    val issues = selectedCommits.mapNotNull { it.issueInfo }.distinctBy { it.id }
                    if (issues.isNotEmpty()) {
                        appendLine("## Related Issue Information")
                        issues.forEach { issue ->
                            appendLine()
                            appendLine("**Issue #${issue.id}**: ${issue.title}")
                            appendLine("**Status**: ${issue.status}")
                            if (issue.description.isNotBlank()) {
                                appendLine()
                                appendLine("**Description**:")
                                appendLine(issue.description)
                            }
                            if (issue.labels.isNotEmpty()) {
                                appendLine()
                                appendLine("**Labels**: ${issue.labels.joinToString(", ")}")
                            }
                        }
                        appendLine()
                    }
                }
                
                val reviewTask = cc.unitmesh.agent.ReviewTask(
                    filePaths = filePaths,
                    reviewType = cc.unitmesh.agent.ReviewType.COMPREHENSIVE,
                    projectPath = workspace.rootPath ?: "",
                    patch = currentState.originDiff,
                    lintResults = currentState.aiProgress.lintResults,
                    additionalContext = additionalContext
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
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.WAITING_FOR_USER_INPUT)
                    )
                }

                _notificationEvent.emit("Analysis Complete" to "Code analysis finished. Please review and provide instructions.")

                // Wait for user input, do not generate fixes automatically
                // generateFixes()

                saveCurrentAnalysisResults()

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
     * Select a commit or toggle selection
     * @param index The index of the commit in the list
     * @param toggle Whether to toggle selection (multi-select) or replace it
     */
    open fun selectCommit(index: Int, toggle: Boolean = false) {
        currentJob?.cancel()
        saveCurrentAnalysisResults()

        currentJob = CoroutineScope(Dispatchers.Default).launch {
            updateState {
                it.copy(
                    aiProgress = AIAnalysisProgress(stage = AnalysisStage.IDLE)
                )
            }

            val newSelection = if (toggle) {
                if (currentState.selectedCommitIndices.contains(index)) {
                    currentState.selectedCommitIndices - index
                } else {
                    currentState.selectedCommitIndices + index
                }
            } else {
                setOf(index)
            }

            // Ensure at least one commit is selected if we're not in a "deselect all" mode (which shouldn't happen usually)
            // But if user deselects the last one, maybe we should allow empty? For now let's allow empty.
            
            loadCommitDiffInternal(newSelection)
            
            // Restore analysis results only if single commit selected (complexity with multi-commit analysis storage)
            if (newSelection.size == 1) {
                val commit = currentState.commitHistory.getOrNull(newSelection.first())
                if (commit != null) {
                    restoreAnalysisResultsForCommit(commit.hash)
                }
            }
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
        // Only save for single commit selection to avoid complexity
        if (currentState.selectedCommitIndices.size != 1) return
        
        val index = currentState.selectedCommitIndices.first()
        val currentCommit = currentState.commitHistory.getOrNull(index)
        val projectPath = workspace.rootPath ?: return

        if (currentCommit != null && currentState.aiProgress.stage != AnalysisStage.IDLE) {
            // Only save if there's actual analysis data
            if (currentState.aiProgress.lintResults.isNotEmpty() ||
                currentState.aiProgress.analysisOutput.isNotBlank() ||
                currentState.aiProgress.fixOutput.isNotBlank()
            ) {
                try {
                    analysisRepository.saveAnalysisResult(
                        remoteUrl = cachedRemoteUrl,
                        projectPath = projectPath,
                        commitHash = currentCommit.hash,
                        progress = currentState.aiProgress
                    )

                    AutoDevLogger.info("CodeReviewViewModel") {
                        "Saved analysis results to database for commit ${currentCommit.shortHash} (remoteUrl: ${cachedRemoteUrl ?: "N/A"})"
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
            val cachedProgress = analysisRepository.getAnalysisResult(
                remoteUrl = cachedRemoteUrl,
                projectPath = projectPath,
                commitHash = commitHash
            )
            if (cachedProgress != null) {
                updateState {
                    it.copy(aiProgress = cachedProgress)
                }

                AutoDevLogger.info("CodeReviewViewModel") {
                    "Restored analysis results from database for commit ${commitHash.take(7)} (remoteUrl: ${cachedRemoteUrl ?: "N/A"})"
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
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
            }

            // Get git diff/patch for changed code context
            val patch = currentState.originDiff
            if (patch.isNullOrBlank()) {
                fixOutputBuilder.appendLine("❌ Error: No git diff available for fix generation")
                updateState {
                    it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
                }
                return
            }

            fixOutputBuilder.appendLine("📖 Using git diff for code context...")
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
            }

            fixOutputBuilder.appendLine("✅ Generating fixes with AI...")
            fixOutputBuilder.appendLine()
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
            }

            // Initialize agent if needed
            val agent = initializeCodingAgent()

            // Delegate to CodeReviewAgent with patch instead of full code content
            val result = agent.generateFixes(
                patch = patch,
                lintResults = currentState.aiProgress.lintResults,
                analysisOutput = currentState.aiProgress.analysisOutput,
                userFeedback = currentState.aiProgress.userFeedback,
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



    fun proceedToGenerateFixes(feedback: String) {
        // Cancel any existing job if needed, though usually we are in WAITING state
        currentJob?.cancel()

        updateState {
            it.copy(
                aiProgress = it.aiProgress.copy(
                    userFeedback = feedback,
                    stage = AnalysisStage.GENERATING_FIX
                )
            )
        }

        currentJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                generateFixes()

                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.COMPLETED)
                    )
                }

                saveCurrentAnalysisResults()
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = AnalysisStage.ERROR),
                        error = "Fix generation failed: ${e.message}"
                    )
                }
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
     * Load issue information for a specific commit asynchronously
     *
     * @param commitIndex Index of the commit in commitHistory
     */
    private fun loadIssueForCommit(commitIndex: Int) {
        val commit = currentState.commitHistory.getOrNull(commitIndex) ?: return

        // Skip if already loaded or loading
        if (commit.issueInfo != null || commit.isLoadingIssue) {
            return
        }

        // Mark as loading
        updateCommitAtIndex(commitIndex) { it.copy(isLoadingIssue = true, issueLoadError = null) }

        // Load issue asynchronously
        scope.launch {
            try {
                val issueDeferred = issueService.getIssueAsync(commit.hash, commit.message)
                val result = issueDeferred.await()

                // Update commit with issue info or error
                updateCommitAtIndex(commitIndex) {
                    it.copy(
                        issueInfo = result.issueInfo,
                        isLoadingIssue = false,
                        issueLoadError = result.error
                    )
                }
            } catch (e: Exception) {
                AutoDevLogger.error("CodeReviewViewModel") {
                    "Failed to load issue for commit ${commit.shortHash}: ${e.message}"
                }
                updateCommitAtIndex(commitIndex) {
                    it.copy(
                        isLoadingIssue = false,
                        issueLoadError = "Failed to load issue"
                    )
                }
            }
        }
    }

    /**
     * Update a commit at a specific index
     */
    private fun updateCommitAtIndex(index: Int, update: (CommitInfo) -> CommitInfo) {
        val commits = currentState.commitHistory
        if (index !in commits.indices) return

        val updatedCommits = commits.toMutableList()
        updatedCommits[index] = update(updatedCommits[index])

        updateState { it.copy(commitHistory = updatedCommits) }
    }

    /**
     * Detect repository information from Git remote URL
     * @return Pair of (owner, repo) or null if not detected
     */
    suspend fun detectRepositoryFromGit(): Pair<String, String>? {
        return try {
            if (!gitOps.isSupported()) {
                return null
            }

            val remoteUrl = gitOps.getRemoteUrl("origin")
            if (remoteUrl != null) {
                cc.unitmesh.agent.tracker.GitHubIssueTracker.parseRepoUrl(remoteUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            AutoDevLogger.warn("CodeReviewViewModel") {
                "Failed to detect repository from Git: ${e.message}"
            }
            null
        }
    }

    /**
     * Reload issue service (called when configuration changes)
     */
    suspend fun reloadIssueService() {
        try {
            AutoDevLogger.info("CodeReviewViewModel") {
                "Reloading issue service..."
            }
            issueService.reload(if (gitOps.isSupported()) gitOps else null)

            // Reload issues for all commits
            currentState.commitHistory.forEachIndexed { index, commit ->
                // Reset issue info
                updateCommitAtIndex(index) { it.copy(issueInfo = null, isLoadingIssue = false, issueLoadError = null) }
                // Reload
                loadIssueForCommit(index)
            }

            AutoDevLogger.info("CodeReviewViewModel") {
                "Issue service reloaded successfully"
            }
        } catch (e: Exception) {
            AutoDevLogger.error("CodeReviewViewModel") {
                "Failed to reload issue service: ${e.message}"
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
