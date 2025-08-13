package cc.unitmesh.diagram.parser.mermaid

/**
 * Lexical analyzer for Mermaid class diagrams
 * Based on the lexical rules in classDiagram.jison
 */
class MermaidLexer(private val input: String) {
    private var position = 0
    private var line = 1
    private var column = 1
    private var state = LexerState.INITIAL
    
    private val tokens = mutableListOf<MermaidToken>()
    
    /**
     * Tokenize the input string
     */
    fun tokenize(): List<MermaidToken> {
        tokens.clear()
        position = 0
        line = 1
        column = 1
        state = LexerState.INITIAL
        
        while (position < input.length) {
            if (!scanToken()) {
                // Skip unknown character
                advance()
            }
        }
        
        // Add EOF token
        addToken(TokenType.EOF, "")
        
        return tokens.toList()
    }
    
    private fun scanToken(): Boolean {
        val start = position
        val startLine = line
        val startColumn = column
        
        return when (state) {
            LexerState.INITIAL -> scanInitialState()
            LexerState.STRING -> scanStringState()
            LexerState.BQSTRING -> scanBackquoteStringState()
            LexerState.GENERIC -> scanGenericState()
            LexerState.CLASS -> scanClassState()
            LexerState.CLASS_BODY -> scanClassBodyState()
            LexerState.NAMESPACE -> scanNamespaceState()
            LexerState.NAMESPACE_BODY -> scanNamespaceBodyState()
            LexerState.CALLBACK_NAME -> scanCallbackNameState()
            LexerState.CALLBACK_ARGS -> scanCallbackArgsState()
            LexerState.ACC_TITLE -> scanAccTitleState()
            LexerState.ACC_DESCR -> scanAccDescrState()
            LexerState.ACC_DESCR_MULTILINE -> scanAccDescrMultilineState()
        }
    }
    
    private fun scanInitialState(): Boolean {
        skipWhitespace()
        
        if (position >= input.length) return false
        
        val char = current()
        
        return when {
            // Comments
            char == '%' && peek() == '%' -> {
                skipComment()
                true
            }
            
            // Newlines
            char == '\n' || char == '\r' -> {
                scanNewline()
                true
            }
            
            // Strings
            char == '"' -> {
                state = LexerState.STRING
                advance()
                true
            }
            
            char == '`' -> {
                state = LexerState.BQSTRING
                advance()
                true
            }
            
            // Generic types
            char == '~' -> {
                state = LexerState.GENERIC
                advance()
                true
            }
            
            // Keywords and identifiers
            char.isLetter() || char == '_' -> scanIdentifierOrKeyword()
            
            // Numbers
            char.isDigit() -> scanNumber()
            
            // Operators and punctuation
            else -> scanOperatorOrPunctuation()
        }
    }
    
    private fun scanStringState(): Boolean {
        val start = position
        
        while (position < input.length && current() != '"') {
            if (current() == '\\') {
                advance() // Skip escape character
                if (position < input.length) advance() // Skip escaped character
            } else {
                advance()
            }
        }
        
        if (position < input.length && current() == '"') {
            val value = input.substring(start, position)
            addToken(TokenType.STR, value)
            advance() // Skip closing quote
            state = LexerState.INITIAL
            return true
        }
        
        return false
    }
    
    private fun scanBackquoteStringState(): Boolean {
        val start = position
        
        while (position < input.length && current() != '`') {
            advance()
        }
        
        if (position < input.length && current() == '`') {
            val value = input.substring(start, position)
            addToken(TokenType.BQUOTE_STR, value)
            advance() // Skip closing backtick
            state = LexerState.INITIAL
            return true
        }
        
        return false
    }
    
    private fun scanGenericState(): Boolean {
        val start = position
        
        while (position < input.length && current() != '~') {
            advance()
        }
        
        if (position < input.length && current() == '~') {
            val value = input.substring(start, position)
            addToken(TokenType.GENERICTYPE, value)
            advance() // Skip closing tilde
            state = LexerState.INITIAL
            return true
        }
        
        return false
    }
    
