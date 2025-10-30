package cc.unitmesh.devins.completion

/**
 * 补全项
 * 
 * 表示一个可供选择的补全选项，包含文本、描述和匹配逻辑
 */
data class CompletionItem(
    /**
     * 实际插入的文本
     */
    val text: String,
    
    /**
     * 显示给用户的文本（默认与 text 相同）
     */
    val displayText: String = text,
    
    /**
     * 补全项的描述/说明
     */
    val description: String? = null,
    
    /**
     * 图标（可以是 emoji 或其他标识）
     */
    val icon: String? = null,
    
    /**
     * 插入处理器
     * 接收当前完整文本和光标位置，返回插入结果
     * 如果为 null，使用默认的插入逻辑
     */
    val insertHandler: ((fullText: String, cursorPosition: Int) -> InsertResult)? = null
) {
    /**
     * 计算与查询字符串的匹配分数（用于排序）
     * 分数越高，匹配度越好
     */
    fun matchScore(query: String): Int {
        // 完全匹配（忽略大小写）
        if (text.equals(query, ignoreCase = true)) return 1000
        
        // 前缀匹配
        if (text.startsWith(query, ignoreCase = true)) return 500
        
        // 包含匹配
        if (text.contains(query, ignoreCase = true)) return 100
        
        // 模糊匹配
        return fuzzyMatchScore(query)
    }
    
    /**
     * 模糊匹配分数
     * 检查查询字符串的字符是否按顺序出现在补全项中
     */
    private fun fuzzyMatchScore(query: String): Int {
        var score = 0
        var queryIndex = 0
        
        for (i in text.indices) {
            if (queryIndex < query.length &&
                text[i].equals(query[queryIndex], ignoreCase = true)) {
                score += 10
                queryIndex++
            }
        }
        
        // 只有所有查询字符都匹配时才返回分数
        return if (queryIndex == query.length) score else 0
    }
    
    /**
     * 执行默认的插入逻辑
     */
    fun defaultInsert(fullText: String, cursorPosition: Int): InsertResult {
        // 如果有自定义处理器，使用它
        insertHandler?.let { handler ->
            return handler(fullText, cursorPosition)
        }
        
        // 默认逻辑: 找到触发字符的位置，替换从触发字符到光标的文本
        val before = fullText.substring(0, cursorPosition)
        val after = fullText.substring(cursorPosition)
        
        // 查找最后一个触发字符的位置
        val triggerPos = maxOf(
            before.lastIndexOf('@'),
            before.lastIndexOf('/'),
            before.lastIndexOf('$'),
            before.lastIndexOf(':'),
            before.lastIndexOf('`')
        )
        
        return if (triggerPos >= 0) {
            val newText = fullText.substring(0, triggerPos + 1) + text + " " + after
            InsertResult(
                newText = newText,
                newCursorPosition = triggerPos + 1 + text.length + 1,
                shouldTriggerNextCompletion = false
            )
        } else {
            // 没有找到触发字符，直接在光标位置插入
            val newText = before + text + " " + after
            InsertResult(
                newText = newText,
                newCursorPosition = cursorPosition + text.length + 1,
                shouldTriggerNextCompletion = false
            )
        }
    }
}

