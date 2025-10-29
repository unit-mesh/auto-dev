package cc.unitmesh.devins.token

/**
 * DevIns 语言的 Token 类型定义
 * 基于原始的 DevInTypes.java 和 DevInLexer.flex 实现
 */
enum class DevInsTokenType(val displayName: String) {
    // 基础 Token
    EOF("EOF"),
    NEWLINE("NEWLINE"),
    TEXT_SEGMENT("TEXT_SEGMENT"),
    
    // 标识符和字面量
    IDENTIFIER("IDENTIFIER"),
    NUMBER("NUMBER"),
    BOOLEAN("BOOLEAN"),
    DATE("DATE"),
    QUOTE_STRING("QUOTE_STRING"),
    SINGLE_QUOTED_STRING("SINGLE_QUOTED_STRING"),
    DOUBLE_QUOTED_STRING("DOUBLE_QUOTED_STRING"),
    
    // 注释
    COMMENTS("COMMENTS"),
    BLOCK_COMMENT("BLOCK_COMMENT"),
    CONTENT_COMMENTS("CONTENT_COMMENTS"),
    
    // 代码块
    CODE_BLOCK_START("CODE_BLOCK_START"),
    CODE_BLOCK_END("CODE_BLOCK_END"),
    CODE_CONTENT("CODE_CONTENT"),
    LANGUAGE_ID("LANGUAGE_ID"),
    
    // 前置元数据
    FRONTMATTER_START("FRONTMATTER_START"),
    FRONTMATTER_END("FRONTMATTER_END"),
    
    // 操作符
    COLON("COLON"),
    SHARP("#"),
    DOT("."),
    COMMA(","),
    PIPE("|"),
    ARROW("=>"),
    ACCESS("::"),
    PROCESS("->"),
    
    // 括号
    LPAREN("("),
    RPAREN(")"),
    LBRACKET("["),
    RBRACKET("]"),
    OPEN_BRACE("{"),
    CLOSE_BRACE("}"),
    
    // 比较操作符
    EQEQ("=="),
    NEQ("!="),
    LT("<"),
    GT(">"),
    LTE("<="),
    GTE(">="),
    
    // 逻辑操作符
    NOT("!"),
    ANDAND("&&"),
    OROR("||"),
    AND("and"),
    
    // 特殊标记
    AGENT_START("@"),
    COMMAND_START("/"),
    VARIABLE_START("$"),
    
    // 命令相关
    COMMAND_PROP("COMMAND_PROP"),
    LINE_INFO("LINE_INFO"),
    
    // 关键字
    CASE("case"),
    DEFAULT("default"),
    IF("if"),
    ELSE("else"),
    ELSEIF("elseif"),
    END("end"),
    ENDIF("endif"),
    FROM("from"),
    WHERE("where"),
    SELECT("select"),
    CONDITION("condition"),
    FUNCTIONS("functions"),
    
    // 生命周期关键字
    WHEN("when"),
    ON_STREAMING("onStreaming"),
    BEFORE_STREAMING("beforeStreaming"),
    ON_STREAMING_END("onStreamingEnd"),
    AFTER_STREAMING("afterStreaming"),
    
    // 模式表达式
    PATTERN_EXPR("PATTERN_EXPR"),
    
    // 缩进
    INDENT("INDENT"),
    
    // 错误 Token
    BAD_CHARACTER("BAD_CHARACTER");
    
    companion object {
        /**
         * 关键字集合
         */
        val KEYWORDS = setOf(
            CASE, DEFAULT, IF, ELSE, ELSEIF, END, ENDIF,
            FROM, WHERE, SELECT, CONDITION, FUNCTIONS,
            WHEN, ON_STREAMING, BEFORE_STREAMING, ON_STREAMING_END, AFTER_STREAMING,
            AND, BOOLEAN
        )
        
        /**
         * 操作符集合
         */
        val OPERATORS = setOf(
            COLON, SHARP, DOT, COMMA, PIPE, ARROW, ACCESS, PROCESS,
            EQEQ, NEQ, LT, GT, LTE, GTE, NOT, ANDAND, OROR
        )
        
        /**
         * 括号集合
         */
        val BRACKETS = setOf(
            LPAREN, RPAREN, LBRACKET, RBRACKET, OPEN_BRACE, CLOSE_BRACE
        )
        
        /**
         * 注释集合
         */
        val COMMENTS_SET = setOf(
            COMMENTS, BLOCK_COMMENT, CONTENT_COMMENTS
        )
        
        /**
         * 字面量集合
         */
        val LITERALS = setOf(
            NUMBER, BOOLEAN, DATE, QUOTE_STRING, SINGLE_QUOTED_STRING, DOUBLE_QUOTED_STRING
        )
        
        /**
         * 根据字符串获取关键字类型
         */
        fun getKeywordType(text: String): DevInsTokenType? {
            return when (text) {
                "case" -> CASE
                "default" -> DEFAULT
                "if" -> IF
                "else" -> ELSE
                "elseif" -> ELSEIF
                "end" -> END
                "endif" -> ENDIF
                "from" -> FROM
                "where" -> WHERE
                "select" -> SELECT
                "condition" -> CONDITION
                "functions" -> FUNCTIONS
                "when" -> WHEN
                "onStreaming" -> ON_STREAMING
                "beforeStreaming" -> BEFORE_STREAMING
                "onStreamingEnd" -> ON_STREAMING_END
                "afterStreaming" -> AFTER_STREAMING
                "and" -> AND
                "true", "false", "TRUE", "FALSE" -> BOOLEAN
                else -> null
            }
        }
        
        /**
         * 检查是否为关键字
         */
        fun isKeyword(text: String): Boolean {
            return getKeywordType(text) != null
        }
    }
}
