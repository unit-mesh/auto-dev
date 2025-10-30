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

        val allCompletions = getDynamicCompletions(query, workspace)
            .distinctBy { it.text }
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }

        return allCompletions.take(20) // 限制结果数量
    }

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