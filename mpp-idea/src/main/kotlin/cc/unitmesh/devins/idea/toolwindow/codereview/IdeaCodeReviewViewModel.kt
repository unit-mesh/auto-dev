package cc.unitmesh.devins.idea.toolwindow.codereview

import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.agent.ReviewTask
import cc.unitmesh.agent.ReviewType
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.DiffParser
import cc.unitmesh.agent.language.LanguageDetector
import cc.unitmesh.agent.platform.GitOperations
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.workspace.GitFileStatus
import cc.unitmesh.llm.KoogLLMService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Code Review in IntelliJ IDEA plugin.
 * Adapted from mpp-ui's CodeReviewViewModel for IntelliJ platform.
 *
 * Uses mpp-core's GitOperations (JVM implementation) for git operations
 * and JewelRenderer for UI rendering.
 */
class IdeaCodeReviewViewModel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) : Disposable {

    private val projectPath: String = project.basePath ?: ""
    private val gitOps = GitOperations(projectPath)

    // Renderer for agent output
    val renderer = JewelRenderer()

    // State
    private val _state = MutableStateFlow(IdeaCodeReviewState())
    val state: StateFlow<IdeaCodeReviewState> = _state.asStateFlow()

    // Control execution
    private var currentJob: Job? = null
    private var codeReviewAgent: CodeReviewAgent? = null
    private var agentInitialized = false

    init {
        if (projectPath.isEmpty()) {
            updateState { it.copy(error = "No project path available") }
        } else {
            coroutineScope.launch {
                try {
                    loadCommitHistory()
                } catch (e: Exception) {
                    updateState { it.copy(error = "Failed to initialize: ${e.message}") }
                }
            }
        }
    }

