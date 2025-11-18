package cc.unitmesh.devins.ui.wasm

import cc.unitmesh.agent.Platform
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
            return "${dateTime.year}-${dateTime.monthNumber.toString().padStart(2, '0')}-${dateTime.dayOfMonth.toString().padStart(2, '0')} " +
                    "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
        }
}

/**
 * UI 状态
 */
data class WasmGitUiState(
    val repoUrl: String = "https://github.com/unit-mesh/untitled",
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
 */
class WasmGitViewModel {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _uiState = MutableStateFlow(WasmGitUiState())
    val uiState: StateFlow<WasmGitUiState> = _uiState.asStateFlow()

    private var gitOps: GitOperations? = null

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
                val operations = GitOperations(projectPath = "/workspace")

                // 检查是否支持
                if (!operations.isSupported()) {
                    addLog("Error: Git operations not supported on this platform", LogType.ERROR)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Git operations not supported on this platform"
                    )
                    return@launch
                }

                gitOps = operations
                addLog("Git operations ready", LogType.DEBUG)

                // 执行克隆
                val targetDir = currentState.targetDir.ifBlank {
                    null
                }

                addLog("Cloning repository: ${currentState.repoUrl}", LogType.INFO)

                val success = gitOps!!.performClone(currentState.repoUrl, targetDir)
                    if (success) {
                        addLog("Clone completed successfully!", LogType.SUCCESS)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            cloneSuccess = true
                        )

                        // 自动获取提交历史
                        fetchCommitHistory()
                    } else {
                        addLog("Clone failed. Check the logs for details.", LogType.ERROR)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to clone repository"
                        )
                    }
            } catch (e: Exception) {
                val errorMsg = "Exception: ${e.message ?: "Unknown error"}"
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
        val ops = gitOps
        if (ops == null) {
            addLog("No repository cloned yet", LogType.WARNING)
            return
        }

        addLog("Fetching commit history...", LogType.INFO)

        scope.launch {
            try {
                val commits = ops.getRecentCommits(20)

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
    }

    fun onCleared() {
        // Cleanup if needed
    }
}

