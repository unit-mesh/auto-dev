package cc.unitmesh.devins.ui.compose.editor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue

// 导入 mpp-core 中的跨平台类型
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionTriggerType
import cc.unitmesh.devins.completion.InsertResult
import cc.unitmesh.devins.editor.EditorEvent
import cc.unitmesh.devins.editor.EditorCallbacks

// 为了向后兼容，重新导出这些类型
typealias CompletionItem = cc.unitmesh.devins.completion.CompletionItem
typealias CompletionContext = cc.unitmesh.devins.completion.CompletionContext
typealias CompletionTriggerType = cc.unitmesh.devins.completion.CompletionTriggerType
typealias InsertResult = cc.unitmesh.devins.completion.InsertResult
typealias EditorEvent = cc.unitmesh.devins.editor.EditorEvent
typealias EditorCallbacks = cc.unitmesh.devins.editor.EditorCallbacks

/**
 * 语法高亮样式
 * 这是 UI 层特定的类型，保留在此处
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

