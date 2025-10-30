package cc.unitmesh.devins.completion

/**
 * 插入操作的结果
 * 
 * 描述了补全项被插入后的文本状态和光标位置
 */
data class InsertResult(
    /**
     * 插入后的新文本
     */
    val newText: String,
    
    /**
     * 插入后的新光标位置
     */
    val newCursorPosition: Int,
    
    /**
     * 是否应该触发下一个补全
     * 例如: 插入 "/file:" 后，应该立即触发路径补全
     */
    val shouldTriggerNextCompletion: Boolean = false
) {
    companion object {
        /**
         * 创建一个无变化的结果
         */
        fun noChange(text: String, cursorPosition: Int): InsertResult {
            return InsertResult(text, cursorPosition, false)
        }
    }
}

