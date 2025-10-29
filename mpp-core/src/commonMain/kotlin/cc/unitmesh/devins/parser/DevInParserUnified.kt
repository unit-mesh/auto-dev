package cc.unitmesh.devins.parser

/**
 * Unified DevIn Parser implementation that works across all platforms
 * 
 * This approach consolidates all parsing logic into commonMain,
 * eliminating the need for platform-specific implementations.
 */
class DevInParserUnified {
    private val lexer = DevInLexerUnified()

    fun parse(input: String): DevInParseResult {
        return try {
            val tokens = lexer.tokenize(input)
            parseTokens(tokens)
        } catch (e: Exception) {
            DevInParseResult(
                ast = null,
                errors = listOf(
                    DevInParseError(
                        message = "Parse error: ${e.message}",
                        line = 1,
                        column = 1,
                        offset = 0,
                        token = null
                    )
                )
            )
        }
    }

    fun parseTokens(tokens: List<DevInToken>): DevInParseResult {
        return try {
            val parser = SimpleDevInParserUnified(tokens)
            val ast = parser.parseFile()
            DevInParseResult(ast = ast, errors = parser.errors)
        } catch (e: Exception) {
            DevInParseResult(
                ast = null,
                errors = listOf(
                    DevInParseError(
                        message = "Parse error: ${e.message}",
                        line = 1,
                        column = 1,
                        offset = 0,
                        token = null
                    )
                )
            )
        }
    }

    fun parseRule(ruleName: String, input: String): DevInParseResult {
        return parse(input)
    }
}

/**
 * Unified DevIn Lexer implementation
 */
class DevInLexerUnified {
    fun tokenize(input: String): List<DevInToken> {
        val tokens = mutableListOf<DevInToken>()
        var pos = 0
        var currentLine = 1
        var currentColumn = 1

        while (pos < input.length) {
            val char = input[pos]
            val startPos = pos
            val startLine = currentLine
            val startColumn = currentColumn

            when {
                char == '\n' -> {
                    tokens.add(createToken(DevInTokenTypes.NEWLINE, "\n", startPos, startLine, startColumn))
                    currentLine++
                    currentColumn = 1
                    pos++
                }
                char.isWhitespace() -> {
                    val whitespace = consumeWhitespace(input, pos)
                    tokens.add(createToken(DevInTokenTypes.WHITE_SPACE, whitespace, startPos, startLine, startColumn))
                    currentColumn += whitespace.length
                    pos += whitespace.length
                }
                char == '@' -> {
                    tokens.add(createToken(DevInTokenTypes.AGENT_START, "@", startPos, startLine, startColumn))
                    currentColumn++
                    pos++
                }
                char == '/' && pos + 1 < input.length && input[pos + 1] == '/' -> {
                    val comment = consumeLineComment(input, pos)
                    tokens.add(createToken(DevInTokenTypes.COMMENTS, comment, startPos, startLine, startColumn))
                    currentColumn += comment.length
                    pos += comment.length
                }
                char == '/' -> {
                    tokens.add(createToken(DevInTokenTypes.COMMAND_START, "/", startPos, startLine, startColumn))
                    currentColumn++
                    pos++
                }
                char == '$' -> {
                    tokens.add(createToken(DevInTokenTypes.VARIABLE_START, "$", startPos, startLine, startColumn))
                    currentColumn++
                    pos++
                }
                char == '#' -> {
                    tokens.add(createToken(DevInTokenTypes.SHARP, "#", startPos, startLine, startColumn))
                    currentColumn++
                    pos++
                }
                input.startsWith("```", pos) -> {
                    tokens.add(createToken(DevInTokenTypes.CODE_BLOCK_START, "```", startPos, startLine, startColumn))
                    currentColumn += 3
                    pos += 3
                }
                input.startsWith("---", pos) -> {
                    tokens.add(createToken(DevInTokenTypes.FRONTMATTER_START, "---", startPos, startLine, startColumn))
                    currentColumn += 3
                    pos += 3
                }
                char.isLetter() || char == '_' -> {
                    // Context-aware tokenization
                    val prevToken = tokens.lastOrNull()
                    val isAfterSpecialMarker = prevToken?.type in listOf(
                        DevInTokenTypes.AGENT_START,
                        DevInTokenTypes.COMMAND_START,
                        DevInTokenTypes.VARIABLE_START,
                        DevInTokenTypes.CODE_BLOCK_START
                    )
                    
                    if (isAfterSpecialMarker) {
                        val identifier = consumeIdentifier(input, pos)
                        val tokenType = getKeywordType(identifier)
                        tokens.add(createToken(tokenType, identifier, startPos, startLine, startColumn))
                        currentColumn += identifier.length
                        pos += identifier.length
                    } else {
                        val textSegment = consumeTextSegment(input, pos)
                        tokens.add(createToken(DevInTokenTypes.TEXT_SEGMENT, textSegment, startPos, startLine, startColumn))
                        currentColumn += textSegment.length
                        pos += textSegment.length
                    }
                }
                char.isDigit() -> {
                    val number = consumeNumber(input, pos)
                    tokens.add(createToken(DevInTokenTypes.NUMBER, number, startPos, startLine, startColumn))
                    currentColumn += number.length
                    pos += number.length
                }
                char == '"' -> {
                    val string = consumeQuotedString(input, pos, '"')
                    tokens.add(createToken(DevInTokenTypes.QUOTE_STRING, string, startPos, startLine, startColumn))
                    currentColumn += string.length
                    pos += string.length
                }
                char == '\'' -> {
                    val string = consumeQuotedString(input, pos, '\'')
                    tokens.add(createToken(DevInTokenTypes.QUOTE_STRING, string, startPos, startLine, startColumn))
                    currentColumn += string.length
                    pos += string.length
                }
                else -> {
                    val tokenType = getOperatorType(input, pos)
                    val operatorLength = getOperatorLength(input, pos)
                    val operator = input.substring(pos, minOf(pos + operatorLength, input.length))
                    tokens.add(createToken(tokenType, operator, startPos, startLine, startColumn))
                    currentColumn += operatorLength
                    pos += operatorLength
                }
            }
        }

        return tokens
    }

