package cc.unitmesh.devins.completion

/**
 * 补全提供者接口
 *
 * 定义了补全提供者的基本契约，不同类型的补全（Agent、Command、Variable 等）
 * 都应该实现这个接口
 */
interface CompletionProvider {
    /**
     * 获取补全项列表
     *
     * @param context 补全上下文，包含触发类型、查询文本等信息
     * @return 补全项列表，已按匹配度排序
     */
    fun getCompletions(context: CompletionContext): List<CompletionItem>

    /**
     * 检查该提供者是否支持指定的触发类型
     *
     * @param triggerType 触发类型
     * @return 如果支持返回 true，否则返回 false
     */
    fun supports(triggerType: CompletionTriggerType): Boolean = true
}

/**
 * 抽象补全提供者基类
 *
 * 提供了一些通用的辅助方法，子类可以直接使用
 */
abstract class BaseCompletionProvider(
    private val supportedTriggerTypes: Set<CompletionTriggerType>
) : CompletionProvider {

    override fun supports(triggerType: CompletionTriggerType): Boolean {
        return triggerType in supportedTriggerTypes
    }

    /**
     * 过滤并排序补全项
     */
    protected fun filterAndSort(
        items: List<CompletionItem>,
        query: String
    ): List<CompletionItem> {
        return items
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }
    }

    /**
     * 创建一个简单的补全项
     */
    protected fun createCompletionItem(
        text: String,
        description: String? = null,
        icon: String? = null
    ): CompletionItem {
        return CompletionItem(
            text = text,
            displayText = text,
            description = description,
            icon = icon
        )
    }
}


/**
 * 默认的插入处理器
 */
fun defaultInsertHandler(insertText: String): (String, Int) -> InsertResult {
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