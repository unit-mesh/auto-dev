package cc.unitmesh.devins.ui.compose.editor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue

/**
 * 补全项
 */
data class CompletionItem(
    val text: String,
    val displayText: String = text,
    val description: String? = null,
    val icon: String? = null, // 使用 emoji 作为图标
    val insertHandler: ((String, Int) -> InsertResult)? = null
) {
    /**
     * 计算匹配分数（用于排序）
     */
    fun matchScore(query: String): Int {
        if (text.equals(query, ignoreCase = true)) return 1000
        if (text.startsWith(query, ignoreCase = true)) return 500
        if (text.contains(query, ignoreCase = true)) return 100
        return fuzzyMatchScore(query)
    }
    
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
        return if (queryIndex == query.length) score else 0
    }
}

/**
 * 插入结果
 */
data class InsertResult(
    val newText: String,
    val newCursorPosition: Int,
    val shouldTriggerNextCompletion: Boolean = false
)

/**
 * 补全触发类型
 */
enum class CompletionTriggerType {
    NONE,
    AGENT,      // @
    COMMAND,    // /
    VARIABLE,   // $
    COMMAND_VALUE,  // :
    CODE_FENCE  // `
}

/**
 * 补全上下文
 */
data class CompletionContext(
    val fullText: String,
    val cursorPosition: Int,
    val triggerType: CompletionTriggerType,
    val triggerOffset: Int,  // 触发字符的位置
    val queryText: String    // 触发字符后到光标的文本
)

/**
 * 语法高亮样式
 */
data class HighlightStyle(
    val color: Color,
    val bold: Boolean = false,
    val italic: Boolean = false
) {
    fun toSpanStyle(): SpanStyle = SpanStyle(
        color = color,
        fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null,
        fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else null
    )
}

/**
 * 编辑器事件
 */
sealed class EditorEvent {
    data class Submit(val text: String) : EditorEvent()
    data class TextChanged(val text: String) : EditorEvent()
    data class CursorMoved(val position: Int) : EditorEvent()
}

/**
 * 编辑器回调
 */
interface EditorCallbacks {
    fun onSubmit(text: String) {}
    fun onTextChanged(text: String) {}
    fun onCursorMoved(position: Int) {}
}

