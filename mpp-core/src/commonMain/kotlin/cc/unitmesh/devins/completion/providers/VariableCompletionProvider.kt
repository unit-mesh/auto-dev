package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionProvider
import cc.unitmesh.devins.completion.defaultInsertHandler

class VariableCompletionProvider : CompletionProvider {
    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        // 从 FrontMatter 中提取变量
        val variables = extractVariablesFromText(context.fullText)

        val query = context.queryText
        return variables
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }
    }

    private fun extractVariablesFromText(text: String): List<CompletionItem> {
        val variables = mutableSetOf<String>()

        val frontMatterRegex = """---\s*\n(.*?)\n---""".toRegex(RegexOption.MULTILINE)
        val match = frontMatterRegex.find(text)
        if (match != null) {
            val frontMatter = match.groupValues[1]
            val varRegex = """(\w+):""".toRegex()
            varRegex.findAll(frontMatter).forEach { varMatch ->
                variables.add(varMatch.groupValues[1])
            }
        }

        // 添加一些常用的预定义变量
        variables.addAll(listOf("input", "output", "context", "selection", "clipboard"))

        return variables.map { varName ->
            CompletionItem(
                text = varName,
                displayText = varName,
                description = "Variable: \$$varName",
                icon = "💡",
                insertHandler = defaultInsertHandler("\$$varName")
            )
        }
    }
}