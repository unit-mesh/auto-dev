package cc.unitmesh.devins.token

/**
 * DevIns Token 数据类
 * 表示词法分析过程中产生的一个 Token
 */
data class DevInsToken(
    /**
     * Token 类型
     */
    val type: DevInsTokenType,
    
    /**
     * Token 的文本内容
     */
    val text: String,
    
    /**
     * Token 在源代码中的起始位置
     */
    val startOffset: Int,
    
    /**
     * Token 在源代码中的结束位置
     */
    val endOffset: Int,
    
    /**
     * Token 所在的行号（从 1 开始）
     */
    val line: Int,
    
    /**
     * Token 在行中的列号（从 1 开始）
     */
    val column: Int
) {
    /**
     * Token 的长度
     */
    val length: Int get() = endOffset - startOffset
    
    /**
     * 检查是否为关键字
     */
    val isKeyword: Boolean get() = type in DevInsTokenType.KEYWORDS
    
    /**
     * 检查是否为操作符
     */
    val isOperator: Boolean get() = type in DevInsTokenType.OPERATORS
    
    /**
     * 检查是否为括号
     */
    val isBracket: Boolean get() = type in DevInsTokenType.BRACKETS
    
    /**
     * 检查是否为注释
     */
    val isComment: Boolean get() = type in DevInsTokenType.COMMENTS_SET
    
    /**
     * 检查是否为字面量
     */
    val isLiteral: Boolean get() = type in DevInsTokenType.LITERALS
    
    /**
     * 检查是否为标识符
     */
    val isIdentifier: Boolean get() = type == DevInsTokenType.IDENTIFIER
    
    /**
     * 检查是否为 EOF
     */
    val isEof: Boolean get() = type == DevInsTokenType.EOF
    
    /**
     * 检查是否为换行符
     */
    val isNewline: Boolean get() = type == DevInsTokenType.NEWLINE
    
    /**
     * 检查是否为空白字符（换行符或缩进）
     */
    val isWhitespace: Boolean get() = type == DevInsTokenType.NEWLINE || type == DevInsTokenType.INDENT
    
    override fun toString(): String {
        return "DevInsToken(type=$type, text='$text', pos=$startOffset-$endOffset, line=$line, col=$column)"
    }
    
    companion object {
        /**
         * 创建 EOF Token
         */
        fun eof(offset: Int, line: Int, column: Int): DevInsToken {
            return DevInsToken(
                type = DevInsTokenType.EOF,
                text = "",
                startOffset = offset,
                endOffset = offset,
                line = line,
                column = column
            )
        }
        
        /**
         * 创建错误 Token
         */
        fun badCharacter(char: Char, offset: Int, line: Int, column: Int): DevInsToken {
            return DevInsToken(
                type = DevInsTokenType.BAD_CHARACTER,
                text = char.toString(),
                startOffset = offset,
                endOffset = offset + 1,
                line = line,
                column = column
            )
        }
        
        /**
         * 创建简单 Token（用于测试）
         */
        fun simple(type: DevInsTokenType, text: String): DevInsToken {
            return DevInsToken(
                type = type,
                text = text,
                startOffset = 0,
                endOffset = text.length,
                line = 1,
                column = 1
            )
        }
    }
}

/**
 * Token 位置信息
 */
data class TokenPosition(
    val offset: Int,
    val line: Int,
    val column: Int
) {
    override fun toString(): String {
        return "($line:$column@$offset)"
    }
}
