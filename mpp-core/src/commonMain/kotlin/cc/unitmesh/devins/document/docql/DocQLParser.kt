package cc.unitmesh.devins.document.docql

/**
 * Parser for DocQL queries
 * 
 * Grammar (EBNF):
 * query         := ROOT path
 * path          := (property | arrayAccess | functionCall)*
 * property      := DOT IDENTIFIER
 * arrayAccess   := LBRACKET (STAR | NUMBER | filter) RBRACKET
 * filter        := QUESTION condition
 * condition     := AT DOT IDENTIFIER operator value
 * operator      := EQUALS | CONTAINS | GREATER_THAN | LESS_THAN
 * value         := STRING | NUMBER
 * functionCall  := DOT IDENTIFIER LPAREN STRING RPAREN
 */
class DocQLParser(private val tokens: List<DocQLToken>) {
    private var position = 0
    
    fun parse(): DocQLQuery {
        val nodes = mutableListOf<DocQLNode>()
        
        // Expect root token
        if (!match(DocQLToken.Root::class)) {
            throw DocQLException("Query must start with '$'")
        }
        nodes.add(DocQLNode.Root)
        
        // Parse path
        while (!isAtEnd()) {
            when {
                check(DocQLToken.Dot::class) -> {
                    advance()
                    
                    // Could be property or function call
                    if (check(DocQLToken.Identifier::class)) {
                        val identifier = current() as DocQLToken.Identifier
                        advance()
                        
                        if (check(DocQLToken.LeftParen::class)) {
                            // Function call
                            nodes.add(parseFunctionCall(identifier.value))
                        } else {
                            // Property
                            nodes.add(DocQLNode.Property(identifier.value))
                        }
                    } else {
                        throw DocQLException("Expected identifier after '.' at position $position")
                    }
                }
                
                check(DocQLToken.LeftBracket::class) -> {
                    nodes.add(parseArrayAccess())
                }
                
                check(DocQLToken.EOF::class) -> {
                    break
                }
                
                else -> {
                    throw DocQLException("Unexpected token '${current()}' at position $position")
                }
            }
        }
        
        return DocQLQuery(nodes)
    }
    
    private fun parseArrayAccess(): DocQLNode.ArrayAccess {
        expect(DocQLToken.LeftBracket::class, "Expected '['")
        
        val result = when {
            check(DocQLToken.Star::class) -> {
                advance()
                DocQLNode.ArrayAccess.All
            }
            
            check(DocQLToken.Number::class) -> {
                val number = current() as DocQLToken.Number
                advance()
                DocQLNode.ArrayAccess.Index(number.value)
            }
            
            check(DocQLToken.Question::class) -> {
                advance()
                val condition = parseFilterCondition()
                DocQLNode.ArrayAccess.Filter(condition)
            }
            
            else -> {
                throw DocQLException("Expected '*', number, or '?' in array access at position $position")
            }
        }
        
        expect(DocQLToken.RightBracket::class, "Expected ']'")
        return result
    }
    
