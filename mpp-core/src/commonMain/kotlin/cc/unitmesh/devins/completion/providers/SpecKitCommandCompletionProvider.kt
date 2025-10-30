package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.command.SpecKitCommand
import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionProvider
import cc.unitmesh.devins.completion.InsertResult
import cc.unitmesh.devins.filesystem.ProjectFileSystem

/**
 * SpecKit 命令补全提供者
 * 从项目文件系统动态加载 SpecKit 命令
 */
class SpecKitCommandCompletionProvider(
    private val fileSystem: ProjectFileSystem?
) : CompletionProvider {
    private var cachedCommands: List<CompletionItem>? = null

    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText

        // 延迟加载 SpecKit 命令
        if (cachedCommands == null && fileSystem != null) {
            cachedCommands = loadSpecKitCommands()
        }

        val commands = cachedCommands ?: emptyList()

        return commands
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }
    }

    private fun loadSpecKitCommands(): List<CompletionItem> {
        if (fileSystem == null) return emptyList()

        return try {
            val commands = SpecKitCommand.Companion.loadAll(fileSystem)
            commands.map { cmd ->
                CompletionItem(
                    text = cmd.fullCommandName,
                    displayText = cmd.fullCommandName,
                    description = cmd.description,
                    icon = "✨",
                    insertHandler = { fullText, cursorPos ->
                        val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
                        if (slashPos >= 0) {
                            val before = fullText.substring(0, slashPos)
                            val after = fullText.substring(cursorPos)
                            val newText = before + "/${cmd.fullCommandName} " + after
                            InsertResult(
                                newText,
                                before.length + cmd.fullCommandName.length + 2
                            )
                        } else {
                            InsertResult(fullText, cursorPos)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 刷新命令缓存（当项目路径改变时）
     */
    fun refresh() {
        cachedCommands = null
    }
}