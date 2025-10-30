package cc.unitmesh.devins.completion

/**
 * 补全上下文
 * 
 * 包含了触发补全时的所有相关信息，用于补全提供者生成合适的补全项
 */
data class CompletionContext(
    /**
     * 完整的文本内容
     */
    val fullText: String,
    
    /**
     * 当前光标位置
     */
    val cursorPosition: Int,
    
    /**
     * 触发类型
     */
    val triggerType: CompletionTriggerType,
    
    /**
     * 触发字符的位置（例如 @ 或 / 的位置）
     */
    val triggerOffset: Int,
    
    /**
     * 查询文本（从触发字符到光标的文本）
     * 例如: 输入 "/file" 时，queryText 为 "file"
     */
    val queryText: String
) {
    /**
     * 获取触发字符之前的文本
     */
    val textBeforeTrigger: String
        get() = fullText.substring(0, triggerOffset)
    
    /**
     * 获取光标之后的文本
     */
    val textAfterCursor: String
        get() = if (cursorPosition < fullText.length) {
            fullText.substring(cursorPosition)
        } else {
            ""
        }
    
    /**
     * 检查查询是否为空
     */
    val isEmptyQuery: Boolean
        get() = queryText.isEmpty()
}

