package cc.unitmesh.devins.ui.compose.editor.highlighting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import cc.unitmesh.devins.lexer.DevInsLexer
import cc.unitmesh.devins.token.DevInsTokenType
import cc.unitmesh.devins.ui.compose.editor.model.HighlightStyle

/**
 * DevIn 语法高亮器
 * 使用 mpp-core 的 DevInsLexer 进行词法分析
 */
class DevInSyntaxHighlighter {
    /**
     * 对文本进行语法高亮
     */
    fun highlight(text: String): AnnotatedString {
        if (text.isEmpty()) {
            return AnnotatedString(text)
        }

        return try {
            val lexer = DevInsLexer(text)
            val tokens = lexer.tokenize()

            buildAnnotatedString {
                append(text)

                tokens.forEach { token ->
                    val style = getStyleForTokenType(token.type)
                    if (style != null && token.startOffset < text.length) {
                        val endOffset = minOf(token.endOffset, text.length)
                        if (endOffset > token.startOffset) {
                            addStyle(
                                style.toSpanStyle(),
                                token.startOffset,
                                endOffset
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 如果解析失败，返回原始文本
            AnnotatedString(text)
        }
    }

    /**
     * 获取 Token 类型对应的样式
     * 采用高对比度、易区分的现代配色方案
     */
    private fun getStyleForTokenType(type: DevInsTokenType): HighlightStyle? {
        return when (type) {
            // Agent 相关 - 使用醒目的青色（类似 Slack 提及）
            DevInsTokenType.AGENT_START ->
                HighlightStyle(
                    color = Color(0xFF00D9FF), // 亮青色 #00D9FF
                    bold = true
                )

            // Command 相关 - 使用鲜艳的绿色（类似终端命令）
            DevInsTokenType.COMMAND_START ->
                HighlightStyle(
                    color = Color(0xFF00FF88), // 亮绿色 #00FF88
                    bold = true
                )
            DevInsTokenType.COMMAND_PROP ->
                HighlightStyle(
                    color = Color(0xFF88DD88) // 柔和绿色
                )

            // Variable 相关 - 使用洋红色/粉紫色（区别于其他）
            DevInsTokenType.VARIABLE_START ->
                HighlightStyle(
                    color = Color(0xFFFF6EC7), // 洋红色 #FF6EC7
                    bold = true
                )

            // 代码块
            DevInsTokenType.CODE_BLOCK_START,
            DevInsTokenType.CODE_BLOCK_END ->
                HighlightStyle(
                    color = Color(0xFFCC7832) // 橙色
                )
            DevInsTokenType.LANGUAGE_ID ->
                HighlightStyle(
                    color = Color(0xFF6897BB) // 蓝色
                )
            DevInsTokenType.CODE_CONTENT ->
                HighlightStyle(
                    color = Color(0xFFA9B7C6) // 浅灰
                )

            // 字符串
            DevInsTokenType.QUOTE_STRING ->
                HighlightStyle(
                    color = Color(0xFF6A8759) // 绿色
                )

            // 注释
            DevInsTokenType.COMMENTS,
            DevInsTokenType.CONTENT_COMMENTS,
            DevInsTokenType.BLOCK_COMMENT ->
                HighlightStyle(
                    color = Color(0xFF808080), // 灰色
                    italic = true
                )

            // 数字
            DevInsTokenType.NUMBER ->
                HighlightStyle(
                    color = Color(0xFF6897BB) // 蓝色
                )

            // 布尔值
            DevInsTokenType.BOOLEAN ->
                HighlightStyle(
                    color = Color(0xFFCC7832) // 橙色
                )

            // FrontMatter
            DevInsTokenType.FRONTMATTER_START,
            DevInsTokenType.FRONTMATTER_END ->
                HighlightStyle(
                    color = Color(0xFF9876AA), // 紫色
                    bold = true
                )

            // 关键字
            DevInsTokenType.WHEN,
            DevInsTokenType.ON_STREAMING,
            DevInsTokenType.BEFORE_STREAMING,
            DevInsTokenType.AFTER_STREAMING,
            DevInsTokenType.ON_STREAMING_END ->
                HighlightStyle(
                    color = Color(0xFFCC7832), // 橙色
                    bold = true
                )

            // 标识符
            DevInsTokenType.IDENTIFIER ->
                HighlightStyle(
                    color = Color(0xFFA9B7C6) // 浅灰
                )

            // 其他不着色
            else -> null
        }
    }

    companion object {
        /**
         * 现代化高对比度配色方案
         * 参考：GitHub、Slack、VSCode 等现代应用
         */
        object ModernColors {
            // 特殊符号 - 高对比度、易区分
            val AGENT = Color(0xFF00D9FF) // 青色 - Agent 提及（@）
            val COMMAND = Color(0xFF00FF88) // 绿色 - 命令（/）
            val VARIABLE = Color(0xFFFF6EC7) // 洋红 - 变量（$）

            // 代码元素
            val KEYWORD = Color(0xFFFF7F50) // 珊瑚橙 - 关键字
            val STRING = Color(0xFF98C379) // 柔和绿 - 字符串
            val NUMBER = Color(0xFFD19A66) // 橙黄色 - 数字
            val COMMENT = Color(0xFF5C6370) // 灰色 - 注释
            val IDENTIFIER = Color(0xFFABB2BF) // 浅灰 - 标识符
            val CONSTANT = Color(0xFFE5C07B) // 金黄色 - 常量
        }

        /**
         * 深色主题颜色方案 (保留向后兼容)
         */
        object DarculaColors {
            val KEYWORD = Color(0xFFCC7832)
            val STRING = Color(0xFF6A8759)
            val NUMBER = Color(0xFF6897BB)
            val COMMENT = Color(0xFF808080)
            val IDENTIFIER = Color(0xFFA9B7C6)
            val CONSTANT = Color(0xFF9876AA)
            val TEXT = Color(0xFFA9B7C6)
        }
    }
}
