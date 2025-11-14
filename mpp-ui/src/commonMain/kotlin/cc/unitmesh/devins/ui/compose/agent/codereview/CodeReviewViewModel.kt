package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.agent.platform.GitOperations
import cc.unitmesh.agent.tool.tracking.ChangeType
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.compose.agent.codereview.analysis.CodeAnalyzer
import cc.unitmesh.devins.ui.compose.agent.codereview.analysis.DiffContextBuilder
import cc.unitmesh.devins.ui.compose.agent.codereview.analysis.LintExecutor
import cc.unitmesh.devins.ui.compose.agent.codereview.analysis.LintResultFormatter
import cc.unitmesh.devins.ui.compose.sketch.DiffParser
import cc.unitmesh.devins.ui.config.ConfigManager
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
    private val gitOps = GitOperations(workspace.rootPath ?: "")

    // Non-AI analysis components (extracted for testability)
    private val codeAnalyzer = CodeAnalyzer(workspace)
    private val lintExecutor = LintExecutor()
    private val lintResultFormatter = LintResultFormatter()
    private val diffContextBuilder = DiffContextBuilder()

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
        CoroutineScope(Dispatchers.Default).launch {
            codeReviewAgent = initializeCodingAgent()
            if (gitOps.isSupported()) {
                loadCommitHistory()
            } else {
                // Fallback to loading diff for platforms without git support
                loadDiff()
            }
        }
    }

    suspend fun initializeCodingAgent(): CodeReviewAgent {
        codeReviewAgent?.let { return it }

        return createCodeReviewAgent(workspace.rootPath ?: "")
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

        // Invalidate cache when loading new diff
        invalidateCodeCache()

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

    /**
     * Analyze modified code to find which functions/classes were changed.
     * Delegates to CodeAnalyzer for the actual analysis logic.
     */
    suspend fun analyzeModifiedCode(): Map<String, List<ModifiedCodeRange>> {
        val projectPath = workspace.rootPath ?: return emptyMap()

        // Delegate to CodeAnalyzer with progress callback
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
     * Analyze lint output using Data-Driven prompt approach
     * This method collects all necessary data upfront and uses the optimized
     * CodeReviewAnalysisTemplate for efficient, reliable analysis
     */
    suspend fun analyzeLintOutput() {
        val phaseStartTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

        try {
            val analysisOutputBuilder = StringBuilder()
            analysisOutputBuilder.appendLine("🤖 Analyzing code with AI (Data-Driven)...")
            analysisOutputBuilder.appendLine()

            updateState {
                it.copy(aiProgress = it.aiProgress.copy(analysisOutput = analysisOutputBuilder.toString()))
            }

            // Collect code content for all changed files
            analysisOutputBuilder.appendLine("📖 Reading code files...")
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(analysisOutput = analysisOutputBuilder.toString()))
            }

            val dataCollectStart = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val codeContent = collectCodeContent()

            // Collect lint results
            val lintResultsMap = formatLintResults()

            // Build diff context
            val diffContext = buildDiffContext()

            val dataCollectDuration = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - dataCollectStart

            analysisOutputBuilder.appendLine("✅ Data collected in ${dataCollectDuration}ms (${codeContent.size} files)")
            analysisOutputBuilder.appendLine("🧠 Generating analysis prompt...")
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(analysisOutput = analysisOutputBuilder.toString()))
            }

            // Get LLM service
            val configWrapper = ConfigManager.load()
            val modelConfig = configWrapper.getActiveModelConfig()!!
            val llmService = KoogLLMService.create(modelConfig)

            // Use the optimized Data-Driven prompt
            val promptRenderer = cc.unitmesh.agent.CodeReviewAgentPromptRenderer()
            val prompt = promptRenderer.renderAnalysisPrompt(
                reviewType = "COMPREHENSIVE",
                filePaths = codeContent.keys.toList(),
                codeContent = codeContent,
                lintResults = lintResultsMap,
                diffContext = diffContext,
                language = "EN"
            )

            val promptLength = prompt.length
            analysisOutputBuilder.appendLine("📊 Prompt size: ${promptLength} chars (~${promptLength / 4} tokens)")
            analysisOutputBuilder.appendLine("⚡ Streaming AI response...")
            analysisOutputBuilder.appendLine()
            updateState {
                it.copy(aiProgress = it.aiProgress.copy(analysisOutput = analysisOutputBuilder.toString()))
            }

            // Stream the LLM response - single-shot analysis, no tool calls
            val llmStartTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            llmService.streamPrompt(prompt, compileDevIns = false).collect { chunk ->
                analysisOutputBuilder.append(chunk)
                updateState {
                    it.copy(aiProgress = it.aiProgress.copy(analysisOutput = analysisOutputBuilder.toString()))
                }
            }

            val totalDuration = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - phaseStartTime
            val llmDuration = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - llmStartTime

            AutoDevLogger.info("CodeReviewViewModel") {
                "Analysis complete: Total ${totalDuration}ms (Data: ${dataCollectDuration}ms, LLM: ${llmDuration}ms)"
            }

            updateState {
                it.copy(aiProgress = it.aiProgress.copy(analysisOutput = analysisOutputBuilder.toString()))
            }

        } catch (e: Exception) {
            AutoDevLogger.error("CodeReviewViewModel") { "Failed to analyze lint output: ${e.message}" }
            updateState {
                it.copy(
                    aiProgress = it.aiProgress.copy(
                        analysisOutput = "Error analyzing lint output: ${e.message}\n"
                    )
                )
            }
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
     * Format lint results for analysis prompt.
     * Delegates to LintResultFormatter.
     */
    fun formatLintResults(): Map<String, String> {
        return lintResultFormatter.formatLintResults(currentState.aiProgress.lintResults)
    }

    /**
     * Build diff context showing what was changed.
     * Delegates to DiffContextBuilder.
     */
    private fun buildDiffContext(): String {
        return diffContextBuilder.buildDiffContext(
            diffFiles = currentState.diffFiles,
            modifiedCodeRanges = currentState.aiProgress.modifiedCodeRanges
        )
    }

    /**
     * Generate fixes with structured context
     * Uses code content, lint results, and analysis to provide actionable fixes
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

            // Get LLM service
            val configWrapper = ConfigManager.load()
            val modelConfig = configWrapper.getActiveModelConfig()!!
            val llmService = KoogLLMService.create(modelConfig)

            // Build structured prompt for fix generation
            val prompt = buildFixGenerationPrompt(codeContent)

            // Stream the LLM response
            llmService.streamPrompt(prompt, compileDevIns = false).collect { chunk ->
                fixOutputBuilder.append(chunk)
                updateState {
                    it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
                }
            }

            updateState {
                it.copy(aiProgress = it.aiProgress.copy(fixOutput = fixOutputBuilder.toString()))
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

    /**
     * Build structured prompt for fix generation with all necessary context
     */
    private fun buildFixGenerationPrompt(codeContent: Map<String, String>): String {
        return buildString {
            appendLine("# Code Fix Generation")
            appendLine()
            appendLine("Based on the analysis below, provide **specific, actionable code fixes**.")
            appendLine()

            // Include original code
            if (codeContent.isNotEmpty()) {
                appendLine("## Original Code")
                appendLine()
                codeContent.forEach { (path, content) ->
                    appendLine("### File: $path")
                    appendLine("```")
                    appendLine(content)
                    appendLine("```")
                    appendLine()
                }
            }

            // Include lint results
            if (currentState.aiProgress.lintResults.isNotEmpty()) {
                appendLine("## Lint Issues")
                appendLine()
                currentState.aiProgress.lintResults.forEach { fileResult ->
                    if (fileResult.issues.isNotEmpty()) {
                        val totalCount = fileResult.errorCount + fileResult.warningCount + fileResult.infoCount
                        appendLine("### ${fileResult.filePath}")
                        appendLine("Total Issues: $totalCount (${fileResult.errorCount} errors, ${fileResult.warningCount} warnings)")
                        appendLine()

                        // Group by severity
                        val critical = fileResult.issues.filter { it.severity == LintSeverityUI.ERROR }
                        val warnings = fileResult.issues.filter { it.severity == LintSeverityUI.WARNING }

                        if (critical.isNotEmpty()) {
                            appendLine("**Critical Issues:**")
                            critical.forEach { issue ->
                                appendLine("- Line ${issue.line}: ${issue.message}")
                                if (issue.rule?.isNotBlank() == true) appendLine("  Rule: ${issue.rule}")
                            }
                            appendLine()
                        }

                        if (warnings.isNotEmpty()) {
                            appendLine("**Warnings:**")
                            warnings.take(5).forEach { issue ->
                                appendLine("- Line ${issue.line}: ${issue.message}")
                                if (issue.rule?.isNotBlank() == true) appendLine("  Rule: ${issue.rule}")
                            }
                            if (warnings.size > 5) {
                                appendLine("... and ${warnings.size - 5} more warnings")
                            }
                            appendLine()
                        }
                    }
                }
            }

            // Include AI analysis summary
            if (currentState.aiProgress.analysisOutput.isNotBlank()) {
                appendLine("## AI Analysis")
                appendLine()
                appendLine(currentState.aiProgress.analysisOutput)
                appendLine()
            }

            // Clear instructions for fix generation
            appendLine("## Your Task")
            appendLine()
            appendLine("For **each critical issue** (errors first, then important warnings), provide:")
            appendLine()
            appendLine("### Fix Format:")
            appendLine("```")
            appendLine("#### Issue: [Brief title]")
            appendLine("**File**: `path/to/file.kt`")
            appendLine("**Line**: `123`")
            appendLine("**Problem**: [Clear explanation]")
            appendLine()
            appendLine("**Fix**:")
            appendLine("```kotlin")
            appendLine("// Fixed code here")
            appendLine("```")
            appendLine()
            appendLine("**Explanation**: [Why this fix works]")
            appendLine("```")
            appendLine()
            appendLine("**Guidelines**:")
            appendLine("1. Prioritize critical issues (errors) over warnings")
            appendLine("2. Provide complete, runnable code fixes")
            appendLine("3. Explain the reasoning behind each fix")
            appendLine("4. Group related fixes together")
            appendLine("5. For large refactorings, provide step-by-step instructions")
            appendLine()
            appendLine("**DO NOT** attempt to use tools. All necessary information is provided above.")
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

    companion object {
        suspend fun createCodeReviewAgent(projectPath: String): CodeReviewAgent {
            val toolConfig = ToolConfigFile.default()

            val configWrapper = ConfigManager.load()
            val modelConfig = configWrapper.getActiveModelConfig()!!
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
        }
    }
}
