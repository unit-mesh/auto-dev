package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionProvider
import cc.unitmesh.devins.completion.defaultInsertHandler
import cc.unitmesh.devins.workspace.WorkspaceManager

/**
 * 文件路径补全提供者（用于 /file:, /write: 等命令之后）
 * 支持动态文件系统补全和边输入边搜索
 */
class FilePathCompletionProvider : CompletionProvider {

    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        val workspace = WorkspaceManager.getCurrentOrEmpty()

        // 合并静态路径和动态文件系统补全
        val staticCompletions = getStaticCompletions(query)
        val dynamicCompletions = getDynamicCompletions(query, workspace)

        val allCompletions = (staticCompletions + dynamicCompletions)
            .distinctBy { it.text }
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }

        return allCompletions.take(20) // 限制结果数量
    }

    /**
     * 获取静态的常用路径补全
     */
    private fun getStaticCompletions(query: String): List<CompletionItem> {
        val commonPaths = listOf(
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
            )
        )

        return commonPaths
    }

    /**
     * 获取基于文件系统的动态补全
     */
    private fun getDynamicCompletions(query: String, workspace: cc.unitmesh.devins.workspace.Workspace): List<CompletionItem> {
        if (workspace.rootPath == null || query.length < 2) {
            return emptyList()
        }

        return try {
            val fileSystem = workspace.fileSystem
            val searchPath = if (query.contains("/")) {
                query.substringBeforeLast("/")
            } else {
                ""
            }

            val searchPattern = if (query.contains("/")) {
                "*${query.substringAfterLast("/")}*"
            } else {
                "*$query*"
            }

            // 搜索文件和目录
            val files = fileSystem.listFiles(searchPath, searchPattern)

            files.map { filePath ->
                val isDirectory = fileSystem.exists("$filePath/") // 简单的目录检测
                CompletionItem(
                    text = filePath,
                    displayText = filePath,
                    description = if (isDirectory) "Directory" else "File",
                    icon = if (isDirectory) "📁" else getFileIcon(filePath),
                    insertHandler = defaultInsertHandler(filePath)
                )
            }
        } catch (e: Exception) {
            emptyList()
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