    private fun scanClassState(): Boolean {
        skipWhitespace()
        
        if (position >= input.length) return false
        
        val char = current()
        
        return when {
            char == '\n' || char == '\r' -> {
                scanNewline()
                state = LexerState.INITIAL
                true
            }
            
            char == '{' -> {
                addToken(TokenType.STRUCT_START, "{")
                advance()
                state = LexerState.CLASS_BODY
                true
            }
            
            char == '}' -> {
                addToken(TokenType.STRUCT_STOP, "}")
                advance()
                state = LexerState.INITIAL
                true
            }
            
            char.isLetter() || char == '_' -> scanIdentifierOrKeyword()
            
            else -> scanOperatorOrPunctuation()
        }
    }
    
    private fun scanClassBodyState(): Boolean {
        skipWhitespace()
        
        if (position >= input.length) {
            addToken(TokenType.EOF, "")
            return true
        }
        
        val char = current()
        
        return when {
            char == '}' -> {
                addToken(TokenType.STRUCT_STOP, "}")
                advance()
                state = LexerState.INITIAL
                true
            }
            
            char == '\n' -> {
                advance()
                true // Skip newlines in class body
            }
            
            char == '{' -> {
                addToken(TokenType.STRUCT_START, "{")
                advance()
                true
            }
            
            else -> {
                // Scan member
                val start = position
                while (position < input.length && current() != '\n' && current() != '}' && current() != '{') {
                    advance()
                }
                
                if (position > start) {
                    val value = input.substring(start, position).trim()
                    if (value.isNotEmpty()) {
                        addToken(TokenType.MEMBER, value)
                    }
                }
                true
            }
        }
    }
    
    private fun scanNamespaceState(): Boolean {
        // Similar to class state but for namespace
        return scanClassState()
    }
    
    private fun scanNamespaceBodyState(): Boolean {
        // Similar to class body state but for namespace
        return scanClassBodyState()
    }
    
    private fun scanCallbackNameState(): Boolean {
        // Scan callback name
        val start = position
        while (position < input.length && current() != '(' && !current().isWhitespace()) {
            advance()
        }
        
        if (position > start) {
            val value = input.substring(start, position)
            addToken(TokenType.CALLBACK_NAME, value)
        }
        
        state = LexerState.INITIAL
        return true
    }
    
    private fun scanCallbackArgsState(): Boolean {
        // Scan callback arguments
        val start = position
        while (position < input.length && current() != ')') {
            advance()
        }
        
        if (position > start) {
            val value = input.substring(start, position)
            addToken(TokenType.CALLBACK_ARGS, value)
        }
        
        if (position < input.length && current() == ')') {
            advance()
        }
        
        state = LexerState.INITIAL
        return true
    }
    
    private fun scanAccTitleState(): Boolean {
        // Scan accessibility title
        val start = position
        while (position < input.length && current() != '\n' && current() != ';' && current() != '#') {
            advance()
        }
        
        if (position > start) {
            val value = input.substring(start, position).trim()
            addToken(TokenType.ACC_TITLE_VALUE, value)
        }
        
        state = LexerState.INITIAL
        return true
    }
    
    private fun scanAccDescrState(): Boolean {
        // Scan accessibility description
        val start = position
        while (position < input.length && current() != '\n' && current() != ';' && current() != '#') {
            advance()
        }
        
        if (position > start) {
            val value = input.substring(start, position).trim()
            addToken(TokenType.ACC_DESCR_VALUE, value)
        }
        
        state = LexerState.INITIAL
        return true
    }
    
    private fun scanAccDescrMultilineState(): Boolean {
        // Scan multiline accessibility description
        val start = position
        while (position < input.length && current() != '}') {
            advance()
        }
        
        if (position > start) {
            val value = input.substring(start, position)
            addToken(TokenType.ACC_DESCR_MULTILINE_VALUE, value)
        }
        
        if (position < input.length && current() == '}') {
            advance()
        }
        
        state = LexerState.INITIAL
        return true
    }
    
    private fun scanIdentifierOrKeyword(): Boolean {
        val start = position
        
        while (position < input.length && (current().isLetterOrDigit() || current() == '_' || current() == '-')) {
            advance()
        }
        
        val value = input.substring(start, position)
        val tokenType = getKeywordType(value) ?: TokenType.ALPHA
        
        addToken(tokenType, value)
        
        // Handle state transitions for certain keywords
        when (tokenType) {
            TokenType.CLASS -> state = LexerState.CLASS
            TokenType.NAMESPACE -> state = LexerState.NAMESPACE
            TokenType.ACC_TITLE -> state = LexerState.ACC_TITLE
            TokenType.ACC_DESCR -> state = LexerState.ACC_DESCR
            else -> {}
        }
        
        return true
    }
    
