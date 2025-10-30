package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionProvider
import cc.unitmesh.devins.completion.InsertResult
import cc.unitmesh.devins.completion.defaultInsertHandler
import cc.unitmesh.devins.workspace.WorkspaceManager

/**
 * 文件路径补全提供者（用于 /file:, /write: 等命令之后）
 * 支持静态常用路径和全局文件搜索（文件级粒度，无需逐级选择目录）
 */
class FilePathCompletionProvider : CompletionProvider {

    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        val workspace = WorkspaceManager.getCurrentOrEmpty()

        // 合并不同类型的补全
        val completions = mutableListOf<CompletionItem>()

        // 1. 静态常用文件（总是显示，作为快捷选项）
        completions.addAll(getStaticCompletions(query))

        // 2. 全局文件搜索（递归搜索所有匹配的文件，包括深层目录）
        if (workspace.rootPath != null) {
            completions.addAll(searchFiles(query, workspace))
        }

        return completions
            .distinctBy { it.text }
            .filter { it.matchScore(query) > 0 }
            .sortedWith(createCompletionComparator(query))
            .take(50)
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
                insertHandler = defaultInsertHandler("README.md")
            ),
            CompletionItem(
                text = "build.gradle.kts",
                displayText = "build.gradle.kts",
                description = "File: build.gradle.kts",
                icon = "🔨",
                insertHandler = defaultInsertHandler("build.gradle.kts")
            ),
            CompletionItem(
                text = "build.gradle",
                displayText = "build.gradle",
                description = "File: build.gradle",
                icon = "🔨",
                insertHandler = defaultInsertHandler("build.gradle")
            ),
            CompletionItem(
                text = "settings.gradle.kts",
                displayText = "settings.gradle.kts",
                description = "File: settings.gradle.kts",
                icon = "🔨",
                insertHandler = defaultInsertHandler("settings.gradle.kts")
            ),
            CompletionItem(
                text = "settings.gradle",
                displayText = "settings.gradle",
                description = "File: settings.gradle",
                icon = "🔨",
                insertHandler = defaultInsertHandler("settings.gradle")
            ),
            CompletionItem(
                text = "gradle.properties",
                displayText = "gradle.properties",
                description = "File: gradle.properties",
                icon = "⚙️",
                insertHandler = defaultInsertHandler("gradle.properties")
            ),
            CompletionItem(
                text = "pom.xml",
                displayText = "pom.xml",
                description = "File: pom.xml",
                icon = "📋",
                insertHandler = defaultInsertHandler("pom.xml")
            ),
            CompletionItem(
                text = "package.json",
                displayText = "package.json",
                description = "File: package.json",
                icon = "📦",
                insertHandler = defaultInsertHandler("package.json")
            ),
            CompletionItem(
                text = ".gitignore",
                displayText = ".gitignore",
                description = "File: .gitignore",
                icon = "🚫",
                insertHandler = defaultInsertHandler(".gitignore")
            ),
            CompletionItem(
                text = "Dockerfile",
                displayText = "Dockerfile",
                description = "File: Dockerfile",
                icon = "🐳",
                insertHandler = defaultInsertHandler("Dockerfile")
            ),
            CompletionItem(
                text = ".dockerignore",
                displayText = ".dockerignore",
                description = "File: .dockerignore",
                icon = "🐳",
                insertHandler = defaultInsertHandler(".dockerignore")
            )
        )

        return commonFiles.filter { it.matchScore(query) > 0 }
    }

    /**
     * 全局文件搜索（递归搜索所有匹配的文件）
     */
    private fun searchFiles(query: String, workspace: cc.unitmesh.devins.workspace.Workspace): List<CompletionItem> {
        return try {
            val fileSystem = workspace.fileSystem
            
            // 根据查询长度调整搜索参数
            val (searchPattern, maxResults) = if (query.isEmpty()) {
                // 空查询：返回所有文件，但限制数量
                "*" to 30
            } else {
                // 有查询：搜索匹配的文件
                "*$query*" to 100
            }
            
            val filePaths = fileSystem.searchFiles(searchPattern, maxDepth = 10, maxResults = maxResults)
            
            filePaths.map { filePath ->
                createFileCompletionItem(filePath)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 创建文件补全项（只处理文件，不处理目录）
     */
    private fun createFileCompletionItem(filePath: String): CompletionItem {
        // 提取文件名用于显示
        val fileName = filePath.substringAfterLast("/")
        val directoryPath = filePath.substringBeforeLast("/", "")
        
        // 显示文本包含路径信息，方便识别
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
     * 创建补全项比较器，用于智能排序
     */
    private fun createCompletionComparator(query: String): Comparator<CompletionItem> {
        return compareBy<CompletionItem> { item ->
            // 1. 文件名匹配度：完全匹配 > 前缀匹配 > 包含匹配
            val fileName = item.text.substringAfterLast("/")
            when {
                fileName.equals(query, ignoreCase = true) -> 0
                fileName.startsWith(query, ignoreCase = true) -> 1
                fileName.contains(query, ignoreCase = true) -> 2
                item.text.contains(query, ignoreCase = true) -> 3
                else -> 4
            }
        }.thenBy { item ->
            // 2. 路径深度：浅的优先（文件在根目录附近的优先）
            item.text.count { it == '/' }
        }.thenBy { item ->
            // 3. 文件名长度：短的优先
            val fileName = item.text.substringAfterLast("/")
            fileName.length
        }.thenBy { item ->
            // 4. 字母顺序
            item.text.lowercase()
        }
    }

    /**
     * 根据文件扩展名获取图标
     */
    private fun getFileIcon(filePath: String): String {
        return when (filePath.substringAfterLast('.', "")) {
            "kt" -> "🟣"
            "java" -> "☕"
            "js", "ts" -> "🟨"
            "py" -> "🐍"
            "md" -> "📝"
            "json" -> "📋"
            "xml" -> "📄"
            "yml", "yaml" -> "⚙️"
            "properties" -> "⚙️"
            "gradle" -> "🔨"
            "txt" -> "📄"
            else -> "📄"
        }
    }
}