package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.BaseCompletionProvider
import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionTriggerType
import cc.unitmesh.devins.completion.InsertResult

/**
 * Builtin command completion provider
 * Provides system commands like /clear, /exit, /help, /config, etc.
 */
class BuiltinCommandCompletionProvider : BaseCompletionProvider(setOf(CompletionTriggerType.COMMAND)) {

    private val builtinCommands = listOf(
        BuiltinCommand(
            name = "help",
            description = "Show available commands and help information",
            icon = "📖"
        ),
        BuiltinCommand(
            name = "clear",
            description = "Clear the screen and conversation history",
            icon = "🧹"
        ),
        BuiltinCommand(
            name = "exit",
            description = "Exit the application",
            icon = "🚪"
        ),
        BuiltinCommand(
            name = "config",
            description = "Show current configuration settings",
            icon = "⚙️"
        ),
        BuiltinCommand(
            name = "model",
            description = "Change or view the current LLM model",
            icon = "🤖"
        )
    )

    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText

        val completionItems = builtinCommands.map { cmd ->
            CompletionItem(
                text = cmd.name,
                displayText = cmd.name,
                description = cmd.description,
                icon = cmd.icon,
                insertHandler = createBuiltinCommandInsertHandler(cmd.name)
            )
        }

        return filterAndSort(completionItems, query)
    }

    /**
     * Create insert handler for builtin commands
     * Most builtin commands don't need parameters, so just add a space
     */
    private fun createBuiltinCommandInsertHandler(commandName: String): (String, Int) -> InsertResult {
        return { fullText, cursorPos ->
            val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
            if (slashPos >= 0) {
                val before = fullText.substring(0, slashPos)
                val after = fullText.substring(cursorPos)

                // Builtin commands typically don't need parameters, use space
                val suffix = " "

                val newText = before + "/$commandName$suffix" + after
                InsertResult(
                    newText = newText,
                    newCursorPosition = before.length + commandName.length + 2,
                    shouldTriggerNextCompletion = false
                )
            } else {
                InsertResult(fullText, cursorPos)
            }
        }
    }

    private data class BuiltinCommand(
        val name: String,
        val description: String,
        val icon: String
    )
}
