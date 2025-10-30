package cc.unitmesh.devins.completion

import cc.unitmesh.agent.tool.registry.GlobalToolRegistry
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.devins.command.SpecKitCommand
import cc.unitmesh.devins.filesystem.ProjectFileSystem

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
                            InsertResult(newText, before.length + cmd.fullCommandName.length + 2)
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

/**
 * 基于 Tool 系统的命令补全提供者
 *
 * 从 ToolRegistry 中获取所有可用的工具，并为每个工具生成补全项
 */
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