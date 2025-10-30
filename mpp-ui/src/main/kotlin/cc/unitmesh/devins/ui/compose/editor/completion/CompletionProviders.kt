package cc.unitmesh.devins.ui.compose.editor.completion

import cc.unitmesh.devins.ui.compose.editor.model.CompletionContext
import cc.unitmesh.devins.ui.compose.editor.model.CompletionItem
import cc.unitmesh.devins.ui.compose.editor.model.InsertResult

/**
 * 补全提供者接口
 */
interface CompletionProvider {
    fun getCompletions(context: CompletionContext): List<CompletionItem>
}

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
                    InsertResult(newText, before.length + 8) // "@clarify".length
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

/**
 * Command 补全提供者（/符号）
 */
class CommandCompletionProvider : CompletionProvider {
    private val commands = listOf(
        CompletionItem(
            text = "file",
            displayText = "file",
            description = "Read file content: /file:path/to/file",
            icon = "📄",
            insertHandler = { fullText, cursorPos ->
                val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
                if (slashPos >= 0) {
                    val before = fullText.substring(0, slashPos)
                    val after = fullText.substring(cursorPos)
                    val newText = before + "/file:" + after
                    InsertResult(newText, before.length + 6, shouldTriggerNextCompletion = true)
                } else {
                    InsertResult(fullText, cursorPos)
                }
            }
        ),
        CompletionItem(
            text = "symbol",
            displayText = "symbol",
            description = "Find code symbol: /symbol:ClassName.methodName",
            icon = "🔤",
            insertHandler = { fullText, cursorPos ->
                val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
                if (slashPos >= 0) {
                    val before = fullText.substring(0, slashPos)
                    val after = fullText.substring(cursorPos)
                    val newText = before + "/symbol:" + after
                    InsertResult(newText, before.length + 8, shouldTriggerNextCompletion = true)
                } else {
                    InsertResult(fullText, cursorPos)
                }
            }
        ),
        CompletionItem(
            text = "write",
            displayText = "write",
            description = "Write to file: /write:path/to/file",
            icon = "✍️",
            insertHandler = { fullText, cursorPos ->
                val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
                if (slashPos >= 0) {
                    val before = fullText.substring(0, slashPos)
                    val after = fullText.substring(cursorPos)
                    val newText = before + "/write:" + after
                    InsertResult(newText, before.length + 7, shouldTriggerNextCompletion = true)
                } else {
                    InsertResult(fullText, cursorPos)
                }
            }
        ),
        CompletionItem(
            text = "run",
            displayText = "run",
            description = "Execute command: /run:command args",
            icon = "▶️",
            insertHandler = { fullText, cursorPos ->
                val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
                if (slashPos >= 0) {
                    val before = fullText.substring(0, slashPos)
                    val after = fullText.substring(cursorPos)
                    val newText = before + "/run:" + after
                    InsertResult(newText, before.length + 5, shouldTriggerNextCompletion = true)
                } else {
                    InsertResult(fullText, cursorPos)
                }
            }
        ),
        CompletionItem(
            text = "refactor",
            displayText = "refactor",
            description = "Refactor code: /refactor:function_name",
            icon = "🔧",
            insertHandler = defaultInsertHandler("/refactor:")
        )
    )
    
    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        return commands
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }
    }
}

/**
 * Variable 补全提供者（$符号）
 */
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
        
        // 简单的正则匹配 FrontMatter 中的变量定义
        val frontMatterRegex = """---\s*\n(.*?)\n---""".toRegex(RegexOption.DOT_MATCHES_ALL)
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

/**
 * 文件路径补全提供者（用于 /file:, /write: 等命令之后）
 */
class FilePathCompletionProvider : CompletionProvider {
    private val commonPaths = listOf(
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
        )
    )
    
    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        return commonPaths
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }
    }
}

/**
 * 默认的插入处理器
 */
private fun defaultInsertHandler(insertText: String): (String, Int) -> InsertResult {
    return { fullText, cursorPos ->
        // 找到触发字符的位置
        val triggerPos = when {
            insertText.startsWith("@") -> fullText.lastIndexOf('@', cursorPos - 1)
            insertText.startsWith("/") -> fullText.lastIndexOf('/', cursorPos - 1)
            insertText.startsWith("$") -> fullText.lastIndexOf('$', cursorPos - 1)
            else -> -1
        }
        
        if (triggerPos >= 0) {
            val before = fullText.substring(0, triggerPos)
            val after = fullText.substring(cursorPos)
            val newText = before + insertText + after
            InsertResult(newText, before.length + insertText.length)
        } else {
            InsertResult(fullText, cursorPos)
        }
    }
}

/**
 * 补全管理器 - 根据上下文选择合适的 Provider
 */
class CompletionManager {
    private val providers = mapOf(
        cc.unitmesh.devins.ui.compose.editor.model.CompletionTriggerType.AGENT to AgentCompletionProvider(),
        cc.unitmesh.devins.ui.compose.editor.model.CompletionTriggerType.COMMAND to CommandCompletionProvider(),
        cc.unitmesh.devins.ui.compose.editor.model.CompletionTriggerType.VARIABLE to VariableCompletionProvider(),
        cc.unitmesh.devins.ui.compose.editor.model.CompletionTriggerType.COMMAND_VALUE to FilePathCompletionProvider()
    )
    
    fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val provider = providers[context.triggerType] ?: return emptyList()
        return provider.getCompletions(context)
    }
}