    /**
     * Load recent git commits
     */
    suspend fun loadCommitHistory(count: Int = 50) {
        updateState { it.copy(isLoading = true, error = null) }

        try {
            val totalCount = gitOps.getTotalCommitCount()
            val gitCommits = gitOps.getRecentCommits(count)

            val hasMore = totalCount?.let { it > gitCommits.size } ?: false
            val commits = gitCommits.map { git ->
                IdeaCommitInfo(
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
                loadCommitDiff(setOf(0))
            }
        } catch (e: Exception) {
            updateState {
                it.copy(isLoading = false, error = "Failed to load commits: ${e.message}")
            }
        }
    }

    /**
     * Select commits and load their diff
     */
    fun selectCommits(indices: Set<Int>) {
        coroutineScope.launch {
            loadCommitDiff(indices)
        }
    }

    /**
     * Load diff for selected commits
     */
    private suspend fun loadCommitDiff(selectedIndices: Set<Int>) {
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
            it.copy(isLoadingDiff = true, selectedCommitIndices = selectedIndices, error = null)
        }

        try {
            val sortedIndices = selectedIndices.sorted()
            val newestIndex = sortedIndices.first()
            val oldestIndex = sortedIndices.last()

            val currentState = _state.value
            val newestCommit = currentState.commitHistory[newestIndex]
            val oldestCommit = currentState.commitHistory[oldestIndex]

            val gitDiff = if (newestIndex == oldestIndex) {
                gitOps.getCommitDiff(newestCommit.hash)
            } else {
                val hasParent = gitOps.hasParent(oldestCommit.hash)
                if (hasParent) {
                    gitOps.getDiff("${oldestCommit.hash}^", newestCommit.hash)
                } else {
                    gitOps.getDiff("4b825dc642cb6eb9a060e54bf8d69288fbee4904", newestCommit.hash)
                }
            }

            if (gitDiff == null) {
                updateState { it.copy(isLoadingDiff = false, error = "No diff available") }
                return
            }

            val diffFiles = gitDiff.files.map { file ->
                val parsedDiff = DiffParser.parse(file.diff)
                val hunks = parsedDiff.firstOrNull()?.hunks ?: emptyList()

                IdeaDiffFileInfo(
                    path = file.path,
                    oldPath = file.oldPath,
                    changeType = when (file.status) {
                        GitFileStatus.ADDED -> ChangeType.CREATE
                        GitFileStatus.DELETED -> ChangeType.DELETE
                        GitFileStatus.MODIFIED -> ChangeType.EDIT
                        GitFileStatus.RENAMED -> ChangeType.RENAME
                        GitFileStatus.COPIED -> ChangeType.EDIT
                    },
                    hunks = hunks,
                    language = LanguageDetector.detectLanguage(file.path)
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
        } catch (e: Exception) {
            updateState { it.copy(isLoadingDiff = false, error = "Failed to load diff: ${e.message}") }
        }
    }

    /**
     * Select a file from the diff list
     */
    fun selectFile(index: Int) {
        updateState { it.copy(selectedFileIndex = index) }
    }

    /**
     * Start AI analysis on the selected commits
     */
    fun startAnalysis() {
        val currentState = _state.value
        if (currentState.diffFiles.isEmpty()) {
            updateState { it.copy(error = "No files to analyze") }
            return
        }

        currentJob?.cancel()
        currentJob = coroutineScope.launch {
            try {
                updateState {
                    it.copy(
                        aiProgress = IdeaAIAnalysisProgress(stage = IdeaAnalysisStage.RUNNING_LINT),
                        error = null
                    )
                }

                val agent = initializeCodeReviewAgent()
                val filePaths = currentState.diffFiles.map { it.path }

                val additionalContext = buildString {
                    val selectedCommits = currentState.selectedCommitIndices
                        .mapNotNull { currentState.commitHistory.getOrNull(it) }

                    if (selectedCommits.isNotEmpty()) {
                        appendLine("## Selected Commits")
                        selectedCommits.forEach { commit ->
                            appendLine("- ${commit.shortHash}: ${commit.message.lines().firstOrNull()}")
                        }
                        appendLine()
                    }
                }

                val reviewTask = ReviewTask(
                    filePaths = filePaths,
                    reviewType = ReviewType.COMPREHENSIVE,
                    projectPath = projectPath,
                    patch = currentState.originDiff,
                    lintResults = emptyList(),
                    additionalContext = additionalContext
                )

                updateState {
                    it.copy(aiProgress = it.aiProgress.copy(
                        stage = IdeaAnalysisStage.ANALYZING,
                        analysisOutput = "Starting code review analysis...\n"
                    ))
                }

                val analysisOutputBuilder = StringBuilder()
                try {
                    agent.execute(reviewTask) { progressMessage ->
                        analysisOutputBuilder.append(progressMessage)
                        updateState {
                            it.copy(aiProgress = it.aiProgress.copy(
                                analysisOutput = analysisOutputBuilder.toString()
                            ))
                        }
                    }

                    updateState {
                        it.copy(aiProgress = it.aiProgress.copy(stage = IdeaAnalysisStage.COMPLETED))
                    }
                } catch (e: Exception) {
                    analysisOutputBuilder.append("\nError: ${e.message}")
                    updateState {
                        it.copy(aiProgress = it.aiProgress.copy(
                            stage = IdeaAnalysisStage.ERROR,
                            analysisOutput = analysisOutputBuilder.toString()
                        ))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        aiProgress = it.aiProgress.copy(stage = IdeaAnalysisStage.ERROR),
                        error = "Analysis failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Cancel current analysis
     */
    fun cancelAnalysis() {
        currentJob?.cancel()
        updateState { it.copy(aiProgress = IdeaAIAnalysisProgress(stage = IdeaAnalysisStage.IDLE)) }
    }

    /**
     * Initialize the CodeReviewAgent
     */
    private suspend fun initializeCodeReviewAgent(): CodeReviewAgent {
        if (codeReviewAgent != null && agentInitialized) {
            return codeReviewAgent!!
        }

        val toolConfig = ToolConfigFile.default()
        val configWrapper = ConfigManager.load()
        val modelConfig = configWrapper.getActiveModelConfig()
            ?: error("No active model configuration found. Please configure a model in settings.")

        val llmService = KoogLLMService.create(modelConfig)
        val mcpToolConfigService = McpToolConfigService(toolConfig)

        codeReviewAgent = CodeReviewAgent(
            projectPath = projectPath,
            llmService = llmService,
            maxIterations = 50,
            renderer = renderer,
            mcpToolConfigService = mcpToolConfigService,
            enableLLMStreaming = true
        )
        agentInitialized = true

        return codeReviewAgent!!
    }

    private fun updateState(update: (IdeaCodeReviewState) -> IdeaCodeReviewState) {
        _state.value = update(_state.value)
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val date = Date(timestamp * 1000)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    override fun dispose() {
        currentJob?.cancel()
    }
}

