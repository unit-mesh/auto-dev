package cc.unitmesh.devins.document.docql

/**
 * Lexical analyzer for DocQL
 */
class DocQLLexer(private val input: String) {
    private var position = 0
    private val length = input.length
    
    fun tokenize(): List<DocQLToken> {
        val tokens = mutableListOf<DocQLToken>()
        
        while (position < length) {
            skipWhitespace()
            
            if (position >= length) break
            
            val token = when (val char = peek()) {
                '$' -> {
                    advance()
                    DocQLToken.Root
                }
                '.' -> {
                    advance()
                    DocQLToken.Dot
                }
                '[' -> {
                    advance()
                    DocQLToken.LeftBracket
                }
                ']' -> {
                    advance()
                    DocQLToken.RightBracket
                }
                '(' -> {
                    advance()
                    DocQLToken.LeftParen
                }
                ')' -> {
                    advance()
                    DocQLToken.RightParen
                }
                '*' -> {
                    advance()
                    DocQLToken.Star
                }
                '?' -> {
                    advance()
                    DocQLToken.Question
                }
                '@' -> {
                    advance()
                    DocQLToken.At
                }
                '=' -> {
                    advance()
                    if (peek() == '=') {
                        advance()
                        DocQLToken.Equals
                    } else {
                        throw DocQLException("Unexpected character '=' at position $position")
                    }
                }
                '~' -> {
                    advance()
                    if (peek() == '=') {
                        advance()
                        DocQLToken.Contains
                    } else {
                        throw DocQLException("Unexpected character '~' at position $position")
                    }
                }
                '>' -> {
                    advance()
                    DocQLToken.GreaterThan
                }
                '<' -> {
                    advance()
                    DocQLToken.LessThan
                }
                '"' -> {
                    parseStringLiteral()
                }
                in '0'..'9' -> {
                    parseNumber()
                }
                in 'a'..'z', in 'A'..'Z', '_' -> {
                    parseIdentifier()
                }
                else -> throw DocQLException("Unexpected character '$char' at position $position")
            }
            
            tokens.add(token)
        }
        
        tokens.add(DocQLToken.EOF)
        return tokens
    }
    
    private fun peek(): Char {
        return if (position < length) input[position] else '\u0000'
    }
    
    private fun advance() {
        position++
    }
    
    private fun skipWhitespace() {
        while (position < length && input[position].isWhitespace()) {
            position++
        }
    }
    
    private fun parseStringLiteral(): DocQLToken.StringLiteral {
        advance() // skip opening quote
        val start = position
        
        while (position < length && input[position] != '"') {
            if (input[position] == '\\' && position + 1 < length) {
                position++ // skip escape char
            }
            position++
        }
        
        if (position >= length) {
            throw DocQLException("Unterminated string literal starting at position ${start - 1}")
        }
        
        val value = input.substring(start, position)
        advance() // skip closing quote
        
        return DocQLToken.StringLiteral(value)
    }
    
    private fun parseNumber(): DocQLToken.Number {
        val start = position
        
        while (position < length && input[position].isDigit()) {
            position++
        }
        
        val value = input.substring(start, position).toInt()
        return DocQLToken.Number(value)
    }
    
    private fun parseIdentifier(): DocQLToken.Identifier {
        val start = position
        
        while (position < length) {
            val char = input[position]
            if (char.isLetterOrDigit() || char == '_') {
                position++
            } else {
                break
            }
        }
        
        val value = input.substring(start, position)
        return DocQLToken.Identifier(value)
    }
}

/**
 * DocQL exception for parsing errors
 */
class DocQLException(message: String) : Exception(message)

