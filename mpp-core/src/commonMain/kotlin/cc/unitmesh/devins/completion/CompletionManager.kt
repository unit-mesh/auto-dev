package cc.unitmesh.devins.completion

import cc.unitmesh.devins.completion.providers.AgentCompletionProvider
import cc.unitmesh.devins.completion.providers.FilePathCompletionProvider
import cc.unitmesh.devins.completion.providers.SpecKitCommandCompletionProvider
import cc.unitmesh.devins.completion.providers.ToolBasedCommandCompletionProvider
import cc.unitmesh.devins.completion.providers.VariableCompletionProvider
import cc.unitmesh.devins.filesystem.ProjectFileSystem

/**
 * 补全管理器 - 根据上下文选择合适的 Provider
 * 现在通过 Workspace 系统进行管理，但保持向后兼容性
 * 支持智能搜索、过滤和实时补全
 */
class CompletionManager(fileSystem: ProjectFileSystem? = null) {
    private val specKitProvider = SpecKitCommandCompletionProvider(fileSystem)

    private val providers = mapOf(
        CompletionTriggerType.AGENT to AgentCompletionProvider(),
        CompletionTriggerType.COMMAND to ToolBasedCommandCompletionProvider(),
        CompletionTriggerType.VARIABLE to VariableCompletionProvider(),
        CompletionTriggerType.COMMAND_VALUE to FilePathCompletionProvider()
    )

    // 缓存最近的补全结果
    private var lastContext: CompletionContext? = null
    private var lastResults: List<CompletionItem> = emptyList()

    fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val provider = providers[context.triggerType] ?: return emptyList()
        val baseCompletions = provider.getCompletions(context)

        // 对于 COMMAND 类型，同时包含 SpecKit 命令
        val allCompletions = if (context.triggerType == CompletionTriggerType.COMMAND) {
            baseCompletions + specKitProvider.getCompletions(context)
        } else {
            baseCompletions
        }

        // 缓存结果
        lastContext = context
        lastResults = allCompletions

        return allCompletions
    }

    /**
     * 基于查询文本过滤补全项（用于边输入边补全）
     */
    fun getFilteredCompletions(context: CompletionContext): List<CompletionItem> {
        val allCompletions = getCompletions(context)
        val query = context.queryText

        if (query.isEmpty()) {
            return allCompletions
        }

        return allCompletions
            .filter { item -> matchesQuery(item, query) }
            .sortedWith(createQueryComparator(query))
            .take(50) // 限制结果数量
    }

    /**
     * 检查补全项是否匹配查询
     */
    private fun matchesQuery(item: CompletionItem, query: String): Boolean {
        val lowerQuery = query.lowercase()

        return item.text.lowercase().contains(lowerQuery) ||
               item.displayText.lowercase().contains(lowerQuery) ||
               item.description?.lowercase()?.contains(lowerQuery) == true
    }

    /**
     * 创建基于查询的比较器
     */
    private fun createQueryComparator(query: String): Comparator<CompletionItem> {
        val lowerQuery = query.lowercase()

        return compareBy<CompletionItem> { item ->
            // 1. 完全匹配优先
            when {
                item.text.equals(query, ignoreCase = true) -> 0
                item.displayText.equals(query, ignoreCase = true) -> 1
                else -> 2
            }
        }.thenBy { item ->
            // 2. 前缀匹配优先
            when {
                item.text.startsWith(query, ignoreCase = true) -> 0
                item.displayText.startsWith(query, ignoreCase = true) -> 1
                else -> 2
            }
        }.thenBy { item ->
            // 3. 匹配位置：越靠前越好
            minOf(
                item.text.lowercase().indexOf(lowerQuery).takeIf { it >= 0 } ?: Int.MAX_VALUE,
                item.displayText.lowercase().indexOf(lowerQuery).takeIf { it >= 0 } ?: Int.MAX_VALUE
            )
        }.thenBy { item ->
            // 4. 文本长度：越短越好
            item.text.length
        }.thenBy { item ->
            // 5. 字母顺序
            item.text.lowercase()
        }
    }

    /**
     * 刷新 SpecKit 命令（当项目路径改变时调用）
     */
    fun refreshSpecKitCommands() {
        specKitProvider.refresh()
        // 清除缓存
        lastContext = null
        lastResults = emptyList()
    }

    /**
     * 获取所有支持的触发类型
     */
    fun getSupportedTriggerTypes(): Set<CompletionTriggerType> {
        return providers.keys
    }

    /**
     * 检查是否支持指定的触发类型
     */
    fun supports(triggerType: CompletionTriggerType): Boolean {
        return triggerType in providers.keys
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        lastContext = null
        lastResults = emptyList()
    }

    /**
     * 获取补全统计信息
     */
    fun getCompletionStats(triggerType: CompletionTriggerType): CompletionStats? {
        val provider = providers[triggerType] ?: return null

        // 创建一个空的上下文来获取所有可用的补全项
        val emptyContext = CompletionContext(
            fullText = "",
            cursorPosition = 0,
            triggerType = triggerType,
            triggerOffset = 0,
            queryText = ""
        )

        val allCompletions = provider.getCompletions(emptyContext)

        return CompletionStats(
            triggerType = triggerType,
            totalItems = allCompletions.size,
            categories = categorizeCompletions(allCompletions)
        )
    }

    /**
     * 将补全项分类
     */
    private fun categorizeCompletions(completions: List<CompletionItem>): Map<String, Int> {
        return completions.groupBy { item ->
            when {
                item.description?.contains("Directory") == true -> "Directories"
                item.description?.contains("File") == true -> "Files"
                item.description?.contains("Command") == true -> "Commands"
                item.description?.contains("Agent") == true -> "Agents"
                item.description?.contains("Variable") == true -> "Variables"
                else -> "Other"
            }
        }.mapValues { it.value.size }
    }
}

/**
 * 补全统计信息
 */
data class CompletionStats(
    val triggerType: CompletionTriggerType,
    val totalItems: Int,
    val categories: Map<String, Int>
)