    private fun scanNumber(): Boolean {
        val start = position
        
        while (position < input.length && current().isDigit()) {
            advance()
        }
        
        val value = input.substring(start, position)
        addToken(TokenType.NUM, value)
        return true
    }
    
    private fun scanNewline(): Boolean {
        if (current() == '\r' && peek() == '\n') {
            advance()
            advance()
        } else {
            advance()
        }
        
        addToken(TokenType.NEWLINE, "\\n")
        return true
    }
    
    private fun scanOperatorOrPunctuation(): Boolean {
        val char = current()
        
        return when {
            // Multi-character operators
            char == '<' && peek() == '|' && peek(2) == '-' && peek(3) == '-' -> {
                addToken(TokenType.EXTENSION, "<|--")
                advance(4)
                true
            }
            
            char == '-' && peek() == '-' && peek(2) == '>' -> {
                addToken(TokenType.LINE, "-->")
                advance(3)
                true
            }
            
            char == '-' && peek() == '-' -> {
                addToken(TokenType.LINE, "--")
                advance(2)
                true
            }
            
            char == '.' && peek() == '.' -> {
                addToken(TokenType.DOTTED_LINE, "..")
                advance(2)
                true
            }
            
            char == '<' && peek() == '<' -> {
                addToken(TokenType.ANNOTATION_START, "<<")
                advance(2)
                true
            }
            
            char == '>' && peek() == '>' -> {
                addToken(TokenType.ANNOTATION_END, ">>")
                advance(2)
                true
            }
            
            char == ':' && peek() == ':' && peek(2) == ':' -> {
                addToken(TokenType.STYLE_SEPARATOR, ":::")
                advance(3)
                true
            }
            
            char == '<' && peek() == '|' -> {
                addToken(TokenType.EXTENSION, "<|")
                advance(2)
                true
            }
            
            char == '|' && peek() == '>' -> {
                addToken(TokenType.EXTENSION, "|>")
                advance(2)
                true
            }
            
            char == '(' && peek() == ')' -> {
                addToken(TokenType.LOLLIPOP, "()")
                advance(2)
                true
            }
            
            // Single character operators
            char == '{' -> {
                addToken(TokenType.STRUCT_START, "{")
                advance()
                true
            }
            
            char == '}' -> {
                addToken(TokenType.STRUCT_STOP, "}")
                advance()
                true
            }
            
            char == '[' -> {
                addToken(TokenType.SQS, "[")
                advance()
                true
            }
            
            char == ']' -> {
                addToken(TokenType.SQE, "]")
                advance()
                true
            }
            
            char == ':' -> {
                // Check if this is a member definition (: followed by +/- or method)
                val start = position + 1
                var end = start
                while (end < input.length && input[end] != '\n' && input[end] != ';') {
                    end++
                }

                if (end > start) {
                    val content = input.substring(start, end).trim()
                    if (content.isNotEmpty()) {
                        // Check if this looks like a member definition
                        if (content.startsWith('+') || content.startsWith('-') || content.startsWith('#') || content.startsWith('~') || content.contains('(')) {
                            // This is a member definition, generate COLON + MEMBER
                            addToken(TokenType.COLON, ":")
                            advance()
                            // Skip whitespace after colon
                            skipWhitespace()
                            // Scan the member
                            val memberStart = position
                            position = end
                            val memberText = input.substring(memberStart, end).trim()
                            if (memberText.isNotEmpty()) {
                                addToken(TokenType.MEMBER, memberText)
                            }
                            return true
                        } else {
                            // Check if this is a class annotation (single word after colon)
                            val words = content.split(Regex("\\s+"))
                            if (words.size == 1 && words[0].matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) {
                                // This looks like a class annotation (ClassName : AnnotationType)
                                addToken(TokenType.COLON, ":")
                                advance()
                                return true
                            } else {
                                // This is a regular label
                                addToken(TokenType.LABEL, ":$content")
                                position = end
                                return true
                            }
                        }
                    }
                }

                addToken(TokenType.COLON, ":")
                advance()
                true
            }
            
            char == ',' -> {
                addToken(TokenType.COMMA, ",")
                advance()
                true
            }
            
            char == '.' -> {
                addToken(TokenType.DOT, ".")
                advance()
                true
            }
            
            char == '+' -> {
                addToken(TokenType.PLUS, "+")
                advance()
                true
            }
            
            char == '-' -> {
                addToken(TokenType.MINUS, "-")
                advance()
                true
            }
            
            char == '#' -> {
                addToken(TokenType.BRKT, "#")
                advance()
                true
            }
            
            char == '%' -> {
                addToken(TokenType.PCT, "%")
                advance()
                true
            }
            
            char == '=' -> {
                addToken(TokenType.EQUALS, "=")
                advance()
                true
            }
            
            char == '*' -> {
                addToken(TokenType.COMPOSITION, "*")
                advance()
                true
            }
            
            char == 'o' && !peek().isLetterOrDigit() -> {
                addToken(TokenType.AGGREGATION, "o")
                advance()
                true
            }
            
            char == '<' -> {
                addToken(TokenType.DEPENDENCY, "<")
                advance()
                true
            }
            
            char == '>' -> {
                addToken(TokenType.DEPENDENCY, ">")
                advance()
                true
            }
            
            char.isPunctuation() -> {
                addToken(TokenType.PUNCTUATION, char.toString())
                advance()
                true
            }
            
            else -> false
        }
    }
    
