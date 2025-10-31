package cc.unitmesh.devins.ui.compose.editor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle

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

