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
                    } else if (peek() == '~') {
                        advance()
                        DocQLToken.RegexMatch
                    } else {
                        // Treat single '=' as '==' for robustness (common typo)
                        DocQLToken.Equals
                    }
                }
                '!' -> {
                    advance()
                    if (peek() == '=') {
                        advance()
                        DocQLToken.NotEquals
                    } else {
                        throw DocQLException("Unexpected character '!' at position $position, expected '!='")
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
                '/' -> {
                    parseRegexLiteral()
                }
                '>' -> {
                    advance()
                    if (peek() == '=') {
                        advance()
                        DocQLToken.GreaterThanOrEquals
                    } else {
                        DocQLToken.GreaterThan
                    }
                }
                '<' -> {
                    advance()
                    if (peek() == '=') {
                        advance()
                        DocQLToken.LessThanOrEquals
                    } else {
                        DocQLToken.LessThan
                    }
                }
                '"' -> {
                    parseStringLiteral('"')
                }
                '\'' -> {
                    parseStringLiteral('\'')
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
    
    private fun parseStringLiteral(quote: Char): DocQLToken.StringLiteral {
        advance() // skip opening quote
        val start = position
        
        while (position < length && input[position] != quote) {
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
    
    private fun parseIdentifier(): DocQLToken {
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
        
        // Check for keyword operators
        return when (value.lowercase()) {
            "startswith" -> DocQLToken.StartsWith
            "endswith" -> DocQLToken.EndsWith
            "starts" -> {
                // Check for "starts with" (two words)
                skipWhitespace()
                if (position < length && tryMatchKeyword("with")) {
                    DocQLToken.StartsWith
                } else {
                    DocQLToken.Identifier(value)
                }
            }
            "ends" -> {
                // Check for "ends with" (two words)
                skipWhitespace()
                if (position < length && tryMatchKeyword("with")) {
                    DocQLToken.EndsWith
                } else {
                    DocQLToken.Identifier(value)
                }
            }
            else -> DocQLToken.Identifier(value)
        }
    }
    
    /**
     * Try to match a keyword (case-insensitive), advancing position if matched
     */
    private fun tryMatchKeyword(keyword: String): Boolean {
        val remaining = input.substring(position).takeWhile { it.isLetter() }
        if (remaining.lowercase() == keyword.lowercase()) {
            position += remaining.length
            return true
        }
        return false
    }
    
    /**
     * Parse regex literal: /pattern/flags
     * Supports flags: i (case-insensitive), m (multiline), s (dotall), g (global)
     */
    private fun parseRegexLiteral(): DocQLToken.RegexLiteral {
        advance() // skip opening /
        val patternStart = position
        
        // Parse the pattern (everything until unescaped /)
        while (position < length && input[position] != '/') {
            if (input[position] == '\\' && position + 1 < length) {
                position += 2 // skip both escape char and the escaped char
            } else {
                position++
            }
        }
        
        if (position >= length) {
            throw DocQLException("Unterminated regex literal starting at position ${patternStart - 1}")
        }
        
        val pattern = input.substring(patternStart, position)
        advance() // skip closing /
        
        // Parse optional flags (i, m, s, g, etc.)
        val flagsStart = position
        while (position < length && input[position] in "imsgu") {
            position++
        }
        val flags = input.substring(flagsStart, position)
        
        return DocQLToken.RegexLiteral(pattern, flags)
    }
}

/**
 * DocQL exception for parsing errors
 */
class DocQLException(message: String) : Exception(message)

