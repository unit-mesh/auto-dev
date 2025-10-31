package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.*
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 文件路径补全提供者（用于 /file:, /write: 等命令之后）
 * 基于 gemini-cli 的 FileSearch 算法实现
 * 支持递归搜索、模糊匹配和智能排序
 */
class FilePathCompletionProvider : CompletionProvider {
    
    private var fileSearch: FileSearch? = null
    private var lastWorkspacePath: String? = null
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cachedResults: MutableMap<String, List<String>> = mutableMapOf()
    private val pendingJobs: MutableMap<String, Job> = mutableMapOf()
    private var initJob: Job? = null
    
    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        val workspace = WorkspaceManager.getCurrentOrEmpty()
        
        // 如果没有 workspace，返回静态补全
        val workspacePath = workspace.rootPath ?: return emptyList()

        // 初始化或更新 FileSearch
        ensureFileSearch(workspacePath)
        
        // 合并静态补全和动态搜索结果
        val completions = mutableListOf<CompletionItem>()
        
        // 1. 静态常用文件（快捷访问）
        completions.addAll(getStaticCompletions(query))
        
        // 2. 动态文件搜索（异步触发 + 使用缓存返回）
        // 如果查询为空，返回根目录下的常见文件
        if (query.isBlank()) {
            completions.addAll(getCommonRootFiles(workspacePath))
        } else {
            val searchResults = cachedResults[query].orEmpty()
            triggerBackgroundSearch(query)
            
            completions.addAll(searchResults.map { filePath ->
                createFileCompletionItem(filePath)
            })
        }
        
        return completions
            .distinctBy { it.text }
            .filter { it.matchScore(query) > 0 }
            .take(50)
    }
    
    /**
     * 获取工作区根目录下的常见文件
     */
    private fun getCommonRootFiles(workspacePath: String): List<CompletionItem> {
        val fileSystem = WorkspaceManager.getCurrentOrEmpty().fileSystem
        val commonFiles = mutableListOf<CompletionItem>()
        
        try {
            // 获取根目录下的文件
            val rootFiles = fileSystem.listFiles(workspacePath)
                .filter { !it.startsWith(".") } // 过滤隐藏文件
                .sortedBy { it.lowercase() }
                .take(20) // 只返回前20个
            
            rootFiles.forEach { fileName ->
                val filePath = "$workspacePath/$fileName"
                if (!fileSystem.isDirectory(filePath)) {
                    commonFiles.add(createFileCompletionItem(fileName))
                }
            }
        } catch (e: Exception) {
            // 如果读取失败，返回空列表
        }
        
        return commonFiles
    }
    
    /**
     * 确保 FileSearch 已初始化
     */
    private fun ensureFileSearch(workspacePath: String) {
        if (fileSearch == null || lastWorkspacePath != workspacePath) {
            val options = FileSearchOptions(
                projectRoot = workspacePath,
                enableRecursiveSearch = true,
                enableFuzzyMatch = true,
                maxDepth = 10,
                cache = true
            )
            
            fileSearch = FileSearchFactory.create(
                fileSystem = WorkspaceManager.getCurrentOrEmpty().fileSystem,
                options = options
            )
            
            // 异步初始化（跨平台安全，不阻塞）
            initJob?.cancel()
            initJob = scope.launch {
                try {
                    fileSearch?.initialize()
                } catch (_: Exception) {
                    // 初始化失败，忽略，保持降级为静态补全
                }
            }
            
            lastWorkspacePath = workspacePath
        }
    }

    /**
     * 触发后台搜索并更新缓存（避免并发重复查询）
     */
    private fun triggerBackgroundSearch(query: String) {
        if (query.isBlank()) return
        val search = fileSearch ?: return
        if (pendingJobs[query]?.isActive == true) return
        pendingJobs[query] = scope.launch {
            try {
                // 等待初始化尽量完成（若已完成则立即继续）
                initJob?.join()
            } catch (_: Exception) {
                // ignore
            }
            try {
                val results = search.search(query, maxResults = 50)
                cachedResults[query] = results
            } catch (_: Exception) {
                // ignore errors during async search
            } finally {
                pendingJobs.remove(query)
            }
        }
    }

    private fun getStaticCompletions(query: String): List<CompletionItem> {
        val workspace = WorkspaceManager.getCurrentOrEmpty()
        val rootPath = workspace.rootPath ?: return emptyList()
        
        // If query is empty, return common files from the workspace root
//        if (query.isEmpty()) {
//            val commonFiles = listOf(
//                "README.md",
//                "package.json",
//                "build.gradle.kts",
//                "settings.gradle.kts",
//                "pom.xml",
//                ".gitignore",
//                "tsconfig.json",
//                "Cargo.toml",
//                "go.mod",
//                "Makefile"
//            )
//
//            return commonFiles.mapNotNull { fileName ->
//                val fileSystem = workspace.fileSystem
//                if (fileSystem.exists("$rootPath/$fileName")) {
//                    createFileCompletionItem(fileName)
//                } else {
//                    null
//                }
//            }
//        }

        return emptyList()
    }

    private fun createFileCompletionItem(filePath: String): CompletionItem {
        val fileName = filePath.substringAfterLast("/", filePath)
        val directoryPath = filePath.substringBeforeLast("/", "")
        
        val displayText = if (directoryPath.isNotEmpty()) {
            "$fileName • $directoryPath"
        } else {
            fileName
        }
        
        return CompletionItem(
            text = filePath,
            displayText = displayText,
            description = "File: $filePath",
            icon = getFileIcon(filePath),
            insertHandler = createFilePathInsertHandler(filePath)
        )
    }
    
    /**
     * 创建文件路径插入处理器
     */
    private fun createFilePathInsertHandler(filePath: String): (String, Int) -> InsertResult {
        return { fullText, cursorPos ->
            // 找到触发字符的位置（通常是冒号）
            val colonPos = fullText.lastIndexOf(':', cursorPos - 1)
            if (colonPos >= 0) {
                val before = fullText.substring(0, colonPos + 1)
                val after = fullText.substring(cursorPos)
                val newText = before + filePath + after
                InsertResult(
                    newText = newText,
                    newCursorPosition = before.length + filePath.length,
                    shouldTriggerNextCompletion = false
                )
            } else {
                InsertResult(fullText, cursorPos)
            }
        }
    }
    
    /**
     * 根据文件扩展名获取图标
     */
    private fun getFileIcon(filePath: String): String {
        return when (filePath.substringAfterLast('.', "")) {
            "kt", "kts" -> "🟣"
            "java" -> "☕"
            "js", "jsx", "ts", "tsx" -> "🟨"
            "py" -> "🐍"
            "rs" -> "🦀"
            "go" -> "🐹"
            "md" -> "📝"
            "json" -> "📋"
            "xml" -> "📄"
            "yml", "yaml" -> "⚙️"
            "properties" -> "⚙️"
            "gradle" -> "🔨"
            "txt" -> "📄"
            "html", "htm" -> "🌐"
            "css", "scss", "sass" -> "🎨"
            "sql" -> "🗄️"
            "sh", "bash" -> "🔧"
            else -> "📄"
        }
    }
}
