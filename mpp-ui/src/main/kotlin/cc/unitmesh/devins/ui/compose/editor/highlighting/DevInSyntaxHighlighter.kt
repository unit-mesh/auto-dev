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
     */
    private fun getStyleForTokenType(type: DevInsTokenType): HighlightStyle? {
        return when (type) {
            // Agent 相关
            DevInsTokenType.AGENT_START -> HighlightStyle(
                color = Color(0xFF9876AA), // 紫色
                bold = true
            )
            
            // Command 相关
            DevInsTokenType.COMMAND_START -> HighlightStyle(
                color = Color(0xFF9876AA), // 紫色
                bold = true
            )
            DevInsTokenType.COMMAND_PROP -> HighlightStyle(
                color = Color(0xFF6A8759) // 绿色
            )
            
            // Variable 相关
            DevInsTokenType.VARIABLE_START -> HighlightStyle(
                color = Color(0xFF9876AA), // 紫色
                bold = true
            )
            
            // 代码块
            DevInsTokenType.CODE_BLOCK_START, 
            DevInsTokenType.CODE_BLOCK_END -> HighlightStyle(
                color = Color(0xFFCC7832) // 橙色
            )
            DevInsTokenType.LANGUAGE_ID -> HighlightStyle(
                color = Color(0xFF6897BB) // 蓝色
            )
            DevInsTokenType.CODE_CONTENT -> HighlightStyle(
                color = Color(0xFFA9B7C6) // 浅灰
            )
            
            // 字符串
            DevInsTokenType.QUOTE_STRING -> HighlightStyle(
                color = Color(0xFF6A8759) // 绿色
            )
            
            // 注释
            DevInsTokenType.COMMENTS,
            DevInsTokenType.CONTENT_COMMENTS,
            DevInsTokenType.BLOCK_COMMENT -> HighlightStyle(
                color = Color(0xFF808080), // 灰色
                italic = true
            )
            
            // 数字
            DevInsTokenType.NUMBER -> HighlightStyle(
                color = Color(0xFF6897BB) // 蓝色
            )
            
            // 布尔值
            DevInsTokenType.BOOLEAN -> HighlightStyle(
                color = Color(0xFFCC7832) // 橙色
            )
            
            // FrontMatter
            DevInsTokenType.FRONTMATTER_START,
            DevInsTokenType.FRONTMATTER_END -> HighlightStyle(
                color = Color(0xFF9876AA), // 紫色
                bold = true
            )
            
            // 关键字
            DevInsTokenType.WHEN,
            DevInsTokenType.ON_STREAMING,
            DevInsTokenType.BEFORE_STREAMING,
            DevInsTokenType.AFTER_STREAMING,
            DevInsTokenType.ON_STREAMING_END -> HighlightStyle(
                color = Color(0xFFCC7832), // 橙色
                bold = true
            )
            
            // 标识符
            DevInsTokenType.IDENTIFIER -> HighlightStyle(
                color = Color(0xFFA9B7C6) // 浅灰
            )
            
            // 其他不着色
            else -> null
        }
    }
    
    companion object {
        /**
         * 深色主题颜色方案 (类似 IntelliJ Darcula)
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
        
        /**
         * 浅色主题颜色方案
         */
        object LightColors {
            val KEYWORD = Color(0xFF0000FF)
            val STRING = Color(0xFF008000)
            val NUMBER = Color(0xFF1750EB)
            val COMMENT = Color(0xFF808080)
            val IDENTIFIER = Color(0xFF000000)
            val CONSTANT = Color(0xFF871094)
            val TEXT = Color(0xFF000000)
        }
    }
}

