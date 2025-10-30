package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.*
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.runBlocking

/**
 * 文件路径补全提供者（用于 /file:, /write: 等命令之后）
 * 基于 gemini-cli 的 FileSearch 算法实现
 * 支持递归搜索、模糊匹配和智能排序
 */
class FilePathCompletionProvider : CompletionProvider {
    
    private var fileSearch: FileSearch? = null
    private var lastWorkspacePath: String? = null
    
    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        val workspace = WorkspaceManager.getCurrentOrEmpty()
        
        // 如果没有 workspace，返回静态补全
        val workspacePath = workspace.rootPath
        if (workspacePath == null) {
            return getStaticCompletions(query)
        }
        
        // 初始化或更新 FileSearch
        ensureFileSearch(workspacePath)
        
        // 合并静态补全和动态搜索结果
        val completions = mutableListOf<CompletionItem>()
        
        // 1. 静态常用文件（快捷访问）
        completions.addAll(getStaticCompletions(query))
        
        // 2. 动态文件搜索
        val searchResults = runBlocking {
            try {
                fileSearch?.search(query, maxResults = 50) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        completions.addAll(searchResults.map { filePath ->
            createFileCompletionItem(filePath)
        })
        
        return completions
            .distinctBy { it.text }
            .filter { it.matchScore(query) > 0 }
            .take(50)
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
            
            // 异步初始化
            runBlocking {
                try {
                    fileSearch?.initialize()
                } catch (e: Exception) {
                    // 初始化失败，使用降级方案
                }
            }
            
            lastWorkspacePath = workspacePath
        }
    }
    
    /**
     * 获取静态常用文件补全（只包含文件，不包含目录）
     */
    private fun getStaticCompletions(query: String): List<CompletionItem> {
        val commonFiles = listOf(
            // 项目配置文件
            CompletionItem(
                text = "README.md",
                displayText = "README.md",
                description = "File: README.md",
                icon = "📝",
                insertHandler = createFilePathInsertHandler("README.md")
            ),
            CompletionItem(
                text = "build.gradle.kts",
                displayText = "build.gradle.kts",
                description = "File: build.gradle.kts",
                icon = "🔨",
                insertHandler = createFilePathInsertHandler("build.gradle.kts")
            ),
            CompletionItem(
                text = "build.gradle",
                displayText = "build.gradle",
                description = "File: build.gradle",
                icon = "🔨",
                insertHandler = createFilePathInsertHandler("build.gradle")
            ),
            CompletionItem(
                text = "settings.gradle.kts",
                displayText = "settings.gradle.kts",
                description = "File: settings.gradle.kts",
                icon = "🔨",
                insertHandler = createFilePathInsertHandler("settings.gradle.kts")
            ),
            CompletionItem(
                text = "settings.gradle",
                displayText = "settings.gradle",
                description = "File: settings.gradle",
                icon = "🔨",
                insertHandler = createFilePathInsertHandler("settings.gradle")
            ),
            CompletionItem(
                text = "gradle.properties",
                displayText = "gradle.properties",
                description = "File: gradle.properties",
                icon = "⚙️",
                insertHandler = createFilePathInsertHandler("gradle.properties")
            ),
            CompletionItem(
                text = "pom.xml",
                displayText = "pom.xml",
                description = "File: pom.xml",
                icon = "📋",
                insertHandler = createFilePathInsertHandler("pom.xml")
            ),
            CompletionItem(
                text = "package.json",
                displayText = "package.json",
                description = "File: package.json",
                icon = "📦",
                insertHandler = createFilePathInsertHandler("package.json")
            ),
            CompletionItem(
                text = ".gitignore",
                displayText = ".gitignore",
                description = "File: .gitignore",
                icon = "🚫",
                insertHandler = createFilePathInsertHandler(".gitignore")
            ),
            CompletionItem(
                text = "Dockerfile",
                displayText = "Dockerfile",
                description = "File: Dockerfile",
                icon = "🐳",
                insertHandler = createFilePathInsertHandler("Dockerfile")
            ),
            CompletionItem(
                text = ".dockerignore",
                displayText = ".dockerignore",
                description = "File: .dockerignore",
                icon = "🐳",
                insertHandler = createFilePathInsertHandler(".dockerignore")
            )
        )
        
        return commonFiles.filter { it.matchScore(query) > 0 }
    }
    
    /**
     * 创建文件补全项
     */
    private fun createFileCompletionItem(filePath: String): CompletionItem {
        // 提取文件名和目录路径
        val fileName = filePath.substringAfterLast("/", filePath)
        val directoryPath = filePath.substringBeforeLast("/", "")
        
        // 显示文本：文件名 + 路径信息
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
