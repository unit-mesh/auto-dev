package cc.unitmesh.devins.ui.compose.editor.context

import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

private const val TAG = "FileSearch"

/**
 * Indexing state for file search
 */
enum class IndexingState {
    NOT_STARTED,
    INDEXING,
    READY,
    ERROR
}

/**
 * File search provider that uses WorkspaceManager's file system with pre-built index.
 * Builds an in-memory index of all files for fast searching.
 */
class WorkspaceFileSearchProvider(
    private val recentFilesProvider: RecentFilesProvider? = null
) : FileSearchProvider {

    private val _indexingState = MutableStateFlow(IndexingState.NOT_STARTED)
    val indexingState: StateFlow<IndexingState> = _indexingState

    private var fileIndex: List<IndexedFile> = emptyList()
    private var indexedWorkspacePath: String? = null

    data class IndexedFile(
        val name: String,
        val relativePath: String,
        val isDirectory: Boolean
    )

    /**
     * Build the file index for the current workspace.
     * Should be called when workspace is opened.
     */
    suspend fun buildIndex() = withContext(Dispatchers.Default) {
        val workspace = WorkspaceManager.currentWorkspace
        if (workspace == null) {
            AutoDevLogger.warn(TAG) { "buildIndex: No workspace available" }
            return@withContext
        }

        val rootPath = workspace.rootPath
        if (rootPath == null) {
            AutoDevLogger.warn(TAG) { "buildIndex: No root path available" }
            return@withContext
        }

        AutoDevLogger.info(TAG) { "buildIndex: workspace=$rootPath, currentState=${_indexingState.value}" }

        // Skip if already indexed for this workspace
        if (indexedWorkspacePath == rootPath && fileIndex.isNotEmpty()) {
            AutoDevLogger.info(TAG) { "buildIndex: Already indexed ${fileIndex.size} files" }
            _indexingState.value = IndexingState.READY
            return@withContext
        }

        _indexingState.value = IndexingState.INDEXING
        AutoDevLogger.info(TAG) { "buildIndex: Starting indexing..." }

        try {
            val files = mutableListOf<IndexedFile>()
            indexFilesRecursively(workspace, "", files, maxDepth = 6)
            fileIndex = files
            indexedWorkspacePath = rootPath
            _indexingState.value = IndexingState.READY
            AutoDevLogger.info(TAG) { "Index built: ${files.size} files" }
        } catch (e: Exception) {
            AutoDevLogger.error(TAG) { "Index error: ${e.message}" }
            e.printStackTrace()
            _indexingState.value = IndexingState.ERROR
        }
    }

    private fun indexFilesRecursively(
        workspace: Workspace,
        currentPath: String,
        files: MutableList<IndexedFile>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (currentDepth >= maxDepth || files.size >= 5000) return

        val fileSystem = workspace.fileSystem
        val pathToList = if (currentPath.isEmpty()) "." else currentPath

        try {
            val entries = fileSystem.listFiles(pathToList, null)
            if (currentDepth == 0) {
                AutoDevLogger.info(TAG) { "indexFilesRecursively: root entries=${entries.size}" }
            }
            for (entry in entries) {
                val name = entry.substringAfterLast('/')

                // Skip hidden files and common ignored directories
                if (name.startsWith(".") || name in IGNORED_DIRS) continue

                val isDir = fileSystem.isDirectory(fileSystem.resolvePath(entry))
                files.add(IndexedFile(name, entry, isDir))

                if (isDir && files.size < 5000) {
                    indexFilesRecursively(workspace, entry, files, maxDepth, currentDepth + 1)
                }
            }
        } catch (e: Exception) {
            AutoDevLogger.error(TAG) { "indexFilesRecursively error at '$pathToList': ${e.message}" }
        }
    }

    override suspend fun searchFiles(query: String): List<SelectedFileItem> = withContext(Dispatchers.Default) {
        val workspace = WorkspaceManager.currentWorkspace
        if (workspace == null) {
            AutoDevLogger.warn(TAG) { "searchFiles: No workspace available" }
            return@withContext emptyList()
        }
        val fileSystem = workspace.fileSystem

        // Build index if not ready
        if (_indexingState.value != IndexingState.READY) {
            AutoDevLogger.info(TAG) { "searchFiles: Index not ready, building..." }
            buildIndex()
        }

        val lowerQuery = query.lowercase()
        AutoDevLogger.info(TAG) { "searchFiles: query='$query', indexSize=${fileIndex.size}" }

        val results = fileIndex
            .filter { it.name.lowercase().contains(lowerQuery) }
            .take(50)
            .map { indexed ->
                SelectedFileItem(
                    name = indexed.name,
                    path = fileSystem.resolvePath(indexed.relativePath),
                    relativePath = indexed.relativePath,
                    isDirectory = indexed.isDirectory
                )
            }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        AutoDevLogger.info(TAG) { "searchFiles: found ${results.size} results" }
        results
    }

    override suspend fun getRecentFiles(): List<SelectedFileItem> = withContext(Dispatchers.Default) {
        recentFilesProvider?.getRecentFiles() ?: getDefaultRecentFiles()
    }

    private fun getDefaultRecentFiles(): List<SelectedFileItem> {
        val workspace = WorkspaceManager.currentWorkspace ?: return emptyList()
        val fileSystem = workspace.fileSystem

        return try {
            val rootFiles = fileSystem.listFiles(".", null).take(10)
            rootFiles.mapNotNull { path ->
                val name = path.substringAfterLast('/')
                if (name.startsWith(".")) return@mapNotNull null
                val isDir = fileSystem.isDirectory(fileSystem.resolvePath(path))
                SelectedFileItem(name = name, path = fileSystem.resolvePath(path), relativePath = path, isDirectory = isDir)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private val IGNORED_DIRS = setOf(
            "node_modules", "build", "dist", "target", "out",
            "__pycache__", ".gradle", ".idea", ".vscode"
        )
    }
}

/** Interface for platform-specific recent files tracking */
interface RecentFilesProvider {
    suspend fun getRecentFiles(): List<SelectedFileItem>
    fun addRecentFile(file: SelectedFileItem)
}

/** In-memory recent files provider */
class InMemoryRecentFilesProvider(private val maxSize: Int = 20) : RecentFilesProvider {
    private val recentFiles = mutableListOf<SelectedFileItem>()

    override suspend fun getRecentFiles(): List<SelectedFileItem> = recentFiles.toList()

    override fun addRecentFile(file: SelectedFileItem) {
        recentFiles.removeAll { it.path == file.path }
        recentFiles.add(0, file)
        if (recentFiles.size > maxSize) recentFiles.removeAt(recentFiles.lastIndex)
    }
}

