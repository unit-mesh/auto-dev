package cc.unitmesh.devins.parser

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.lexer.DevInsLexer
import cc.unitmesh.devins.token.DevInsToken
import cc.unitmesh.devins.token.DevInsTokenType
import cc.unitmesh.devins.token.TokenPosition

/**
 * DevIns 语法分析器
 * 基于递归下降解析算法实现
 */
class DevInsParser(
    private val tokens: List<DevInsToken>
) {
    private val logger = getLogger("DevInsParser")
    private val state = ParserState()
    private val context = ParseContext()
    
    constructor(lexer: DevInsLexer) : this(lexer.tokenize())
    constructor(input: String) : this(DevInsLexer(input))
    
    /**
     * 解析 DevIns 文件
     */
    fun parse(): ParseResult<DevInsFileNode> {
        return try {
            val children = mutableListOf<DevInsNode>()
            
            // 解析前置元数据（可选）
            val frontMatter = parseFrontMatter()
            if (frontMatter != null) {
                children.add(frontMatter)
            }
            
            // 解析文件内容
            while (!isAtEnd()) {
                val node = parseTopLevelElement()
                if (node != null) {
                    children.add(node)
                }
            }
            
            val fileNode = DevInsFileNode(children)
            
            if (context.hasErrors()) {
                ParseResult.failure(context.getFirstError()!!)
            } else {
                ParseResult.success(fileNode)
            }
        } catch (e: Exception) {
            val position = getCurrentPosition()
            ParseResult.failure(ParseError("Unexpected error during parsing", position, e))
        }
    }
    
    /**
     * 解析前置元数据
     */
    private fun parseFrontMatter(): DevInsFrontMatterNode? {
        if (!match(DevInsTokenType.FRONTMATTER_START)) {
            return null
        }
        
        val children = mutableListOf<DevInsNode>()
        children.add(DevInsTokenNode(previous()))
        
        // 跳过换行符
        if (match(DevInsTokenType.NEWLINE)) {
            children.add(DevInsTokenNode(previous()))
        }
        
        // 解析前置元数据条目
        val entries = mutableListOf<DevInsFrontMatterEntryNode>()
        while (!check(DevInsTokenType.FRONTMATTER_END) && !isAtEnd()) {
            // 跳过换行符和注释
            if (match(DevInsTokenType.NEWLINE) || match(DevInsTokenType.COMMENTS)) {
                children.add(DevInsTokenNode(previous()))
                continue
            }

            val entry = parseFrontMatterEntry()
            if (entry != null) {
                entries.add(entry)
                children.add(entry)
            } else {
                // 如果无法解析条目，跳过当前 token
                if (!check(DevInsTokenType.FRONTMATTER_END)) {
                    advance()
                }
            }
        }
        
        // 消费结束标记
        if (match(DevInsTokenType.FRONTMATTER_END)) {
            children.add(DevInsTokenNode(previous()))
        } else {
            addError("Expected '---' to close front matter")
        }
        
        return DevInsFrontMatterNode(children)
    }
    
    /**
     * 解析前置元数据条目
     */
    private fun parseFrontMatterEntry(): DevInsFrontMatterEntryNode? {
        val children = mutableListOf<DevInsNode>()

        // 跳过缩进
        while (match(DevInsTokenType.INDENT)) {
            children.add(DevInsTokenNode(previous()))
        }

        // 解析键
        val key = when {
            check(DevInsTokenType.IDENTIFIER) -> {
                val token = advance()
                children.add(DevInsTokenNode(token))
                DevInsIdentifierNode(token.text, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.QUOTE_STRING) -> {
                val token = advance()
                children.add(DevInsTokenNode(token))
                DevInsLiteralNode(token.text, DevInsLiteralNode.LiteralType.STRING, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.PATTERN_EXPR) -> {
                val token = advance()
                children.add(DevInsTokenNode(token))
                DevInsPatternNode(token.text, null, listOf(DevInsTokenNode(token)))
            }
            // 支持生命周期关键字作为键
            check(DevInsTokenType.WHEN) || check(DevInsTokenType.ON_STREAMING) ||
            check(DevInsTokenType.BEFORE_STREAMING) || check(DevInsTokenType.ON_STREAMING_END) ||
            check(DevInsTokenType.AFTER_STREAMING) || check(DevInsTokenType.FUNCTIONS) -> {
                val token = advance()
                children.add(DevInsTokenNode(token))
                DevInsIdentifierNode(token.text, listOf(DevInsTokenNode(token)))
            }
            else -> {
                addError("Expected identifier, string, or pattern in front matter entry")
                return null
            }
        }

        // 消费冒号
        if (!match(DevInsTokenType.COLON)) {
            addError("Expected ':' after front matter key")
            return null
        }
        children.add(DevInsTokenNode(previous()))

        // 解析值
        val value = parseFrontMatterValue()
        if (value != null) {
            children.add(value)
        }

        // 跳过注释和换行符
        while (match(DevInsTokenType.COMMENTS, DevInsTokenType.NEWLINE)) {
            children.add(DevInsTokenNode(previous()))
        }

        return DevInsFrontMatterEntryNode(key, value, children)
    }
    
    /**
     * 解析前置元数据值
     */
    private fun parseFrontMatterValue(): DevInsNode? {
        // 如果遇到换行符或注释，说明值可能是嵌套的对象，暂时返回 null
        if (check(DevInsTokenType.NEWLINE) || check(DevInsTokenType.COMMENTS)) {
            return null
        }

        return when {
            check(DevInsTokenType.NUMBER) -> {
                val token = advance()
                DevInsLiteralNode(token.text.toIntOrNull() ?: 0, DevInsLiteralNode.LiteralType.NUMBER, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.BOOLEAN) -> {
                val token = advance()
                DevInsLiteralNode(token.text.toBoolean(), DevInsLiteralNode.LiteralType.BOOLEAN, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.DATE) -> {
                val token = advance()
                DevInsLiteralNode(token.text, DevInsLiteralNode.LiteralType.DATE, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.QUOTE_STRING) -> {
                val token = advance()
                DevInsLiteralNode(token.text, DevInsLiteralNode.LiteralType.STRING, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.IDENTIFIER) -> {
                val token = advance()
                DevInsIdentifierNode(token.text, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.PATTERN_EXPR) -> {
                parsePatternAction()
            }
            check(DevInsTokenType.OPEN_BRACE) -> {
                parseFunctionStatement()
            }
            else -> null
        }
    }
    
    /**
     * 解析模式动作
     */
    private fun parsePatternAction(): DevInsPatternNode? {
        if (!check(DevInsTokenType.PATTERN_EXPR)) {
            return null
        }
        
        val patternToken = advance()
        val pattern = patternToken.text
        
        // 解析动作块（可选）
        val action = if (check(DevInsTokenType.OPEN_BRACE)) {
            parseFunctionStatement()
        } else {
            null
        }
        
        val children = mutableListOf<DevInsNode>(DevInsTokenNode(patternToken))
        if (action != null) {
            children.add(action)
        }
        
        return DevInsPatternNode(pattern, action, children)
    }
    
    /**
     * 解析函数语句
     */
    private fun parseFunctionStatement(): DevInsNode? {
        if (!match(DevInsTokenType.OPEN_BRACE)) {
            return null
        }
        
        val children = mutableListOf<DevInsNode>()
        children.add(DevInsTokenNode(previous()))
        
        // 跳过换行符
        while (match(DevInsTokenType.NEWLINE)) {
            children.add(DevInsTokenNode(previous()))
        }
        
        // 解析函数体内容
        while (!check(DevInsTokenType.CLOSE_BRACE) && !isAtEnd()) {
            val element = parseExpression()
            if (element != null) {
                children.add(element)
            } else {
                advance() // 跳过无法解析的内容
            }
        }
        
        // 消费右大括号
        if (match(DevInsTokenType.CLOSE_BRACE)) {
            children.add(DevInsTokenNode(previous()))
        } else {
            addError("Expected '}' to close function statement")
        }
        
        return object : DevInsStatementNode(children) {
            override val nodeType: String = "FunctionStatement"
        }
    }
    
    /**
     * 解析顶层元素
     */
    private fun parseTopLevelElement(): DevInsNode? {
        return when {
            check(DevInsTokenType.AGENT_START) -> parseUsed()
            check(DevInsTokenType.COMMAND_START) -> parseUsed()
            check(DevInsTokenType.VARIABLE_START) -> parseUsed()
            check(DevInsTokenType.CODE_BLOCK_START) -> parseCodeBlock()
            check(DevInsTokenType.SHARP) -> parseVelocityExpression()
            check(DevInsTokenType.TEXT_SEGMENT) -> parseTextSegment()
            check(DevInsTokenType.NEWLINE) -> {
                val token = advance()
                DevInsTokenNode(token)
            }
            check(DevInsTokenType.COMMENTS) || check(DevInsTokenType.CONTENT_COMMENTS) -> {
                val token = advance()
                DevInsTokenNode(token)
            }
            else -> {
                if (!isAtEnd()) {
                    addError("Unexpected token: ${peek().type}")
                    advance() // 跳过无法识别的 token
                }
                null
            }
        }
    }
    
    // 辅助方法
    private fun match(vararg types: DevInsTokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }
    
    private fun check(type: DevInsTokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }
    
    private fun advance(): DevInsToken {
        if (!isAtEnd()) state.tokenIndex++
        return previous()
    }
    
    private fun isAtEnd(): Boolean {
        return state.tokenIndex >= tokens.size || peek().type == DevInsTokenType.EOF
    }
    
    private fun peek(): DevInsToken {
        return if (state.tokenIndex < tokens.size) tokens[state.tokenIndex] else DevInsToken.eof(0, 1, 1)
    }
    
    private fun previous(): DevInsToken {
        return tokens[state.tokenIndex - 1]
    }
    
    private fun getCurrentPosition(): TokenPosition {
        val token = peek()
        return TokenPosition(token.startOffset, token.line, token.column)
    }
    
    private fun addError(message: String) {
        context.addError(message, getCurrentPosition())
    }
    
    /**
     * 解析文本段
     */
    private fun parseTextSegment(): DevInsNode? {
        if (!check(DevInsTokenType.TEXT_SEGMENT)) {
            return null
        }

        val token = advance()
        return DevInsTextSegmentNode(token.text, listOf(DevInsTokenNode(token)))
    }

    /**
     * 解析表达式（简化版本，用于函数体）
     */
    private fun parseExpression(): DevInsNode? {
        return when {
            check(DevInsTokenType.IDENTIFIER) -> {
                val token = advance()
                DevInsIdentifierNode(token.text, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.QUOTE_STRING) -> {
                val token = advance()
                DevInsLiteralNode(token.text, DevInsLiteralNode.LiteralType.STRING, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.NUMBER) -> {
                val token = advance()
                DevInsLiteralNode(token.text.toIntOrNull() ?: 0, DevInsLiteralNode.LiteralType.NUMBER, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.BOOLEAN) -> {
                val token = advance()
                DevInsLiteralNode(token.text.toBoolean(), DevInsLiteralNode.LiteralType.BOOLEAN, listOf(DevInsTokenNode(token)))
            }
            check(DevInsTokenType.VARIABLE_START) -> parseUsed()
            check(DevInsTokenType.PIPE) || check(DevInsTokenType.COMMA) ||
            check(DevInsTokenType.LPAREN) || check(DevInsTokenType.RPAREN) ||
            check(DevInsTokenType.NEWLINE) || check(DevInsTokenType.COMMENTS) -> {
                val token = advance()
                DevInsTokenNode(token)
            }
            else -> null
        }
    }

    /**
     * 解析 Used 节点（Agent、Command、Variable）
     * 生成 DevInsUsedNode 作为统一的 AST 节点，与 IDEA 版本保持一致
     */
    private fun parseUsed(): DevInsNode? {
        if (!check(DevInsTokenType.AGENT_START) &&
            !check(DevInsTokenType.COMMAND_START) &&
            !check(DevInsTokenType.VARIABLE_START)) {
            return null
        }

        val startToken = advance()
        val children = mutableListOf<DevInsNode>(DevInsTokenNode(startToken))

        // 确定 Used 节点的类型
        val usedType = when (startToken.type) {
            DevInsTokenType.AGENT_START -> DevInsUsedNode.UsedType.AGENT
            DevInsTokenType.COMMAND_START -> DevInsUsedNode.UsedType.COMMAND
            DevInsTokenType.VARIABLE_START -> DevInsUsedNode.UsedType.VARIABLE
            else -> return null
        }

        // 尝试解析标识符（如果存在）
        // 支持带点号的命令名（如 speckit.clarify）
        var name = ""
        if (check(DevInsTokenType.IDENTIFIER)) {
            val identifierToken = advance()
            name = identifierToken.text
            children.add(DevInsTokenNode(identifierToken))
            
            // 如果后面跟着 DOT 和 IDENTIFIER，继续拼接
            while (check(DevInsTokenType.DOT)) {
                val dotToken = advance()
                children.add(DevInsTokenNode(dotToken))
                
                if (check(DevInsTokenType.IDENTIFIER)) {
                    val nextIdentifier = advance()
                    name += "." + nextIdentifier.text
                    children.add(DevInsTokenNode(nextIdentifier))
                } else {
                    break
                }
            }
        }

        if (startToken.type == DevInsTokenType.COMMAND_START) {
            if (check(DevInsTokenType.COLON)) {
                val colonToken = advance()
                children.add(DevInsTokenNode(colonToken))

                // 消费命令属性
                if (check(DevInsTokenType.COMMAND_PROP)) {
                    val propToken = advance()
                    children.add(DevInsTokenNode(propToken))
                }
            } else {
                while (check(DevInsTokenType.IDENTIFIER)) {
                    val argToken = advance()
                    children.add(DevInsTokenNode(argToken))
                }
            }
        }

        val identifierNode = DevInsIdentifierNode(name, listOf())
        
        val usedNode = DevInsUsedNode(
            type = usedType,
            identifier = identifierNode,
            children = children
        )

        logger.debug { "🔍 [DevInsParser] Parsed Used node: type=$usedType, name='$name'" }

        return usedNode
    }

    /**
     * 解析代码块
     * 临时实现：消费 token 并创建基本节点以避免无限循环
     */
    private fun parseCodeBlock(): DevInsNode? {
        if (!check(DevInsTokenType.CODE_BLOCK_START)) {
            return null
        }

        val children = mutableListOf<DevInsNode>()
        val startToken = advance()
        children.add(DevInsTokenNode(startToken))

        // 消费语言标识符（如果存在）
        var language: String? = null
        if (check(DevInsTokenType.LANGUAGE_ID)) {
            val langToken = advance()
            language = langToken.text
            children.add(DevInsTokenNode(langToken))
        }

        // 消费代码内容直到遇到结束标记
        val contentBuilder = StringBuilder()
        while (!check(DevInsTokenType.CODE_BLOCK_END) && !isAtEnd()) {
            val token = advance()
            contentBuilder.append(token.text)
            children.add(DevInsTokenNode(token))
        }

        // 消费结束标记
        if (match(DevInsTokenType.CODE_BLOCK_END)) {
            children.add(DevInsTokenNode(previous()))
        }

        return DevInsCodeBlockNode(language, contentBuilder.toString(), children)
    }

    /**
     * 解析 Velocity 表达式
     * 临时实现：消费 token 并创建基本节点以避免无限循环
     */
    private fun parseVelocityExpression(): DevInsNode? {
        if (!check(DevInsTokenType.SHARP)) {
            return null
        }

        val children = mutableListOf<DevInsNode>()
        val sharpToken = advance()
        children.add(DevInsTokenNode(sharpToken))

        // 尝试解析标识符或其他内容
        var name = ""
        if (check(DevInsTokenType.IDENTIFIER)) {
            val identifierToken = advance()
            name = identifierToken.text
            children.add(DevInsTokenNode(identifierToken))
        }

        // 返回一个标识符节点作为临时实现
        return DevInsIdentifierNode(name, children)
    }
}
