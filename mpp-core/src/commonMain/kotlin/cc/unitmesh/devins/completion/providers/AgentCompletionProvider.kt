package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionProvider
import cc.unitmesh.devins.completion.InsertResult
import cc.unitmesh.devins.completion.defaultInsertHandler

/**
 * Agent 补全提供者（@符号）
 */
class AgentCompletionProvider : CompletionProvider {
    private val agents = listOf(
        CompletionItem(
            text = "clarify",
            displayText = "clarify",
            description = "Clarify requirements and ask questions",
            icon = "❓",
            insertHandler = { fullText, cursorPos ->
                // 找到 @ 符号的位置
                val atPos = fullText.lastIndexOf('@', cursorPos - 1)
                if (atPos >= 0) {
                    val before = fullText.substring(0, atPos)
                    val after = fullText.substring(cursorPos)
                    val newText = before + "@clarify" + after
                    InsertResult(
                        newText,
                        before.length + 8
                    ) // "@clarify".length
                } else {
                    InsertResult(fullText, cursorPos)
                }
            }
        ),
        CompletionItem(
            text = "code-review",
            displayText = "code-review",
            description = "Review code and provide suggestions",
            icon = "🔍",
            insertHandler = defaultInsertHandler("@code-review")
        ),
        CompletionItem(
            text = "test-gen",
            displayText = "test-gen",
            description = "Generate unit tests",
            icon = "🧪",
            insertHandler = defaultInsertHandler("@test-gen")
        ),
        CompletionItem(
            text = "refactor",
            displayText = "refactor",
            description = "Suggest refactoring improvements",
            icon = "♻️",
            insertHandler = defaultInsertHandler("@refactor")
        )
    )

    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        return agents
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }
    }
}