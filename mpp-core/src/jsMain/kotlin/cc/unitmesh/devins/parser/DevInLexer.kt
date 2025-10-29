package cc.unitmesh.devins.parser

/**
 * DevIn Language Lexer for JavaScript platform
 *
 * This lexer tokenizes DevIn language source code into a stream of tokens.
 * It supports multiple lexer states to handle different contexts like:
 * - Agent blocks (@agent)
 * - Command blocks (/command)
 * - Variable blocks ($variable)
 * - Code blocks (```)
 * - Front matter (---)
 * - Expression blocks (#expr)
 */

/**
 * DevIn Lexer implementation for JavaScript platform
 *
 * This is a simplified implementation that provides basic tokenization
 * functionality without external JavaScript dependencies.
 */
actual class DevInLexerImpl : DevInLexer {
    private var input: String = ""
    private var position: Int = 0
    private var line: Int = 1
    private var column: Int = 1
    private var tokens: List<DevInToken> = emptyList()
    private var tokenIndex: Int = 0
    private var currentState: String = DevInLexerStates.INITIAL

    actual override fun reset(input: String) {
        this.input = input
        this.position = 0
        this.line = 1
        this.column = 1
        this.tokens = tokenizeInput(input)
        this.tokenIndex = 0
    }

    actual override fun next(): DevInToken? {
        return if (tokenIndex < tokens.size) {
            tokens[tokenIndex++]
        } else null
    }

    actual override fun hasNext(): Boolean {
        return tokenIndex < tokens.size
    }

    actual override fun getCurrentState(): String {
        return currentState
    }

    actual override fun setState(state: String) {
        currentState = state
    }

    actual override fun tokenize(input: String): List<DevInToken> {
        val result = tokenizeInput(input)

        // Debug output removed - all tests passing

        return result
    }

    private fun tokenizeInput(input: String): List<DevInToken> {
        val tokens = mutableListOf<DevInToken>()
        var pos = 0
        var currentLine = 1
        var currentColumn = 1
        var lexerState = DevInLexerStates.INITIAL
        var inFrontMatter = false

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
                    if (!inFrontMatter) {
                        // This is the start of front matter
                        tokens.add(createToken(DevInTokenTypes.FRONTMATTER_START, "---", startPos, startLine, startColumn))
                        inFrontMatter = true
                        lexerState = DevInLexerStates.FRONT_MATTER_BLOCK
                    } else {
                        // This is the end of front matter
                        tokens.add(createToken(DevInTokenTypes.FRONTMATTER_END, "---", startPos, startLine, startColumn))
                        inFrontMatter = false
                        lexerState = DevInLexerStates.INITIAL
                    }
                    currentColumn += 3
                    pos += 3
                }
                char.isLetter() || char == '_' -> {
                    // Check if we're in a special context where identifiers are expected
                    val prevToken = tokens.lastOrNull()
                    val isAfterSpecialMarker = prevToken?.type in listOf(
                        DevInTokenTypes.AGENT_START,
                        DevInTokenTypes.COMMAND_START,
                        DevInTokenTypes.VARIABLE_START,
                        DevInTokenTypes.CODE_BLOCK_START
                    )

                    // Check if we're after # for expressions
                    val isAfterSharp = prevToken?.type == DevInTokenTypes.SHARP

                    // Check if we're in front matter context
                    val isInFrontMatter = inFrontMatter

                    if (isAfterSpecialMarker || isAfterSharp || isInFrontMatter) {
                        val identifier = consumeIdentifier(input, pos)
                        val tokenType = getKeywordType(identifier)
                        tokens.add(createToken(tokenType, identifier, startPos, startLine, startColumn))
                        currentColumn += identifier.length
                        pos += identifier.length
                    } else {
                        // Treat as text segment
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
            // Stop at colon to separate key from value in front matter
            if (input[pos] == ':') break
            pos++
        }
        return input.substring(start, pos)
    }

    private fun consumeTextSegment(input: String, start: Int): String {
        var pos = start
        // TEXT_SEGMENT = [^$/@#\n]+
        while (pos < input.length && input[pos] != '$' && input[pos] != '/' && input[pos] != '@' && input[pos] != '#' && input[pos] != '\n' && !input[pos].isWhitespace() && input[pos] != '(' && input[pos] != ')' && input[pos] != ':') {
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
        var pos = start + 1 // Skip opening quote
        while (pos < input.length && input[pos] != quote) {
            if (input[pos] == '\\' && pos + 1 < input.length) {
                pos += 2 // Skip escaped character
            } else {
                pos++
            }
        }
        if (pos < input.length) pos++ // Include closing quote
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
