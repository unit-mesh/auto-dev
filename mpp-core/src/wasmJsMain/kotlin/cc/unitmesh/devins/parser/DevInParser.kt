package cc.unitmesh.devins.parser

/**
 * WebAssembly-JS implementation of DevIn Parser
 * 
 * This implementation is based on the JavaScript version but optimized for WASM-JS runtime.
 * Since WASM-JS shares many characteristics with regular JS, most of the implementation
 * is identical to the JS version.
 */
actual class DevInParserImpl : DevInParser {
    private val lexer = DevInLexerImpl()

    actual override fun parse(input: String): DevInParseResult {
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

    actual override fun parseTokens(tokens: List<DevInToken>): DevInParseResult {
        return try {
            val parser = SimpleDevInParser(tokens)
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

    actual override fun parseRule(ruleName: String, input: String): DevInParseResult {
        // Check if the rule name is valid
        val validRules = setOf("file", "frontMatter", "codeBlock", "agentBlock", "commandBlock", "variableBlock", "expressionBlock")
        
        if (!validRules.contains(ruleName)) {
            return DevInParseResult(
                ast = null,
                errors = listOf(
                    DevInParseError(
                        message = "Invalid rule name: $ruleName",
                        line = 1,
                        column = 1,
                        offset = 0,
                        token = null
                    )
                )
            )
        }
        
        // Parse the input and extract the specific rule type
        val fullResult = parse(input)
        if (fullResult.ast == null) {
            return fullResult
        }
        
        // Find the first child node that matches the expected rule type
        val expectedNodeType = when (ruleName) {
            "agentBlock" -> DevInASTNodeTypes.AGENT_BLOCK
            "commandBlock" -> DevInASTNodeTypes.COMMAND_BLOCK
            "variableBlock" -> DevInASTNodeTypes.VARIABLE_BLOCK
            "expressionBlock" -> DevInASTNodeTypes.EXPRESSION_BLOCK
            "codeBlock" -> DevInASTNodeTypes.CODE_BLOCK
            "frontMatter" -> DevInASTNodeTypes.FRONT_MATTER_HEADER
            else -> return fullResult // Return full result for "file" rule
        }
        
        val targetNode = findNodeByType(fullResult.ast!!, expectedNodeType)
        
        return if (targetNode != null) {
            DevInParseResult(ast = targetNode, errors = fullResult.errors)
        } else {
            DevInParseResult(
                ast = null,
                errors = fullResult.errors + listOf(
                    DevInParseError(
                        message = "No $ruleName found in input",
                        line = 1,
                        column = 1,
                        offset = 0,
                        token = null
                    )
                )
            )
        }
    }
    
    private fun findNodeByType(node: DevInASTNode, targetType: String): DevInASTNode? {
        if (node.type == targetType) {
            return node
        }
        
        for (child in node.children) {
            val found = findNodeByType(child, targetType)
            if (found != null) {
                return found
            }
        }
        
        return null
    }
}

/**
 * Simple recursive descent parser for DevIn language (WASM-JS version)
 */
private class SimpleDevInParser(private val tokens: List<DevInToken>) {
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
            DevInTokenTypes.SHARP -> parseSharpExpression()
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

    private fun parseSharpExpression(): DevInASTNode {
        val startToken = consume(DevInTokenTypes.SHARP)

        // Check if this is an expression (like #if, #else, etc.) or a markdown header
        val nextToken = currentToken()
        val isExpression = nextToken?.type == DevInTokenTypes.IF ||
                          nextToken?.type == DevInTokenTypes.ELSE ||
                          nextToken?.type == DevInTokenTypes.ELSEIF ||
                          nextToken?.type == DevInTokenTypes.END ||
                          nextToken?.type == DevInTokenTypes.ENDIF ||
                          nextToken?.type == DevInTokenTypes.WHEN ||
                          nextToken?.type == DevInTokenTypes.CASE ||
                          nextToken?.type == DevInTokenTypes.DEFAULT

        if (isExpression) {
            // Parse as expression block
            val expressionTokens = mutableListOf<DevInToken>()
            expressionTokens.add(startToken)

            while (!isAtEnd() && currentToken()?.type != DevInTokenTypes.NEWLINE) {
                expressionTokens.add(currentToken()!!)
                advance()
            }

            return object : DevInASTNode {
                override val type = DevInASTNodeTypes.EXPRESSION_BLOCK
                override val children = emptyList<DevInASTNode>()
                override val startOffset = startToken.offset
                override val endOffset = expressionTokens.lastOrNull()?.let { it.offset + it.value.length } ?: startToken.offset + startToken.value.length
                override val line = startToken.line
                override val column = startToken.col
            }
        } else {
            // Parse as markdown header
            val textTokens = mutableListOf<DevInToken>()

            while (!isAtEnd() && currentToken()?.type != DevInTokenTypes.NEWLINE) {
                textTokens.add(currentToken()!!)
                advance()
            }

            return createSimpleNode(DevInASTNodeTypes.MARKDOWN_HEADER, startToken, *textTokens.toTypedArray())
        }
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
