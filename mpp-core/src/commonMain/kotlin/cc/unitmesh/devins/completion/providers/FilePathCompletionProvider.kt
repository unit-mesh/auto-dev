package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionProvider
import cc.unitmesh.devins.completion.defaultInsertHandler
import cc.unitmesh.devins.workspace.WorkspaceManager

/**
 * 文件路径补全提供者（用于 /file:, /write: 等命令之后）
 * 支持静态常用路径、动态文件系统补全和智能搜索
 */
class FilePathCompletionProvider : CompletionProvider {

    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        val workspace = WorkspaceManager.getCurrentOrEmpty()

        // 合并不同类型的补全
        val completions = mutableListOf<CompletionItem>()

        // 1. 静态常用路径
        completions.addAll(getStaticCompletions(query))

        // 2. 动态文件补全
        if (workspace.rootPath != null) {
            completions.addAll(getDynamicCompletions(query, workspace))
        }

        return completions
            .distinctBy { it.text }
            .filter { it.matchScore(query) > 0 }
            .sortedWith(createCompletionComparator(query))
            .take(50) // 增加结果数量限制
    }

    /**
     * 获取静态常用路径补全
     */
    private fun getStaticCompletions(query: String): List<CompletionItem> {
        val commonPaths = listOf(
            // 源码目录
            CompletionItem(
                text = "src/main/kotlin/",
                displayText = "src/main/kotlin/",
                description = "Kotlin source directory",
                icon = "📁",
                insertHandler = defaultInsertHandler("src/main/kotlin/")
            ),
            CompletionItem(
                text = "src/main/java/",
                displayText = "src/main/java/",
                description = "Java source directory",
                icon = "📁",
                insertHandler = defaultInsertHandler("src/main/java/")
            ),
            CompletionItem(
                text = "src/test/kotlin/",
                displayText = "src/test/kotlin/",
                description = "Kotlin test directory",
                icon = "📁",
                insertHandler = defaultInsertHandler("src/test/kotlin/")
            ),
            CompletionItem(
                text = "src/test/java/",
                displayText = "src/test/java/",
                description = "Java test directory",
                icon = "📁",
                insertHandler = defaultInsertHandler("src/test/java/")
            ),

            // 资源目录
            CompletionItem(
                text = "src/main/resources/",
                displayText = "src/main/resources/",
                description = "Main resources directory",
                icon = "📁",
                insertHandler = defaultInsertHandler("src/main/resources/")
            ),
            CompletionItem(
                text = "src/test/resources/",
                displayText = "src/test/resources/",
                description = "Test resources directory",
                icon = "📁",
                insertHandler = defaultInsertHandler("src/test/resources/")
            ),

            // 配置文件
            CompletionItem(
                text = "README.md",
                displayText = "README.md",
                description = "Project README",
                icon = "📝",
                insertHandler = defaultInsertHandler("README.md")
            ),
            CompletionItem(
                text = "build.gradle.kts",
                displayText = "build.gradle.kts",
                description = "Gradle build file",
                icon = "🔨",
                insertHandler = defaultInsertHandler("build.gradle.kts")
            ),
            CompletionItem(
                text = "settings.gradle.kts",
                displayText = "settings.gradle.kts",
                description = "Gradle settings file",
                icon = "🔨",
                insertHandler = defaultInsertHandler("settings.gradle.kts")
            ),
            CompletionItem(
                text = "gradle.properties",
                displayText = "gradle.properties",
                description = "Gradle properties file",
                icon = "⚙️",
                insertHandler = defaultInsertHandler("gradle.properties")
            ),

            // 其他常用文件
            CompletionItem(
                text = ".gitignore",
                displayText = ".gitignore",
                description = "Git ignore file",
                icon = "🚫",
                insertHandler = defaultInsertHandler(".gitignore")
            ),
            CompletionItem(
                text = "package.json",
                displayText = "package.json",
                description = "NPM package file",
                icon = "📦",
                insertHandler = defaultInsertHandler("package.json")
            )
        )

        return commonPaths.filter { it.matchScore(query) > 0 }
    }

    private fun getDynamicCompletions(query: String, workspace: cc.unitmesh.devins.workspace.Workspace): List<CompletionItem> {
        return try {
            val fileSystem = workspace.fileSystem

            // 如果查询为空或很短，只显示根目录内容
            if (query.isEmpty()) {
                return getRootDirectoryCompletions(fileSystem)
            }

            // 合并目录浏览和文件搜索结果
            val completions = mutableListOf<CompletionItem>()

            // 1. 目录浏览补全
            completions.addAll(getDirectoryCompletions(query, fileSystem))

            // 2. 文件搜索补全（当查询长度 >= 2 时）
            if (query.length >= 2) {
                completions.addAll(getSearchCompletions(query, fileSystem))
            }

            completions
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取根目录内容
     */
    private fun getRootDirectoryCompletions(fileSystem: cc.unitmesh.devins.filesystem.ProjectFileSystem): List<CompletionItem> {
        return try {
            val files = fileSystem.listFiles("", null)
            files.take(20).map { filePath ->
                createCompletionItem(filePath, fileSystem)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取目录浏览补全
     */
    private fun getDirectoryCompletions(query: String, fileSystem: cc.unitmesh.devins.filesystem.ProjectFileSystem): List<CompletionItem> {
        return try {
            // 确定要浏览的目录
            val targetDir = if (query.contains("/")) {
                query.substringBeforeLast("/")
            } else {
                "" // 根目录
            }

            val nameFilter = if (query.contains("/")) {
                query.substringAfterLast("/")
            } else {
                query
            }

            // 列出目录内容
            val files = fileSystem.listFiles(targetDir, "*$nameFilter*")

            files.map { filePath ->
                createCompletionItem(filePath, fileSystem)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取文件搜索补全
     */
    private fun getSearchCompletions(query: String, fileSystem: cc.unitmesh.devins.filesystem.ProjectFileSystem): List<CompletionItem> {
        return try {
            // 在整个项目中搜索匹配的文件
            val searchPattern = "*$query*"
            val files = fileSystem.listFiles("", searchPattern)

            files.map { filePath ->
                createCompletionItem(filePath, fileSystem)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 创建补全项
     */
    private fun createCompletionItem(filePath: String, fileSystem: cc.unitmesh.devins.filesystem.ProjectFileSystem): CompletionItem {
        // 简单的目录检测：检查路径是否以 / 结尾或者通过文件系统检测
        val isDirectory = filePath.endsWith("/") ||
                         (!filePath.contains(".") && fileSystem.exists("$filePath/"))
        val displayPath = if (isDirectory && !filePath.endsWith("/")) "$filePath/" else filePath

        return CompletionItem(
            text = displayPath,
            displayText = displayPath,
            description = if (isDirectory) "Directory" else "File",
            icon = if (isDirectory) "📁" else getFileIcon(filePath),
            insertHandler = defaultInsertHandler(displayPath)
        )
    }

    /**
     * 创建补全项比较器，用于智能排序
     */
    private fun createCompletionComparator(query: String): Comparator<CompletionItem> {
        return compareBy<CompletionItem> { item ->
            // 1. 优先级：目录 > 文件
            if (item.description?.contains("Directory") == true) 0 else 1
        }.thenBy { item ->
            // 2. 匹配度：完全匹配 > 前缀匹配 > 包含匹配
            when {
                item.text.equals(query, ignoreCase = true) -> 0
                item.text.startsWith(query, ignoreCase = true) -> 1
                item.text.contains(query, ignoreCase = true) -> 2
                else -> 3
            }
        }.thenBy { item ->
            // 3. 文件名长度：短的优先
            item.text.length
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