    private fun getKeywordType(value: String): TokenType? {
        return when (value.lowercase()) {
            "classdiagram", "classdiagram-v2" -> TokenType.CLASS_DIAGRAM
            "class" -> TokenType.CLASS
            "namespace" -> TokenType.NAMESPACE
            "note" -> TokenType.NOTE
            "style" -> TokenType.STYLE
            "classdef" -> TokenType.CLASSDEF
            "callback" -> TokenType.CALLBACK
            "link" -> TokenType.LINK
            "click" -> TokenType.CLICK
            "href" -> TokenType.HREF
            "cssclass" -> TokenType.CSSCLASS
            "acctitle" -> TokenType.ACC_TITLE
            "accdescr" -> TokenType.ACC_DESCR
            "_self", "_blank", "_parent", "_top" -> TokenType.LINK_TARGET
            else -> {
                // Check for direction keywords
                when {
                    value.contains("direction") && value.contains("TB") -> TokenType.DIRECTION_TB
                    value.contains("direction") && value.contains("BT") -> TokenType.DIRECTION_BT
                    value.contains("direction") && value.contains("RL") -> TokenType.DIRECTION_RL
                    value.contains("direction") && value.contains("LR") -> TokenType.DIRECTION_LR
                    value == "note for" -> TokenType.NOTE_FOR
                    else -> null
                }
            }
        }
    }
    
    private fun skipWhitespace() {
        while (position < input.length && current().isWhitespace() && current() != '\n' && current() != '\r') {
            advance()
        }
    }
    
    private fun skipComment() {
        // Skip %% comments
        while (position < input.length && current() != '\n') {
            advance()
        }
    }
    
    private fun current(): Char = if (position < input.length) input[position] else '\u0000'
    
    private fun peek(offset: Int = 1): Char = 
        if (position + offset < input.length) input[position + offset] else '\u0000'
    
    private fun advance(count: Int = 1) {
        repeat(count) {
            if (position < input.length) {
                if (input[position] == '\n') {
                    line++
                    column = 1
                } else {
                    column++
                }
                position++
            }
        }
    }
    
    private fun addToken(type: TokenType, value: String) {
        tokens.add(MermaidToken(type, value, line, column))
    }
    
    private fun Char.isPunctuation(): Boolean {
        return this in "!\"#$%&'*+,-.`?\\/"
    }
    
    /**
     * Lexer states for context-sensitive parsing
     */
    private enum class LexerState {
        INITIAL,
        STRING,
        BQSTRING,
        GENERIC,
        CLASS,
        CLASS_BODY,
        NAMESPACE,
        NAMESPACE_BODY,
        CALLBACK_NAME,
        CALLBACK_ARGS,
        ACC_TITLE,
        ACC_DESCR,
        ACC_DESCR_MULTILINE
    }
}