    private fun parseFilterCondition(): FilterCondition {
        // Parse: (@.property operator value)
        
        // Optional opening paren
        val hasOpenParen = check(DocQLToken.LeftParen::class)
        if (hasOpenParen) {
            advance()
        }
        
        expect(DocQLToken.At::class, "Expected '@' in filter condition")
        expect(DocQLToken.Dot::class, "Expected '.' after '@'")
        
        val property = if (check(DocQLToken.Identifier::class)) {
            val id = current() as DocQLToken.Identifier
            advance()
            id.value
        } else {
            throw DocQLException("Expected property name in filter condition at position $position")
        }
        
        val operator = when {
            check(DocQLToken.Equals::class) -> {
                advance()
                "equals"
            }
            check(DocQLToken.NotEquals::class) -> {
                advance()
                "notEquals"
            }
            check(DocQLToken.Contains::class) -> {
                advance()
                "contains"
            }
            check(DocQLToken.RegexMatch::class) -> {
                advance()
                "regex"
            }
            check(DocQLToken.GreaterThan::class) -> {
                advance()
                "greater"
            }
            check(DocQLToken.GreaterThanOrEquals::class) -> {
                advance()
                "greaterOrEquals"
            }
            check(DocQLToken.LessThan::class) -> {
                advance()
                "less"
            }
            check(DocQLToken.LessThanOrEquals::class) -> {
                advance()
                "lessOrEquals"
            }
            check(DocQLToken.StartsWith::class) -> {
                advance()
                "startsWith"
            }
            check(DocQLToken.EndsWith::class) -> {
                advance()
                "endsWith"
            }
            else -> {
                throw DocQLException("Expected operator (==, !=, ~=, =~, >, >=, <, <=, startsWith, endsWith) at position $position")
            }
        }
        
        val condition = when {
            check(DocQLToken.StringLiteral::class) -> {
                val str = current() as DocQLToken.StringLiteral
                advance()
                when (operator) {
                    "equals" -> FilterCondition.Equals(property, str.value)
                    "notEquals" -> FilterCondition.NotEquals(property, str.value)
                    "contains" -> FilterCondition.Contains(property, str.value)
                    "regex" -> FilterCondition.RegexMatch(property, str.value, "")
                    "startsWith" -> FilterCondition.StartsWith(property, str.value)
                    "endsWith" -> FilterCondition.EndsWith(property, str.value)
                    else -> throw DocQLException("String value not valid for operator '$operator'")
                }
            }
            
            check(DocQLToken.RegexLiteral::class) -> {
                val regex = current() as DocQLToken.RegexLiteral
                advance()
                when (operator) {
                    "regex" -> FilterCondition.RegexMatch(property, regex.pattern, regex.flags)
                    else -> throw DocQLException("Regex literal not valid for operator '$operator', use '=~' operator")
                }
            }
            
            check(DocQLToken.Number::class) -> {
                val num = current() as DocQLToken.Number
                advance()
                when (operator) {
                    "equals" -> FilterCondition.Equals(property, num.value.toString())
                    "notEquals" -> FilterCondition.NotEquals(property, num.value.toString())
                    "greater" -> FilterCondition.GreaterThan(property, num.value)
                    "greaterOrEquals" -> FilterCondition.GreaterThanOrEquals(property, num.value)
                    "less" -> FilterCondition.LessThan(property, num.value)
                    "lessOrEquals" -> FilterCondition.LessThanOrEquals(property, num.value)
                    else -> throw DocQLException("Number value not valid for operator '$operator'")
                }
            }
            
            else -> {
                throw DocQLException("Expected value (string, regex, or number) at position $position")
            }
        }
        
        // Optional closing paren
        if (hasOpenParen) {
            expect(DocQLToken.RightParen::class, "Expected ')' to close filter condition")
        }
        
        return condition
    }
    
    private fun parseFunctionCall(name: String): DocQLNode.FunctionCall {
        expect(DocQLToken.LeftParen::class, "Expected '(' for function call")
        
        // Check if there's an argument or if it's an empty call like chunks()
        val argument = if (check(DocQLToken.StringLiteral::class)) {
            val str = current() as DocQLToken.StringLiteral
            advance()
            str.value
        } else if (check(DocQLToken.RightParen::class)) {
            // Empty argument - allowed for functions like chunks(), all()
            ""
        } else {
            throw DocQLException("Expected string argument or ')' for function call at position $position")
        }
        
        expect(DocQLToken.RightParen::class, "Expected ')' to close function call")
        
        return DocQLNode.FunctionCall(name, argument)
    }
    
    private fun current(): DocQLToken {
        return if (position < tokens.size) tokens[position] else DocQLToken.EOF
    }
    
    private fun advance() {
        if (!isAtEnd()) position++
    }
    
    private fun isAtEnd(): Boolean {
        return position >= tokens.size || current() is DocQLToken.EOF
    }
    
    private fun check(tokenClass: kotlin.reflect.KClass<out DocQLToken>): Boolean {
        return !isAtEnd() && current()::class == tokenClass
    }
    
    private fun match(tokenClass: kotlin.reflect.KClass<out DocQLToken>): Boolean {
        if (check(tokenClass)) {
            advance()
            return true
        }
        return false
    }
    
    private fun expect(tokenClass: kotlin.reflect.KClass<out DocQLToken>, message: String) {
        if (!check(tokenClass)) {
            throw DocQLException("$message at position $position, got '${current()}'")
        }
        advance()
    }
}

/**
 * Parse a DocQL query string
 */
fun parseDocQL(query: String): DocQLQuery {
    val lexer = DocQLLexer(query)
    val tokens = lexer.tokenize()
    val parser = DocQLParser(tokens)
    return parser.parse()
}

