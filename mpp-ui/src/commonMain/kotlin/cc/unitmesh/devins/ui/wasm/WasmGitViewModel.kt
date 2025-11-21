package cc.unitmesh.devins.ui.wasm

import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.platform.GitCloneErrorType
import cc.unitmesh.agent.platform.GitCloneException
import cc.unitmesh.agent.platform.GitOperations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Git 操作的日志类型
 */
enum class LogType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    DEBUG
}

/**
 * Git 日志条目
 */
data class GitLog(
    val message: String,
    val type: LogType = LogType.INFO,
    val timestamp: Long = Platform.getCurrentTimestamp()
)

/**
 * Git 提交信息（用于 UI 显示）
 */
data class GitCommitDisplay(
    val hash: String,
    val author: String,
    val email: String,
    val date: Long,
    val message: String
) {
    val dateFormatted: String
        get() {
            val instant = Instant.fromEpochMilliseconds(date * 1000) // Git timestamp is in seconds
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            return "${dateTime.year}-${
                dateTime.monthNumber.toString().padStart(2, '0')
            }-${dateTime.dayOfMonth.toString().padStart(2, '0')} " +
                "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
        }
}

/**
 * UI 状态
 */
data class WasmGitUiState(
    val repoUrl: String = "https://github.com/phodal-archive/mini-file",
    val targetDir: String = "",
    val isLoading: Boolean = false,
    val cloneSuccess: Boolean = false,
    val errorMessage: String? = null,
    val logs: List<GitLog> = emptyList(),
    val commits: List<GitCommitDisplay> = emptyList()
)

/**
 * Wasm Git ViewModel
 * 管理 Git 操作和 UI 状态
 *
 * @param gitOperations 共享的 GitOperations 实例（通过 WasmGitManager 获取）
 */
class WasmGitViewModel(
    private val gitOperations: GitOperations = WasmGitManager.getInstance()
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _uiState = MutableStateFlow(WasmGitUiState())
    val uiState: StateFlow<WasmGitUiState> = _uiState.asStateFlow()

    init {
        addLog("Git client initialized", LogType.INFO)
        addLog("Default repository: ${_uiState.value.repoUrl}", LogType.DEBUG)

        if (!Platform.isWasm) {
            addLog("Note: Git operations are best supported on WebAssembly platform", LogType.WARNING)
        }
    }

    fun updateRepoUrl(url: String) {
        _uiState.value = _uiState.value.copy(repoUrl = url, errorMessage = null)
    }

    fun updateTargetDir(dir: String) {
        _uiState.value = _uiState.value.copy(targetDir = dir, errorMessage = null)
    }

    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logs = emptyList())
    }

    private fun addLog(message: String, type: LogType = LogType.INFO) {
        _uiState.value = _uiState.value.copy(
            logs = _uiState.value.logs + GitLog(message, type)
        )
    }

    /**
     * 克隆仓库
     */
    suspend fun cloneRepository() {
        val currentState = _uiState.value
        if (currentState.repoUrl.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "Repository URL is required")
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            cloneSuccess = false,
            errorMessage = null,
            commits = emptyList()
        )

        addLog("Starting repository clone...", LogType.INFO)
        addLog("Repository: ${currentState.repoUrl}", LogType.DEBUG)

        scope.launch {
            try {
                // 检查是否支持
                if (!gitOperations.isSupported()) {
                    addLog("Error: Git operations not supported on this platform", LogType.ERROR)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Git operations not supported on this platform"
                    )
                    return@launch
                }

                addLog("Git operations ready (using shared instance)", LogType.DEBUG)

                // 执行克隆
                val targetDir = currentState.targetDir.ifBlank { null }

                addLog("Cloning repository: ${currentState.repoUrl}", LogType.INFO)

                try {
                    val success = gitOperations.performClone(currentState.repoUrl, targetDir)

                    if (success) {
                        addLog("Clone completed successfully!", LogType.SUCCESS)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            cloneSuccess = true
                        )

                        // 自动获取提交历史
                        fetchCommitHistory()
                    }
                } catch (e: GitCloneException) {
                    // Handle specific Git clone errors with user-friendly messages
                    val userMessage = when (e.errorType) {
                        GitCloneErrorType.CORS_ERROR -> {
                            addLog("All clone attempts failed", LogType.ERROR)
                            
                            // Show detailed errors from all attempts
                            e.message?.let { msg ->
                                msg.lines().forEach { line ->
                                    if (line.isNotBlank()) {
                                        addLog(line, LogType.WARNING)
                                    }
                                }
                            }
                            
                            addLog("", LogType.INFO)
                            addLog("Suggestions:", LogType.INFO)
                            addLog("1. Download the repository as a ZIP file from GitHub", LogType.INFO)
                            addLog("2. Use a backend proxy server", LogType.INFO)
                            addLog("3. Clone on server side instead of in browser", LogType.INFO)
                            
                            "Clone failed due to CORS restrictions. Please download as ZIP or use a backend proxy."
                        }
                        GitCloneErrorType.NETWORK_ERROR -> {
                            addLog("Network error: ${e.message}", LogType.ERROR)
                            addLog("Please check your internet connection", LogType.WARNING)
                            
                            "Network error: ${e.message}. Please check your connection and try again."
                        }
                        GitCloneErrorType.INITIALIZATION_ERROR -> {
                            addLog("Git initialization failed: ${e.message}", LogType.ERROR)
                            
                            "Git module failed to initialize. Please refresh the page and try again."
                        }
                        GitCloneErrorType.CLONE_FAILED -> {
                            addLog("Clone failed: ${e.message}", LogType.ERROR)
                            
                            "Clone failed: ${e.message}"
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = userMessage
                    )
                } catch (e: Exception) {
                    val errorMsg = "Unexpected error: ${e.message ?: "Unknown error"}"
                    addLog(errorMsg, LogType.ERROR)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            } catch (e: Exception) {
                val errorMsg = "Failed to start clone: ${e.message ?: "Unknown error"}"
                addLog(errorMsg, LogType.ERROR)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    /**
     * 获取提交历史
     */
    suspend fun fetchCommitHistory() {
        addLog("Fetching commit history...", LogType.INFO)

        try {
            val commits = gitOperations.getRecentCommits(20)

            if (commits.isEmpty()) {
                addLog("No commits found in repository", LogType.WARNING)
            } else {
                addLog("Found ${commits.size} commits", LogType.SUCCESS)

                val displayCommits = commits.map { commit ->
                    GitCommitDisplay(
                        hash = commit.hash,
                        author = commit.author,
                        email = commit.email,
                        date = commit.date,
                        message = commit.message
                    )
                }

                _uiState.value = _uiState.value.copy(commits = displayCommits)
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to fetch commits: ${e.message ?: "Unknown error"}"
            addLog(errorMsg, LogType.ERROR)
        }
    }

    fun onCleared() {
        // Cleanup if needed
    }
}