    private fun createToken(type: String, value: String, offset: Int, line: Int, column: Int): DevInToken {
        return object : DevInToken {
            override val type: String = type
            override val value: String = value
            override val text: String = value
            override val offset: Int = offset
            override val lineBreaks: Int = if (value.contains('\n')) value.count { it == '\n' } else 0
            override val line: Int = line
            override val col: Int = column
        }
    }

    private fun consumeWhitespace(input: String, start: Int): String {
        var pos = start
        while (pos < input.length && input[pos].isWhitespace() && input[pos] != '\n') {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun consumeLineComment(input: String, start: Int): String {
        var pos = start
        while (pos < input.length && input[pos] != '\n') {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun consumeIdentifier(input: String, start: Int): String {
        var pos = start
        while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == '_' || input[pos] == '-' || input[pos] == '.')) {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun consumeTextSegment(input: String, start: Int): String {
        var pos = start
        while (pos < input.length && input[pos] != '$' && input[pos] != '/' && input[pos] != '@' && input[pos] != '#' && input[pos] != '\n' && !input[pos].isWhitespace()) {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun consumeNumber(input: String, start: Int): String {
        var pos = start
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun consumeQuotedString(input: String, start: Int, quote: Char): String {
        var pos = start + 1
        while (pos < input.length && input[pos] != quote) {
            if (input[pos] == '\\' && pos + 1 < input.length) {
                pos += 2
            } else {
                pos++
            }
        }
        if (pos < input.length) pos++
        return input.substring(start, pos)
    }

    private fun getKeywordType(identifier: String): String {
        return when (identifier) {
            "true", "false" -> DevInTokenTypes.BOOLEAN
            "if" -> DevInTokenTypes.IF
            "else" -> DevInTokenTypes.ELSE
            "elseif" -> DevInTokenTypes.ELSEIF
            "end" -> DevInTokenTypes.END
            "endif" -> DevInTokenTypes.ENDIF
            "when" -> DevInTokenTypes.WHEN
            "case" -> DevInTokenTypes.CASE
            "default" -> DevInTokenTypes.DEFAULT
            "from" -> DevInTokenTypes.FROM
            "where" -> DevInTokenTypes.WHERE
            "select" -> DevInTokenTypes.SELECT
            "condition" -> DevInTokenTypes.CONDITION
            "functions" -> DevInTokenTypes.FUNCTIONS
            "onStreaming" -> DevInTokenTypes.ON_STREAMING
            "beforeStreaming" -> DevInTokenTypes.BEFORE_STREAMING
            "onStreamingEnd" -> DevInTokenTypes.ON_STREAMING_END
            "afterStreaming" -> DevInTokenTypes.AFTER_STREAMING
            else -> DevInTokenTypes.IDENTIFIER
        }
    }

    private fun getOperatorType(input: String, pos: Int): String {
        return when {
            input.startsWith("::", pos) -> DevInTokenTypes.ACCESS
            input.startsWith("==", pos) -> DevInTokenTypes.EQEQ
            input.startsWith("=>", pos) -> DevInTokenTypes.ARROW
            input.startsWith("->", pos) -> DevInTokenTypes.PROCESS
            input.startsWith("!=", pos) -> DevInTokenTypes.NEQ
            input.startsWith("<=", pos) -> DevInTokenTypes.LTE
            input.startsWith(">=", pos) -> DevInTokenTypes.GTE
            input.startsWith("&&", pos) -> DevInTokenTypes.ANDAND
            input.startsWith("||", pos) -> DevInTokenTypes.OROR
            else -> when (input[pos]) {
                ':' -> DevInTokenTypes.COLON
                '(' -> DevInTokenTypes.LPAREN
                ')' -> DevInTokenTypes.RPAREN
                '[' -> DevInTokenTypes.LBRACKET
                ']' -> DevInTokenTypes.RBRACKET
                '{' -> DevInTokenTypes.OPEN_BRACE
                '}' -> DevInTokenTypes.CLOSE_BRACE
                ',' -> DevInTokenTypes.COMMA
                '.' -> DevInTokenTypes.DOT
                '|' -> DevInTokenTypes.PIPE
                '<' -> DevInTokenTypes.LT
                '>' -> DevInTokenTypes.GT
                '!' -> DevInTokenTypes.NOT
                else -> DevInTokenTypes.TEXT_SEGMENT
            }
        }
    }

    private fun getOperatorLength(input: String, pos: Int): Int {
        return when {
            input.startsWith("::", pos) -> 2
            input.startsWith("==", pos) -> 2
            input.startsWith("=>", pos) -> 2
            input.startsWith("->", pos) -> 2
            input.startsWith("!=", pos) -> 2
            input.startsWith("<=", pos) -> 2
            input.startsWith(">=", pos) -> 2
            input.startsWith("&&", pos) -> 2
            input.startsWith("||", pos) -> 2
            else -> 1
        }
    }
}

/**
 * Simplified parser implementation that works across all platforms
 */
private class SimpleDevInParserUnified(private val tokens: List<DevInToken>) {
    private var position = 0
    val errors = mutableListOf<DevInParseError>()

    fun parseFile(): DevInASTNode {
        val children = mutableListOf<DevInASTNode>()
        val startOffset = currentToken()?.offset ?: 0
        val startLine = currentToken()?.line ?: 1
        val startColumn = currentToken()?.col ?: 1

        while (!isAtEnd()) {
            try {
                val node = parseTopLevel()
                if (node != null) {
                    children.add(node)
                }
            } catch (e: Exception) {
                errors.add(
                    DevInParseError(
                        message = "Parse error: ${e.message}",
                        line = currentToken()?.line ?: 1,
                        column = currentToken()?.col ?: 1,
                        offset = currentToken()?.offset ?: 0,
                        token = currentToken()
                    )
                )
                advance()
            }
        }

        val endOffset = if (tokens.isNotEmpty()) tokens.last().offset + tokens.last().value.length else 0

        return object : DevInASTNode {
            override val type = DevInASTNodeTypes.FILE
            override val children = children
            override val startOffset = startOffset
            override val endOffset = endOffset
            override val line = startLine
            override val column = startColumn
        }
    }

    private fun parseTopLevel(): DevInASTNode? {
        val token = currentToken() ?: return null

        return when (token.type) {
            DevInTokenTypes.FRONTMATTER_START -> parseFrontMatter()
            DevInTokenTypes.CODE_BLOCK_START -> parseCodeBlock()
            DevInTokenTypes.AGENT_START -> parseAgentBlock()
            DevInTokenTypes.COMMAND_START -> parseCommandBlock()
            DevInTokenTypes.VARIABLE_START -> parseVariableBlock()
            DevInTokenTypes.SHARP -> parseMarkdownHeader()
            DevInTokenTypes.COMMENTS -> parseComment()
            DevInTokenTypes.NEWLINE -> {
                advance()
                createSimpleNode(DevInASTNodeTypes.NEWLINE, token)
            }
            DevInTokenTypes.WHITE_SPACE -> {
                advance()
                null
            }
            else -> parseTextSegment()
        }
    }

    private fun parseFrontMatter(): DevInASTNode {
        val startToken = consume(DevInTokenTypes.FRONTMATTER_START)
        val children = mutableListOf<DevInASTNode>()

        while (!isAtEnd() && currentToken()?.type != DevInTokenTypes.FRONTMATTER_END) {
            if (currentToken()?.type == DevInTokenTypes.NEWLINE) {
                advance()
                continue
            }

            val entry = parseFrontMatterEntry()
            if (entry != null) {
                children.add(entry)
            }
        }

        val endToken = if (currentToken()?.type == DevInTokenTypes.FRONTMATTER_END) {
            advance()
            currentToken()
        } else null

        return object : DevInASTNode {
            override val type = DevInASTNodeTypes.FRONT_MATTER_HEADER
            override val children = children
            override val startOffset = startToken.offset
            override val endOffset = endToken?.offset ?: startToken.offset + startToken.value.length
            override val line = startToken.line
            override val column = startToken.col
        }
    }

    private fun parseFrontMatterEntry(): DevInASTNode? {
        val keyToken = currentToken()
        if (keyToken?.type != DevInTokenTypes.IDENTIFIER) {
            advance()
            return null
        }
        advance()

        if (currentToken()?.type == DevInTokenTypes.COLON) {
            advance()
            val valueToken = currentToken()
            if (valueToken != null) {
                advance()
                return createSimpleNode(DevInASTNodeTypes.FRONT_MATTER_ENTRY, keyToken, valueToken)
            }
        }

        return createSimpleNode(DevInASTNodeTypes.FRONT_MATTER_KEY, keyToken)
    }

    private fun parseCodeBlock(): DevInASTNode {
        val startToken = consume(DevInTokenTypes.CODE_BLOCK_START)
        val languageToken = if (currentToken()?.type == DevInTokenTypes.IDENTIFIER) {
            advance()
            currentToken()
        } else null

        val contentTokens = mutableListOf<DevInToken>()
        while (!isAtEnd() && currentToken()?.type != DevInTokenTypes.CODE_BLOCK_END) {
            contentTokens.add(currentToken()!!)
            advance()
        }

        val endToken = if (currentToken()?.type == DevInTokenTypes.CODE_BLOCK_END) {
            advance()
            currentToken()
        } else null

        return object : DevInASTNode {
            override val type = DevInASTNodeTypes.CODE_BLOCK
            override val children = emptyList<DevInASTNode>()
            override val startOffset = startToken.offset
            override val endOffset = endToken?.offset ?: (contentTokens.lastOrNull()?.offset ?: startToken.offset) + (contentTokens.lastOrNull()?.value?.length ?: startToken.value.length)
            override val line = startToken.line
            override val column = startToken.col
        }
    }

    private fun parseAgentBlock(): DevInASTNode {
        val startToken = consume(DevInTokenTypes.AGENT_START)
        val idToken = if (currentToken()?.type == DevInTokenTypes.IDENTIFIER || currentToken()?.type == DevInTokenTypes.QUOTE_STRING) {
            val token = currentToken()!!
            advance()
            token
        } else {
            startToken
        }

        return createSimpleNode(DevInASTNodeTypes.AGENT_BLOCK, startToken, idToken)
    }

    private fun parseCommandBlock(): DevInASTNode {
        val startToken = consume(DevInTokenTypes.COMMAND_START)
        val idToken = if (currentToken()?.type == DevInTokenTypes.IDENTIFIER) {
            val token = currentToken()!!
            advance()
            token
        } else {
            startToken
        }

        val propToken = if (currentToken()?.type == DevInTokenTypes.COLON) {
            advance()
            if (currentToken()?.type == DevInTokenTypes.COMMAND_PROP) {
                val token = currentToken()!!
                advance()
                token
            } else null
        } else null

        return if (propToken != null) {
            createSimpleNode(DevInASTNodeTypes.COMMAND_BLOCK, startToken, idToken, propToken)
        } else {
            createSimpleNode(DevInASTNodeTypes.COMMAND_BLOCK, startToken, idToken)
        }
    }

    private fun parseVariableBlock(): DevInASTNode {
        val startToken = consume(DevInTokenTypes.VARIABLE_START)
        val idToken = if (currentToken()?.type == DevInTokenTypes.IDENTIFIER) {
            val token = currentToken()!!
            advance()
            token
        } else {
            startToken
        }

        return createSimpleNode(DevInASTNodeTypes.VARIABLE_BLOCK, startToken, idToken)
    }

    private fun parseMarkdownHeader(): DevInASTNode {
        val startToken = consume(DevInTokenTypes.SHARP)
        val textTokens = mutableListOf<DevInToken>()

        while (!isAtEnd() && currentToken()?.type != DevInTokenTypes.NEWLINE) {
            textTokens.add(currentToken()!!)
            advance()
        }

        return createSimpleNode(DevInASTNodeTypes.MARKDOWN_HEADER, startToken, *textTokens.toTypedArray())
    }

    private fun parseComment(): DevInASTNode {
        val token = consume(DevInTokenTypes.COMMENTS)
        return createSimpleNode(DevInASTNodeTypes.COMMENTS, token)
    }

    private fun parseTextSegment(): DevInASTNode {
        val tokens = mutableListOf<DevInToken>()

        while (!isAtEnd() && !isSpecialToken(currentToken()!!)) {
            tokens.add(currentToken()!!)
            advance()
        }

        return if (tokens.isNotEmpty()) {
            createSimpleNode(DevInASTNodeTypes.TEXT_SEGMENT, *tokens.toTypedArray())
        } else {
            createSimpleNode(DevInASTNodeTypes.TEXT_SEGMENT, currentToken()!!)
        }
    }

    private fun isSpecialToken(token: DevInToken): Boolean {
        return when (token.type) {
            DevInTokenTypes.FRONTMATTER_START,
            DevInTokenTypes.CODE_BLOCK_START,
            DevInTokenTypes.AGENT_START,
            DevInTokenTypes.COMMAND_START,
            DevInTokenTypes.VARIABLE_START,
            DevInTokenTypes.SHARP,
            DevInTokenTypes.COMMENTS,
            DevInTokenTypes.NEWLINE -> true
            else -> false
        }
    }

    private fun createSimpleNode(type: String, vararg tokens: DevInToken): DevInASTNode {
        val firstToken = tokens.firstOrNull()
        val lastToken = tokens.lastOrNull()

        return object : DevInASTNode {
            override val type = type
            override val children = emptyList<DevInASTNode>()
            override val startOffset = firstToken?.offset ?: 0
            override val endOffset = lastToken?.let { it.offset + it.value.length } ?: 0
            override val line = firstToken?.line ?: 1
            override val column = firstToken?.col ?: 1
        }
    }

    private fun currentToken(): DevInToken? {
        return if (position < tokens.size) tokens[position] else null
    }

    private fun advance(): DevInToken? {
        val token = currentToken()
        if (!isAtEnd()) position++
        return token
    }

    private fun consume(expectedType: String): DevInToken {
        val token = currentToken()
        if (token?.type == expectedType) {
            advance()
            return token
        } else {
            throw IllegalStateException("Expected $expectedType but got ${token?.type}")
        }
    }

    private fun isAtEnd(): Boolean {
        return position >= tokens.size
    }
}
