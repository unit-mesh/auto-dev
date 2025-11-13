package cc.unitmesh.devins.workspace

import cc.unitmesh.devins.completion.CompletionManager
import cc.unitmesh.devins.filesystem.DefaultFileSystem
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * 工作空间接口
 * 类似于 IntelliJ IDEA 的 Project 概念，管理项目的所有资源和服务
 */
interface Workspace {
    /**
     * 工作空间名称
     */
    val name: String

    /**
     * 工作空间根路径
     */
    val rootPath: String?

    /**
     * 文件系统服务
     */
    val fileSystem: ProjectFileSystem

    /**
     * 补全管理器
     */
    val completionManager: CompletionManager

    /**
     * 工作空间状态流
     */
    val stateFlow: StateFlow<WorkspaceState>

    /**
     * 检查工作空间是否已初始化
     */
    fun isInitialized(): Boolean

    /**
     * 刷新工作空间（重新加载配置、缓存等）
     */
    suspend fun refresh()

    /**
     * 关闭工作空间
     */
    suspend fun close()

    /**
     * 获取最后一次 Git 提交信息（预留接口）
     * 实际实现需要在平台特定代码中完成
     */
    suspend fun getLastCommit(): GitCommitInfo?

    /**
     * 获取 Git Diff（预留接口）
     * @param base 基准分支或提交，null 表示 HEAD
     * @param target 目标分支或提交，null 表示工作区
     */
    suspend fun getGitDiff(base: String? = null, target: String? = null): GitDiffInfo?
}

/**
 * Git 提交信息
 */
data class GitCommitInfo(
    val hash: String,
    val author: String,
    val email: String,
    val date: Long,
    val message: String,
    val shortHash: String = hash.take(7)
)

/**
 * Git Diff 信息
 */
data class GitDiffInfo(
    val files: List<GitDiffFile>,
    val totalAdditions: Int,
    val totalDeletions: Int
)

/**
 * Git Diff 文件信息
 */
data class GitDiffFile(
    val path: String,
    val oldPath: String? = null,
    val status: GitFileStatus,
    val additions: Int,
    val deletions: Int,
    val diff: String
)

/**
 * Git 文件状态
 */
enum class GitFileStatus {
    ADDED,
    DELETED,
    MODIFIED,
    RENAMED,
    COPIED
}

/**
 * 工作空间状态
 */
data class WorkspaceState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshTime: Long = 0L
)

/**
 * 默认工作空间实现
 */
class DefaultWorkspace private constructor(
    override val name: String,
    override val rootPath: String?
) : Workspace {

    private val _stateFlow = MutableStateFlow(WorkspaceState())
    override val stateFlow: StateFlow<WorkspaceState> = _stateFlow.asStateFlow()

    override val fileSystem: ProjectFileSystem by lazy {
        rootPath?.let { DefaultFileSystem(it) } ?: EmptyFileSystem()
    }

    override val completionManager: CompletionManager by lazy {
        CompletionManager(fileSystem)
    }

    init {
        _stateFlow.value = WorkspaceState(
            isInitialized = rootPath != null,
            lastRefreshTime = Clock.System.now().toEpochMilliseconds()
        )
    }

    override fun isInitialized(): Boolean {
        return _stateFlow.value.isInitialized
    }

    override suspend fun refresh() {
        _stateFlow.value = _stateFlow.value.copy(isLoading = true)

        try {
            // 刷新补全管理器
            completionManager.refreshSpecKitCommands()

            _stateFlow.value = _stateFlow.value.copy(
                isLoading = false,
                error = null,
                lastRefreshTime = Clock.System.now().toEpochMilliseconds()
            )
        } catch (e: Exception) {
            _stateFlow.value = _stateFlow.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    override suspend fun close() {
        _stateFlow.value = WorkspaceState(isInitialized = false)
    }

    override suspend fun getLastCommit(): GitCommitInfo? {
        // TODO: 实现 Git 提交信息获取
        // 需要在各个平台（JVM/JS/Native）中实现具体逻辑
        return null
    }

    override suspend fun getGitDiff(base: String?, target: String?): GitDiffInfo? {
        // TODO: 实现 Git Diff 获取
        // 需要在各个平台（JVM/JS/Native）中实现具体逻辑
        return null
    }

    companion object {
        /**
         * 创建工作空间实例
         */
        fun create(name: String, rootPath: String?): Workspace {
            return DefaultWorkspace(name, rootPath)
        }

        /**
         * 创建空工作空间
         */
        fun createEmpty(name: String = "Empty Workspace"): Workspace {
            return DefaultWorkspace(name, null)
        }
    }
}

/**
 * 工作空间管理器 - 全局单例
 * 管理当前活动的工作空间
 */
object WorkspaceManager {
    private var _currentWorkspace: Workspace? = null
    private val _workspaceFlow = MutableStateFlow<Workspace?>(null)

    /**
     * 当前工作空间状态流
     */
    val workspaceFlow: StateFlow<Workspace?> = _workspaceFlow.asStateFlow()

    /**
     * 当前工作空间
     */
    val currentWorkspace: Workspace?
        get() = _currentWorkspace

    /**
     * 获取当前工作空间，如果没有则创建空工作空间
     */
    fun getCurrentOrEmpty(): Workspace {
        return _currentWorkspace ?: DefaultWorkspace.createEmpty()
    }

    /**
     * 打开工作空间
     */
    suspend fun openWorkspace(name: String, rootPath: String): Workspace {
        // 关闭当前工作空间
        _currentWorkspace?.close()

        // 创建新工作空间
        val workspace = DefaultWorkspace.create(name, rootPath)
        _currentWorkspace = workspace
        _workspaceFlow.value = workspace

        // 刷新工作空间
        workspace.refresh()

        return workspace
    }

    /**
     * 打开空工作空间
     */
    suspend fun openEmptyWorkspace(name: String = "Empty Workspace"): Workspace {
        _currentWorkspace?.close()

        val workspace = DefaultWorkspace.createEmpty(name)
        _currentWorkspace = workspace
        _workspaceFlow.value = workspace

        return workspace
    }

    /**
     * 关闭当前工作空间
     */
    suspend fun closeCurrentWorkspace() {
        _currentWorkspace?.close()
        _currentWorkspace = null
        _workspaceFlow.value = null
    }

    /**
     * 刷新当前工作空间
     */
    suspend fun refreshCurrentWorkspace() {
        _currentWorkspace?.refresh()
    }

    /**
     * 检查是否有活动的工作空间
     */
    fun hasActiveWorkspace(): Boolean {
        return _currentWorkspace?.isInitialized() == true
    }
}

/**
 * 工作空间扩展函数
 */

/**
 * 获取工作空间的显示名称
 */
fun Workspace.getDisplayName(): String {
    val path = rootPath
    return if (path != null) {
        "$name (${path.substringAfterLast('/')})"
    } else {
        name
    }
}

/**
 * 检查工作空间是否为空
 */
fun Workspace.isEmpty(): Boolean {
    return rootPath == null
}

/**
 * 获取工作空间的相对路径
 */
fun Workspace.getRelativePath(absolutePath: String): String {
    val path = rootPath
    return if (path != null && absolutePath.startsWith(path)) {
        absolutePath.substring(path.length).removePrefix("/")
    } else {
        absolutePath
    }
}
