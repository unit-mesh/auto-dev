package cc.unitmesh.devins.lexer

import cc.unitmesh.devins.token.DevInsToken
import cc.unitmesh.devins.token.DevInsTokenType

/**
 * DevIns 词法分析器
 * 基于原始的 DevInLexer.flex 实现，转换为纯 Kotlin 实现
 */
class DevInsLexer(
    private val input: String
) {
    private var position = 0
    private var line = 1
    private var column = 1
    private val context = LexerContext()
    
    /**
     * 获取下一个 Token
     */
    fun nextToken(): DevInsToken {
        if (position >= input.length) {
            return DevInsToken.eof(position, line, column)
        }

        return when (context.currentState) {
            LexerState.INITIAL -> tokenizeInitial()
            LexerState.FRONT_MATTER_BLOCK -> tokenizeFrontMatter()
            LexerState.FRONT_MATTER_VALUE_BLOCK -> tokenizeFrontMatterValue()
            LexerState.FRONT_MATTER_VAL_OBJECT -> tokenizeFrontMatterObject()
            LexerState.PATTERN_ACTION_BLOCK -> tokenizePatternAction()
            LexerState.CONTENT_COMMENT_BLOCK -> tokenizeContentComment()
            LexerState.FUNCTION_DECL_BLOCK -> tokenizeFunctionDecl()
            LexerState.USED -> tokenizeUsed()
            LexerState.COMMAND_BLOCK -> tokenizeCommand()
            LexerState.SINGLE_COMMENT_BLOCK -> tokenizeSingleComment()
            LexerState.COMMAND_VALUE_BLOCK -> tokenizeCommandValue()
            LexerState.LINE_BLOCK -> tokenizeLine()
            LexerState.AGENT_BLOCK -> tokenizeAgent()
            LexerState.VARIABLE_BLOCK -> tokenizeVariable()
            LexerState.EXPR_BLOCK -> tokenizeExpression()
            LexerState.CODE_BLOCK -> tokenizeCodeBlock()
            LexerState.LANG_ID -> tokenizeLanguageId()
            else -> tokenizeDefault()
        }
    }
    
    /**
     * 获取所有 Token
     */
    fun tokenize(): List<DevInsToken> {
        val tokens = mutableListOf<DevInsToken>()
        var token = nextToken()
        
        while (!token.isEof) {
            tokens.add(token)
            token = nextToken()
        }
        
        tokens.add(token) // 添加 EOF token
        return tokens
    }
    
    private fun tokenizeInitial(): DevInsToken {
        val startPos = position
        val startLine = line
        val startColumn = column

        // 不跳过空白符，因为空白符可能是 TEXT_SEGMENT 的一部分

        // 检查位置
        if (position >= input.length) {
            return DevInsToken.eof(position, line, column)
        }

        // 如果在代码块内部，处理代码内容
        if (context.isCodeStart && !matchString("```")) {
            return consumeCodeContent(startPos, startLine, startColumn)
        }

        when {
            // 前置元数据开始
            matchString("---") -> {
                if (context.isCodeStart) {
                    return createToken(DevInsTokenType.CODE_CONTENT, consumeString("---"), startPos, startLine, startColumn)
                } else {
                    context.isInsideFrontMatter = true
                    context.switchTo(LexerState.FRONT_MATTER_BLOCK)
                    return createToken(DevInsTokenType.FRONTMATTER_START, consumeString("---"), startPos, startLine, startColumn)
                }
            }

            // 换行符
            peek() == '\n' -> {
                advance()
                return createToken(DevInsTokenType.NEWLINE, "\n", startPos, startLine, startColumn)
            }

            // 块注释
            matchString("/*") -> {
                return tokenizeBlockComment(startPos, startLine, startColumn)
            }

            // 行注释
            matchString("//") -> {
                return tokenizeLineComment(startPos, startLine, startColumn)
            }

            // 其他所有内容都通过 content() 方法处理
            else -> {
                return tokenizeContent(startPos, startLine, startColumn)
            }
        }
    }

    // 处理内容 - 模拟原始 Flex 的 content() 方法
    private fun tokenizeContent(startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        val char = peek()

        // 检查是否是代码块开始或结束
        if (matchString("```")) {
            consumeString("```")
            if (context.isCodeStart) {
                // 代码块结束
                context.isCodeStart = false
                return createToken(DevInsTokenType.CODE_BLOCK_END, "```", startPos, startLine, startColumn)
            } else {
                // 代码块开始
                context.isCodeStart = true
                return createToken(DevInsTokenType.CODE_BLOCK_START, "```", startPos, startLine, startColumn)
            }
        }

        // 检查是否是内容注释
        if (char == '[') {
            context.switchTo(LexerState.CONTENT_COMMENT_BLOCK)
            return tokenizeContentComment()
        }

        // 检查第一个字符
        when (char) {
            '@' -> {
                advance()
                context.switchTo(LexerState.AGENT_BLOCK)
                return createToken(DevInsTokenType.AGENT_START, "@", startPos, startLine, startColumn)
            }
            '/' -> {
                advance()
                context.switchTo(LexerState.COMMAND_BLOCK)
                return createToken(DevInsTokenType.COMMAND_START, "/", startPos, startLine, startColumn)
            }
            '$' -> {
                advance()
                context.switchTo(LexerState.VARIABLE_BLOCK)
                return createToken(DevInsTokenType.VARIABLE_START, "$", startPos, startLine, startColumn)
            }
            else -> {
                // 消费文本段直到遇到特殊字符或换行符
                return consumeTextSegment(startPos, startLine, startColumn)
            }
        }
    }

    // 消费文本段
    private fun consumeTextSegment(startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        // 确保至少消费一个字符，避免死循环
        if (position < input.length) {
            advance()
        }

        while (position < input.length) {
            val char = peek()
            if (char in "@/$#\n" || matchString("```")) {
                break
            }
            advance()
        }

        val text = input.substring(startPos, position)
        return createToken(DevInsTokenType.TEXT_SEGMENT, text, startPos, startLine, startColumn)
    }
    
    private fun tokenizeFrontMatter(): DevInsToken {
        val startPos = position
        val startLine = line
        val startColumn = column

        // 跳过空白符（但不包括换行符）
        while (position < input.length && peek() in " \t\r") {
            advance()
        }

        // 重新检查位置
        if (position >= input.length) {
            return DevInsToken.eof(position, line, column)
        }

        when {
            // 生命周期关键字
            matchKeyword("when") -> {
                return createKeywordToken("when", startPos, startLine, startColumn)
            }
            matchKeyword("onStreaming") -> {
                return createKeywordToken("onStreaming", startPos, startLine, startColumn)
            }
            matchKeyword("beforeStreaming") -> {
                return createKeywordToken("beforeStreaming", startPos, startLine, startColumn)
            }
            matchKeyword("onStreamingEnd") -> {
                return createKeywordToken("onStreamingEnd", startPos, startLine, startColumn)
            }
            matchKeyword("afterStreaming") -> {
                return createKeywordToken("afterStreaming", startPos, startLine, startColumn)
            }
            matchKeyword("functions") -> {
                return createKeywordToken("functions", startPos, startLine, startColumn)
            }

            // 标识符
            isIdentifierStart(peek()) -> {
                val identifier = consumeIdentifier()
                return createToken(DevInsTokenType.IDENTIFIER, identifier, startPos, startLine, startColumn)
            }

            // 引用字符串
            peek() == '"' || peek() == '\'' -> {
                val string = consumeQuotedString()
                return createToken(DevInsTokenType.QUOTE_STRING, string, startPos, startLine, startColumn)
            }

            // 模式表达式
            peek() == '/' -> {
                return tokenizePatternExpr(startPos, startLine, startColumn)
            }

            // 冒号
            peek() == ':' -> {
                advance()
                context.switchTo(LexerState.FRONT_MATTER_VALUE_BLOCK)
                return createToken(DevInsTokenType.COLON, ":", startPos, startLine, startColumn)
            }

            // 左大括号
            peek() == '{' -> {
                advance()
                context.patternActionBraceLevel++
                context.switchTo(LexerState.FUNCTION_DECL_BLOCK)
                return createToken(DevInsTokenType.OPEN_BRACE, "{", startPos, startLine, startColumn)
            }

            // 换行符
            peek() == '\n' -> {
                advance()
                return createToken(DevInsTokenType.NEWLINE, "\n", startPos, startLine, startColumn)
            }

            // 前置元数据结束
            matchString("---") -> {
                context.isInsideFrontMatter = false
                context.hasFrontMatter = true
                context.switchTo(LexerState.INITIAL)
                return createToken(DevInsTokenType.FRONTMATTER_END, consumeString("---"), startPos, startLine, startColumn)
            }

            else -> {
                // 回退到初始状态
                context.switchTo(LexerState.INITIAL)
                return tokenizeInitial()
            }
        }
    }
    
    private fun tokenizeFrontMatterValue(): DevInsToken {
        val startPos = position
        val startLine = line
        val startColumn = column

        // 跳过空白符（但不包括换行符）
        while (position < input.length && peek() in " \t\r") {
            advance()
        }

        // 重新检查位置
        if (position >= input.length) {
            return DevInsToken.eof(position, line, column)
        }

        when {
            // 数字
            isDigit(peek()) -> {
                val number = consumeNumber()
                return createToken(DevInsTokenType.NUMBER, number, startPos, startLine, startColumn)
            }

            // 日期格式 YYYY-MM-DD
            isDateStart() -> {
                val date = consumeDate()
                return createToken(DevInsTokenType.DATE, date, startPos, startLine, startColumn)
            }

            // 布尔值
            matchKeyword("true") || matchKeyword("false") ||
            matchKeyword("TRUE") || matchKeyword("FALSE") -> {
                val bool = consumeKeyword()
                return createToken(DevInsTokenType.BOOLEAN, bool, startPos, startLine, startColumn)
            }

            // 标识符
            isIdentifierStart(peek()) -> {
                val identifier = consumeIdentifier()
                return createToken(DevInsTokenType.IDENTIFIER, identifier, startPos, startLine, startColumn)
            }

            // 引用字符串
            peek() == '"' || peek() == '\'' -> {
                val string = consumeQuotedString()
                return createToken(DevInsTokenType.QUOTE_STRING, string, startPos, startLine, startColumn)
            }

            // 模式表达式
            peek() == '/' -> {
                val pattern = consumePatternExpr()
                context.switchTo(LexerState.PATTERN_ACTION_BLOCK)
                return createToken(DevInsTokenType.PATTERN_EXPR, pattern, startPos, startLine, startColumn)
            }

            // 操作符
            matchString("::") -> return createToken(DevInsTokenType.ACCESS, consumeString("::"), startPos, startLine, startColumn)
            matchString("->") -> return createToken(DevInsTokenType.PROCESS, consumeString("->"), startPos, startLine, startColumn)

            // 其他符号
            peek() == '[' -> {
                advance()
                return createToken(DevInsTokenType.LBRACKET, "[", startPos, startLine, startColumn)
            }
            peek() == ']' -> {
                advance()
                return createToken(DevInsTokenType.RBRACKET, "]", startPos, startLine, startColumn)
            }
            peek() == ',' -> {
                advance()
                return createToken(DevInsTokenType.COMMA, ",", startPos, startLine, startColumn)
            }

            // 换行符 - 回到前置元数据块状态
            peek() == '\n' -> {
                context.switchTo(LexerState.FRONT_MATTER_BLOCK)
                return tokenizeFrontMatter()
            }

            else -> {
                // 回退到前置元数据块状态
                context.switchTo(LexerState.FRONT_MATTER_BLOCK)
                return tokenizeFrontMatter()
            }
        }
    }
    
    // 辅助方法
    private fun peek(offset: Int = 0): Char {
        val pos = position + offset
        return if (pos < input.length) input[pos] else '\u0000'
    }
    
    private fun advance(): Char {
        if (position >= input.length) return '\u0000'
        
        val char = input[position]
        position++
        
        if (char == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        
        return char
    }
    
    private fun matchString(str: String): Boolean {
        if (position + str.length > input.length) return false
        return input.substring(position, position + str.length) == str
    }
    
    private fun consumeString(str: String): String {
        if (matchString(str)) {
            repeat(str.length) { advance() }
            return str
        }
        return ""
    }
    
    private fun skipWhitespace() {
        while (position < input.length && input[position] in " \t\r") {
            advance()
        }
    }
    
    private fun createToken(type: DevInsTokenType, text: String, startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        return DevInsToken(type, text, startPos, position, startLine, startColumn)
    }
    
    private fun isIdentifierStart(char: Char): Boolean {
        return char.isLetter() || char == '_'
    }

    private fun isIdentifierPart(char: Char): Boolean {
        return char.isLetterOrDigit() || char == '_' || char == '-'
    }

    private fun isDigit(char: Char): Boolean {
        return char.isDigit()
    }

    private fun isIndent(): Boolean {
        val char = peek()
        return char == ' ' || char == '\t'
    }

    private fun isDateStart(): Boolean {
        // 检查是否为日期格式 YYYY-MM-DD
        if (position + 10 > input.length) return false
        val substr = input.substring(position, position + 10)
        return substr.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    }

    private fun consumeIdentifier(): String {
        val start = position
        while (position < input.length && isIdentifierPart(peek())) {
            advance()
        }
        return input.substring(start, position)
    }

    private fun consumeNumber(): String {
        val start = position
        while (position < input.length && isDigit(peek())) {
            advance()
        }
        return input.substring(start, position)
    }

    private fun consumeDate(): String {
        val start = position
        repeat(10) { advance() } // YYYY-MM-DD
        return input.substring(start, position)
    }

    private fun consumeIndent(): String {
        val start = position
        while (position < input.length && (peek() == ' ' || peek() == '\t')) {
            advance()
        }
        return input.substring(start, position)
    }

    private fun consumeQuotedString(): String {
        val start = position
        val quote = advance() // 消费开始引号

        while (position < input.length) {
            val char = peek()
            if (char == quote) {
                advance() // 消费结束引号
                break
            } else if (char == '\\') {
                advance() // 消费转义字符
                if (position < input.length) {
                    advance() // 消费被转义的字符
                }
            } else {
                advance()
            }
        }

        return input.substring(start, position)
    }

    private fun consumePatternExpr(): String {
        val start = position
        advance() // 消费开始的 /

        while (position < input.length) {
            val char = peek()
            if (char == '/') {
                advance() // 消费结束的 /
                break
            } else if (char == '\\') {
                advance() // 消费转义字符
                if (position < input.length) {
                    advance() // 消费被转义的字符
                }
            } else {
                advance()
            }
        }

        return input.substring(start, position)
    }

    private fun matchKeyword(keyword: String): Boolean {
        if (!matchString(keyword)) return false

        // 检查关键字后面不是标识符字符
        val nextChar = peek(keyword.length)
        return !isIdentifierPart(nextChar)
    }

    private fun consumeKeyword(): String {
        val start = position
        while (position < input.length && isIdentifierPart(peek())) {
            advance()
        }
        return input.substring(start, position)
    }

    private fun createKeywordToken(keyword: String, startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        consumeString(keyword)
        val tokenType = DevInsTokenType.getKeywordType(keyword) ?: DevInsTokenType.IDENTIFIER
        return createToken(tokenType, keyword, startPos, startLine, startColumn)
    }

    // 处理代码块的方法
    private fun handleCodeBlock(startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        if (context.isCodeStart) {
            context.isCodeStart = false
            consumeString("```")
            return createToken(DevInsTokenType.CODE_BLOCK_END, "```", startPos, startLine, startColumn)
        } else {
            context.isCodeStart = true
            consumeString("```")
            // 不切换状态，继续在 INITIAL 状态处理
            return createToken(DevInsTokenType.CODE_BLOCK_START, "```", startPos, startLine, startColumn)
        }
    }

    // 处理块注释
    private fun tokenizeBlockComment(startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        advance() // 消费 /
        advance() // 消费 *

        while (position < input.length - 1) {
            if (peek() == '*' && peek(1) == '/') {
                advance() // 消费 *
                advance() // 消费 /
                break
            }
            advance()
        }

        return createToken(DevInsTokenType.BLOCK_COMMENT, input.substring(startPos, position), startPos, startLine, startColumn)
    }

    // 处理行注释
    private fun tokenizeLineComment(startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        while (position < input.length && peek() != '\n') {
            advance()
        }

        return createToken(DevInsTokenType.COMMENTS, input.substring(startPos, position), startPos, startLine, startColumn)
    }



    // 消费代码内容
    private fun consumeCodeContent(startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        while (position < input.length && !matchString("```")) {
            advance()
        }

        val text = input.substring(startPos, position)
        return createToken(DevInsTokenType.CODE_CONTENT, text, startPos, startLine, startColumn)
    }

    // 处理模式表达式
    private fun tokenizePatternExpr(startPos: Int, startLine: Int, startColumn: Int): DevInsToken {
        val pattern = consumePatternExpr()
        return createToken(DevInsTokenType.PATTERN_EXPR, pattern, startPos, startLine, startColumn)
    }

    /**
     * 函数声明块状态的词法分析
     */
    private fun tokenizeFunctionDecl(): DevInsToken {
        val startPos = position
        val startLine = line
        val startColumn = column

        // 跳过空白符（但不包括换行符）
        while (position < input.length && peek() in " \t\r") {
            advance()
        }

        if (position >= input.length) {
            return DevInsToken.eof(position, line, column)
        }

        when {
            // 标识符
            isIdentifierStart(peek()) -> {
                val identifier = consumeIdentifier()
                return createToken(DevInsTokenType.IDENTIFIER, identifier, startPos, startLine, startColumn)
            }

            // 引用字符串
            peek() == '"' || peek() == '\'' -> {
                val string = consumeQuotedString()
                return createToken(DevInsTokenType.QUOTE_STRING, string, startPos, startLine, startColumn)
            }

            // 数字
            isDigit(peek()) -> {
                val number = consumeNumber()
                return createToken(DevInsTokenType.NUMBER, number, startPos, startLine, startColumn)
            }

            // 右大括号 - 结束函数声明块
            peek() == '}' -> {
                advance()
                context.patternActionBraceLevel--
                if (context.patternActionBraceLevel <= 0) {
                    context.patternActionBraceLevel = 0
                    // 根据之前的状态决定返回哪个状态
                    if (context.isInsideFrontMatter) {
                        context.switchTo(LexerState.FRONT_MATTER_BLOCK)
                    } else {
                        context.switchTo(LexerState.INITIAL)
                    }
                }
                return createToken(DevInsTokenType.CLOSE_BRACE, "}", startPos, startLine, startColumn)
            }

            // 左大括号 - 嵌套函数声明块
            peek() == '{' -> {
                advance()
                context.patternActionBraceLevel++
                return createToken(DevInsTokenType.OPEN_BRACE, "{", startPos, startLine, startColumn)
            }

            // 换行符
            peek() == '\n' -> {
                advance()
                return createToken(DevInsTokenType.NEWLINE, "\n", startPos, startLine, startColumn)
            }

            // 其他符号
            peek() == ',' -> {
                advance()
                return createToken(DevInsTokenType.COMMA, ",", startPos, startLine, startColumn)
            }
            peek() == '(' -> {
                advance()
                return createToken(DevInsTokenType.LPAREN, "(", startPos, startLine, startColumn)
            }
            peek() == ')' -> {
                advance()
                return createToken(DevInsTokenType.RPAREN, ")", startPos, startLine, startColumn)
            }
            peek() == '|' -> {
                advance()
                return createToken(DevInsTokenType.PIPE, "|", startPos, startLine, startColumn)
            }

            else -> {
                val char = advance()
                return DevInsToken.badCharacter(char, startPos, startLine, startColumn)
            }
        }
    }

    /**
     * 模式动作块状态的词法分析
     */
    private fun tokenizePatternAction(): DevInsToken {
        val startPos = position
        val startLine = line
        val startColumn = column

        // 跳过空白符（但不包括换行符）
        while (position < input.length && peek() in " \t\r") {
            advance()
        }

        if (position >= input.length) {
            return DevInsToken.eof(position, line, column)
        }

        when {
            // 左大括号 - 开始动作块
            peek() == '{' -> {
                advance()
                context.patternActionBraceLevel++
                context.switchTo(LexerState.FUNCTION_DECL_BLOCK)
                return createToken(DevInsTokenType.OPEN_BRACE, "{", startPos, startLine, startColumn)
            }

            // 换行符 - 回到前置元数据块状态（模式表达式后没有动作块）
            peek() == '\n' -> {
                if (context.isInsideFrontMatter) {
                    context.switchTo(LexerState.FRONT_MATTER_BLOCK)
                } else {
                    context.switchTo(LexerState.INITIAL)
                }
                advance()
                return createToken(DevInsTokenType.NEWLINE, "\n", startPos, startLine, startColumn)
            }

            else -> {
                // 其他情况，回到前置元数据块状态
                if (context.isInsideFrontMatter) {
                    context.switchTo(LexerState.FRONT_MATTER_BLOCK)
                    return tokenizeFrontMatter()
                } else {
                    context.switchTo(LexerState.INITIAL)
                    return tokenizeInitial()
                }
            }
        }
    }

    // 简化的状态处理方法（其他状态的具体实现）
    private fun tokenizeFrontMatterObject(): DevInsToken = tokenizeDefault()
    private fun tokenizeContentComment(): DevInsToken = tokenizeDefault()
    private fun tokenizeUsed(): DevInsToken = tokenizeDefault()
    private fun tokenizeSingleComment(): DevInsToken = tokenizeDefault()

    // <COMMAND_VALUE_BLOCK>
    // {COMMAND_PROP}          { return COMMAND_PROP;  }
    // [^]                     { yypushback(yylength()); yybegin(YYINITIAL); }
    private fun tokenizeCommandValue(): DevInsToken {
        val startPos = position
        val startLine = line
        val startColumn = column

        if (position >= input.length) {
            context.switchTo(LexerState.INITIAL)
            return DevInsToken.eof(position, line, column)
        }

        // COMMAND_PROP: [^\ \t\r\n]* - any characters except space, tab, carriage return, and newline
        val start = position
        while (position < input.length && peek() !in " \t\r\n") {
            advance()
        }

        if (position > start) {
            val commandProp = input.substring(start, position)
            context.switchTo(LexerState.INITIAL)
            return createToken(DevInsTokenType.COMMAND_PROP, commandProp, startPos, startLine, startColumn)
        }

        // No command prop found, switch back to INITIAL
        context.switchTo(LexerState.INITIAL)
        return tokenizeInitial()
    }

    private fun tokenizeLine(): DevInsToken = tokenizeDefault()
    private fun tokenizeExpression(): DevInsToken = tokenizeDefault()
    private fun tokenizeCodeBlock(): DevInsToken = tokenizeDefault()
    private fun tokenizeLanguageId(): DevInsToken = tokenizeDefault()

    // <AGENT_BLOCK>
    // {IDENTIFIER}           { yybegin(YYINITIAL); return IDENTIFIER; }
    // {QUOTE_STRING}         { yybegin(YYINITIAL); return QUOTE_STRING; }
    // [^]                    { yypushback(yylength()); yybegin(YYINITIAL); }
    private fun tokenizeAgent(): DevInsToken {
        // 跳过空白符（但不包括换行符）
        while (position < input.length && peek() in " \t\r") {
            advance()
        }

        val startPos = position
        val startLine = line
        val startColumn = column

        if (position >= input.length) {
            context.switchTo(LexerState.INITIAL)
            return DevInsToken.eof(position, line, column)
        }

        when {
            // IDENTIFIER
            isIdentifierStart(peek()) -> {
                val identifier = consumeIdentifier()
                context.switchTo(LexerState.INITIAL)
                return createToken(DevInsTokenType.IDENTIFIER, identifier, startPos, startLine, startColumn)
            }
            // QUOTE_STRING
            peek() == '"' || peek() == '\'' -> {
                val string = consumeQuotedString()
                context.switchTo(LexerState.INITIAL)
                return createToken(DevInsTokenType.QUOTE_STRING, string, startPos, startLine, startColumn)
            }
            // 其他字符：不消费，切换回 INITIAL 状态，让 INITIAL 状态处理
            else -> {
                context.switchTo(LexerState.INITIAL)
                // 不调用 nextToken()，而是直接调用 tokenizeInitial()
                return tokenizeInitial()
            }
        }
    }

    // <COMMAND_BLOCK>
    // [a-zA-Z0-9][_\-a-zA-Z0-9.]*  { return IDENTIFIER; }
    // {COLON}                 { yybegin(COMMAND_VALUE_BLOCK); return COLON; }
    // [^]                     { yypushback(1); yybegin(YYINITIAL); }
    private fun tokenizeCommand(): DevInsToken {
        // 跳过空白符（但不包括换行符）
        while (position < input.length && peek() in " \t\r") {
            advance()
        }

        val startPos = position
        val startLine = line
        val startColumn = column

        if (position >= input.length) {
            context.switchTo(LexerState.INITIAL)
            return DevInsToken.eof(position, line, column)
        }

        when {
            // IDENTIFIER: [a-zA-Z0-9][_\-a-zA-Z0-9.]*
            isIdentifierStart(peek()) -> {
                val identifier = consumeIdentifier()
                // 不切换状态，继续在 COMMAND_BLOCK 中
                return createToken(DevInsTokenType.IDENTIFIER, identifier, startPos, startLine, startColumn)
            }
            // COLON
            peek() == ':' -> {
                advance()
                context.switchTo(LexerState.COMMAND_VALUE_BLOCK)
                return createToken(DevInsTokenType.COLON, ":", startPos, startLine, startColumn)
            }
            // 其他字符：不消费，切换回 INITIAL 状态
            else -> {
                context.switchTo(LexerState.INITIAL)
                return tokenizeInitial()
            }
        }
    }

    // <VARIABLE_BLOCK>
    // {IDENTIFIER}         { return IDENTIFIER; }
    // "{"                  { return OPEN_BRACE; }
    // "}"                  { return CLOSE_BRACE; }
    // "."                  { return DOT; }
    // "("                  { return LPAREN; }
    // ")"                  { return RPAREN; }
    // [^]                  { yypushback(yylength()); yybegin(YYINITIAL); }
    private fun tokenizeVariable(): DevInsToken {
        // 跳过空白符（但不包括换行符）
        while (position < input.length && peek() in " \t\r") {
            advance()
        }

        val startPos = position
        val startLine = line
        val startColumn = column

        if (position >= input.length) {
            context.switchTo(LexerState.INITIAL)
            return DevInsToken.eof(position, line, column)
        }

        when {
            // IDENTIFIER
            isIdentifierStart(peek()) -> {
                val identifier = consumeIdentifier()
                return createToken(DevInsTokenType.IDENTIFIER, identifier, startPos, startLine, startColumn)
            }
            // {
            peek() == '{' -> {
                advance()
                return createToken(DevInsTokenType.OPEN_BRACE, "{", startPos, startLine, startColumn)
            }
            // }
            peek() == '}' -> {
                advance()
                return createToken(DevInsTokenType.CLOSE_BRACE, "}", startPos, startLine, startColumn)
            }
            // .
            peek() == '.' -> {
                advance()
                return createToken(DevInsTokenType.DOT, ".", startPos, startLine, startColumn)
            }
            // (
            peek() == '(' -> {
                advance()
                return createToken(DevInsTokenType.LPAREN, "(", startPos, startLine, startColumn)
            }
            // )
            peek() == ')' -> {
                advance()
                return createToken(DevInsTokenType.RPAREN, ")", startPos, startLine, startColumn)
            }
            // 其他字符：不消费，切换回 INITIAL 状态
            else -> {
                context.switchTo(LexerState.INITIAL)
                return tokenizeInitial()
            }
        }
    }

    private fun tokenizeDefault(): DevInsToken {
        val startPos = position
        val startLine = line
        val startColumn = column

        if (position >= input.length) {
            return DevInsToken.eof(position, line, column)
        }

        val char = advance()
        return DevInsToken.badCharacter(char, startPos, startLine, startColumn)
    }
}
