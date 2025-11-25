package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.codereview.ModifiedCodeRange
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
import cc.unitmesh.agent.util.WalkthroughExtractor
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.wasm.WasmGitManager
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class CodeReviewViewModel(
    val workspace: Workspace,
    private var codeReviewAgent: CodeReviewAgent? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
            error("Cannot initialize coding agent: workspace root path is null or empty")
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

            // Find related tests
            findRelatedTests()

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

            // Find related tests
            findRelatedTests()
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

                val additionalContext = buildString {
                    val selectedCommits =
                        currentState.selectedCommitIndices.mapNotNull { currentState.commitHistory.getOrNull(it) }

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

                // Generate modification plan after analysis
                generateModificationPlan()

                _notificationEvent.emit("Analysis Complete" to "Code analysis finished. Please review the modification plan.")

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
     * Generate modification plan based on analysis results
     */
    suspend fun generateModificationPlan() {
        try {
            updateState {
                it.copy(
                    aiProgress = it.aiProgress.copy(stage = AnalysisStage.GENERATING_PLAN)
                )
            }

            val planOutputBuilder = StringBuilder()
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(planOutput = "💡 生成修改建议...\n"))
            }

            // Use CodeReviewAgentPromptRenderer to generate structured prompt
            val promptRenderer = cc.unitmesh.agent.CodeReviewAgentPromptRenderer()

            val planPrompt = promptRenderer.renderModificationPlanPrompt(
                lintResults = currentState.aiProgress.lintResults,
                analysisOutput = WalkthroughExtractor.extract(currentState.aiProgress.analysisOutput),
                modifiedCodeRanges = currentState.aiProgress.modifiedCodeRanges,
                language = "ZH"  // Use Chinese for better user experience
            )

            try {
                // Create a temporary LLM service for plan generation
                val configWrapper = ConfigManager.load()
                val modelConfig = configWrapper.getActiveModelConfig()
                    ?: error("No active model configuration found")

                val llmService = KoogLLMService.create(modelConfig)

                // Use LLM service to generate plan with streaming
                llmService.streamPrompt(planPrompt, compileDevIns = false).collect { chunk ->
                    planOutputBuilder.append(chunk)
                    updateState {
                        it.copy(
                            aiProgress = it.aiProgress.copy(
                                planOutput = planOutputBuilder.toString()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                AutoDevLogger.error("CodeReviewViewModel") { "LLM call failed during plan generation: ${e.message}" }
                planOutputBuilder.append("\n❌ Error: ${e.message}")
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(
                            planOutput = planOutputBuilder.toString()
                        )
                    )
                }
            }

            updateState {
                it.copy(
                    aiProgress = it.aiProgress.copy(stage = AnalysisStage.WAITING_FOR_USER_INPUT)
                )
            }

        } catch (e: Exception) {
            AutoDevLogger.error("CodeReviewViewModel") { "Failed to generate modification plan: ${e.message}" }
            updateState {
                it.copy(
                    aiProgress = it.aiProgress.copy(
                        planOutput = "\n❌ Error generating plan: ${e.message}",
                        stage = AnalysisStage.WAITING_FOR_USER_INPUT
                    )
                )
            }
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
     * Find related test files for all changed files
     */
    suspend fun findRelatedTests() {
        if (currentState.diffFiles.isEmpty()) {
            AutoDevLogger.info("CodeReviewViewModel") {
                "findRelatedTests: No diff files to process"
            }
            return
        }

        AutoDevLogger.info("CodeReviewViewModel") {
            "findRelatedTests: Starting test discovery for ${currentState.diffFiles.size} files"
        }

        updateState { it.copy(isLoadingTests = true) }

        try {
            val testMap = mutableMapOf<String, List<TestFileInfo>>()

            for (diffFile in currentState.diffFiles) {
                if (diffFile.changeType == ChangeType.DELETE) continue

                val testFiles = TestFinderFactory.findTests(
                    sourceFile = diffFile.path,
                    language = diffFile.language,
                    workspace = workspace
                )

                if (testFiles.isNotEmpty()) {
                    testMap[diffFile.path] = testFiles
                }
            }

            updateState {
                it.copy(
                    relatedTests = testMap,
                    isLoadingTests = false
                )
            }

            AutoDevLogger.info("CodeReviewViewModel") {
                "findRelatedTests: Completed! Found ${testMap.values.sumOf { it.size }} test files for ${testMap.size} changed files"
            }
        } catch (e: Exception) {
            AutoDevLogger.error("CodeReviewViewModel") {
                "Failed to find related tests: ${e.message}"
            }
            e.printStackTrace()
            updateState { it.copy(isLoadingTests = false) }
        }
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
     * Creates CodingAgent directly with ComposeRenderer for timeline display
     */
    suspend fun generateFixes() {
        try {
            val fixRenderer = ComposeRenderer()

            // Initialize the renderer in state
            updateState {
                it.copy(
                    aiProgress = it.aiProgress.copy(
                        fixRenderer = fixRenderer,
                        fixOutput = ""
                    )
                )
            }

            // Get git diff/patch for changed code context
            val patch = currentState.originDiff
            if (patch.isNullOrBlank()) {
                fixRenderer.renderError("No git diff available for fix generation")
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(
                            fixOutput = "❌ Error: No git diff available for fix generation"
                        )
                    )
                }
                return
            }

            // Build user feedback from selected plan items
            val selectedItemsFeedback = if (selectedPlanItems.isNotEmpty() && currentState.aiProgress.planOutput.isNotBlank()) {
                formatSelectedPlanItemsAsFeedback(
                    planOutput = currentState.aiProgress.planOutput,
                    selectedIndices = selectedPlanItems
                )
            } else {
                ""
            }

            // Combine user feedback with selected plan items feedback
            var combinedUserFeedback = buildString {
                if (currentState.aiProgress.userFeedback.isNotBlank()) {
                    appendLine(currentState.aiProgress.userFeedback)
                    if (selectedItemsFeedback.isNotBlank()) {
                        appendLine()
                    }
                }
                if (selectedItemsFeedback.isNotBlank()) {
                    append(selectedItemsFeedback)
                }
            }.trim()

            if (combinedUserFeedback.isEmpty() && currentState.aiProgress.planOutput.isNotEmpty()) {
                combinedUserFeedback = currentState.aiProgress.planOutput
            }

            AutoDevLogger.info("CodeReviewViewModel") {
                "Generating fixes with ${selectedPlanItems.size} selected plan items"
            }

            val agent = initializeCodingAgent()

            // Execute fix generation using the agent
            fixRenderer.addUserMessage("Generating fixes...")

            val result = agent.generateFixes(
                patch = patch,
                lintResults = currentState.aiProgress.lintResults,
                analysisOutput = currentState.aiProgress.analysisOutput,
                userFeedback = combinedUserFeedback,
                language = "ZH",
                renderer = fixRenderer
            ) { progress ->
                // Progress callback
                val currentOutput = currentState.aiProgress.fixOutput
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(
                            fixOutput = currentOutput + progress
                        )
                    )
                }
            }

            AutoDevLogger.info("CodeReviewViewModel") {
                "Fix generation completed - success: ${result.success}"
            }

            if (!result.success) {
                fixRenderer.renderError(result.content)
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(
                            fixOutput = currentState.aiProgress.fixOutput + "\n❌ Error: ${result.content}"
                        )
                    )
                }
            }

        } catch (e: Exception) {
            AutoDevLogger.error("CodeReviewViewModel") { "Failed to generate fixes: ${e.message}" }
            currentState.aiProgress.fixRenderer?.renderError("Error generating fixes: ${e.message}")
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
     * Store selected plan items for use in fix generation
     */
    private var selectedPlanItems: Set<Int> = emptySet()

    fun setSelectedPlanItems(items: Set<Int>) {
        selectedPlanItems = items
        AutoDevLogger.debug("CodeReviewViewModel") {
            "Selected plan items: ${items.joinToString()}"
        }
    }

    fun openFile(filePath: String, startLine: Int? = null, endLine: Int? = null) {
        // Get workspace root path and construct absolute path
        val root = workspace.rootPath
        val absolutePath = if (root != null && !filePath.startsWith("/") && !filePath.startsWith(root)) {
            "$root/$filePath"
        } else {
            filePath
        }

        AutoDevLogger.info("CodeReviewViewModel") {
            "Opening file: $absolutePath (original: $filePath, root: $root, lines: ${startLine?.let { "$it-$endLine" } ?: "all"})"
        }

        // Update state to show file viewer dialog
        updateState {
            it.copy(
                fileViewerPath = absolutePath,
                fileViewerStartLine = startLine,
                fileViewerEndLine = endLine
            )
        }
    }

    /**
     * Close file viewer dialog
     */
    fun closeFileViewer() {
        updateState {
            it.copy(
                fileViewerPath = null,
                fileViewerStartLine = null,
                fileViewerEndLine = null
            )
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
                    ?: error("No active model configuration found. Please configure a model in settings.")

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

        /**
         * Format selected plan items as user feedback for fix generation
         * This converts the selected plan items into a structured text that guides
         * the AI to focus on specific issues from the modification plan
         */
        fun formatSelectedPlanItemsAsFeedback(planOutput: String, selectedIndices: Set<Int>): String {
            if (selectedIndices.isEmpty()) {
                return ""
            }

            // Parse plan items from the plan output
            val planItems = PlanParser.parse(planOutput)
            val selectedItems = planItems.filter { it.number in selectedIndices }

            if (selectedItems.isEmpty()) {
                AutoDevLogger.warn("CodeReviewViewModel") {
                    "No plan items found for selected indices: ${selectedIndices.joinToString()}"
                }
                return ""
            }

            return buildString {
                appendLine("## 用户选择的修复项 (User Selected Items)")
                appendLine()
                appendLine("请优先修复以下选中的问题项：")
                appendLine()

                selectedItems.forEach { item ->
                    appendLine("### ${item.number}. ${item.title} - ${item.priority}")

                    // Include file paths from steps
                    val filePaths = item.getAllFilePaths()
                    if (filePaths.isNotEmpty()) {
                        appendLine("**相关文件**:")
                        filePaths.forEach { path ->
                            appendLine("- $path")
                        }
                        appendLine()
                    }

                    // Include step details
                    if (item.steps.isNotEmpty()) {
                        appendLine("**修复步骤**:")
                        item.steps.forEachIndexed { index, step ->
                            val statusIcon = when (step.status) {
                                StepStatus.COMPLETED -> "✓"
                                StepStatus.FAILED -> "!"
                                StepStatus.IN_PROGRESS -> "*"
                                StepStatus.TODO -> "-"
                            }
                            appendLine("$statusIcon ${step.text}")
                        }
                        appendLine()
                    }
                }

                appendLine("---")
                appendLine()
                appendLine("**注意**: 请只修复上述选中的问题项，其他未选中的项可以忽略。")
            }
        }
    }
}
