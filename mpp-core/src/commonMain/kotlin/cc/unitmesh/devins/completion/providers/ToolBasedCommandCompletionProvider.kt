package cc.unitmesh.devins.completion.providers

import cc.unitmesh.agent.tool.ToolNames
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.agent.tool.registry.GlobalToolRegistry
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.devins.completion.BaseCompletionProvider
import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionTriggerType
import cc.unitmesh.devins.completion.InsertResult

class ToolBasedCommandCompletionProvider(
    private val toolRegistry: ToolRegistry = GlobalToolRegistry.getInstance()
) : BaseCompletionProvider(setOf(CompletionTriggerType.COMMAND)) {

    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        val tools = toolRegistry.getAllTools()

        val completionItems = tools.values.map { tool ->
            CompletionItem(
                text = tool.name,
                displayText = tool.name,
                description = tool.description,
                icon = getToolIcon(tool.name),
                insertHandler = createCommandInsertHandler(tool.name)
            )
        }

        return filterAndSort(completionItems, query)
    }

    /**
     * 根据工具名称获取对应的图标 - 现在使用 ToolType 系统
     */
    private fun getToolIcon(toolName: String): String {
        val toolType = toolName.toToolType()
        return toolType?.tuiEmoji ?: when (toolName) {
            // Fallback for legacy tools
            ToolNames.READ_FILE -> "📄"
            ToolNames.WRITE_FILE -> "✏️"
            "grep" -> "🔍"
            "glob" -> "🌐"
            "shell" -> "💻"
            else -> "🔧"
        }
    }

    /**
     * 创建命令插入处理器
     * 根据命令类型决定插入空格还是冒号
     */
    private fun createCommandInsertHandler(commandName: String): (String, Int) -> InsertResult {
        return { fullText, cursorPos ->
            val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
            if (slashPos >= 0) {
                val before = fullText.substring(0, slashPos)
                val after = fullText.substring(cursorPos)

                // 根据命令类型决定后缀
                val suffix = when {
                    // 需要参数的命令使用冒号
                    commandName in listOf("read-file", "write-file", "file", "write", "read") -> ":"
                    // 其他命令使用空格
                    else -> " "
                }

                val newText = before + "/$commandName$suffix" + after
                InsertResult(
                    newText = newText,
                    newCursorPosition = before.length + commandName.length + 2,
                    shouldTriggerNextCompletion = suffix == ":" // 如果是冒号，触发下一级补全
                )
            } else {
                InsertResult(fullText, cursorPos)
            }
        }
    }
}