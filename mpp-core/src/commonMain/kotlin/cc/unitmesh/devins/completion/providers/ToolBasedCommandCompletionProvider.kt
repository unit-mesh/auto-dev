package cc.unitmesh.devins.completion.providers

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

    private fun getToolIcon(toolName: String): String = toolName.toToolType()?.tuiEmoji ?: "🔧"

    private fun createCommandInsertHandler(commandName: String): (String, Int) -> InsertResult {
        return { fullText, cursorPos ->
            val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
            if (slashPos >= 0) {
                val before = fullText.take(slashPos)
                val after = fullText.substring(cursorPos)

                val suffix = when {
                    commandName in listOf(
                        ToolType.ReadFile.name,
                        ToolType.WriteFile.name,
                        ToolType.Glob.name,
                        ToolType.Grep.name
                    ) -> ":"

                    else -> " "
                }

                val newText = before + "/$commandName$suffix" + after
                InsertResult(
                    newText = newText,
                    newCursorPosition = before.length + commandName.length + 2,
                    shouldTriggerNextCompletion = suffix == ":"
                )
            } else {
                InsertResult(fullText, cursorPos)
            }
        }
    }
}