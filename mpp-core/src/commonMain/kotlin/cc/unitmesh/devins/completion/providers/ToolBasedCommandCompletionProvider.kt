package cc.unitmesh.devins.completion.providers

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
                insertHandler = { fullText, cursorPos ->
                    val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
                    if (slashPos >= 0) {
                        val before = fullText.substring(0, slashPos)
                        val after = fullText.substring(cursorPos)
                        val newText = before + "/${tool.name} " + after
                        InsertResult(
                            newText = newText,
                            newCursorPosition = before.length + tool.name.length + 2,
                            shouldTriggerNextCompletion = false
                        )
                    } else {
                        InsertResult(fullText, cursorPos)
                    }
                }
            )
        }

        return filterAndSort(completionItems, query)
    }

    /**
     * 根据工具名称获取对应的图标
     */
    private fun getToolIcon(toolName: String): String {
        return when (toolName) {
            "read-file" -> "📄"
            "write-file" -> "✏️"
            "grep" -> "🔍"
            "glob" -> "🌐"
            "shell" -> "💻"
            else -> "🔧"
        }
    }
}