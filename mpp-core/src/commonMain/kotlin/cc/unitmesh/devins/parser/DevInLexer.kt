package cc.unitmesh.devins.parser

/**
 * DevIn Language Lexer for multiplatform support
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
 * DevIn Token interface
 */
interface DevInToken {
    val type: String
    val value: String
    val text: String
    val offset: Int
    val lineBreaks: Int
    val line: Int
    val col: Int
}

/**
 * DevIn Lexer States
 */
object DevInLexerStates {
    const val INITIAL = "INITIAL"
    const val AGENT_BLOCK = "AGENT_BLOCK"
    const val VARIABLE_BLOCK = "VARIABLE_BLOCK"
    const val COMMAND_BLOCK = "COMMAND_BLOCK"
    const val COMMAND_VALUE_BLOCK = "COMMAND_VALUE_BLOCK"
    const val EXPR_BLOCK = "EXPR_BLOCK"
    const val CODE_BLOCK = "CODE_BLOCK"
    const val FRONT_MATTER_BLOCK = "FRONT_MATTER_BLOCK"
    const val FRONT_MATTER_VALUE_BLOCK = "FRONT_MATTER_VALUE_BLOCK"
    const val LANG_ID = "LANG_ID"
}

/**
 * DevIn Token Types
 */
object DevInTokenTypes {
    // Basic tokens
    const val TEXT_SEGMENT = "TEXT_SEGMENT"
    const val NEWLINE = "NEWLINE"
    const val WHITE_SPACE = "WHITE_SPACE"
    const val IDENTIFIER = "IDENTIFIER"
    const val NUMBER = "NUMBER"
    const val BOOLEAN = "BOOLEAN"
    const val QUOTE_STRING = "QUOTE_STRING"
    
    // Special markers
    const val AGENT_START = "AGENT_START"           // @
    const val COMMAND_START = "COMMAND_START"       // /
    const val VARIABLE_START = "VARIABLE_START"     // $
    const val SHARP = "SHARP"                       // #
    
    // Code blocks
    const val CODE_BLOCK_START = "CODE_BLOCK_START" // ```
    const val CODE_BLOCK_END = "CODE_BLOCK_END"     // ```
    const val CODE_CONTENT = "CODE_CONTENT"
    const val LANGUAGE_ID = "LANGUAGE_ID"
    
    // Front matter
    const val FRONTMATTER_START = "FRONTMATTER_START" // ---
    const val FRONTMATTER_END = "FRONTMATTER_END"     // ---
    
    // Operators and punctuation
    const val COLON = "COLON"                       // :
    const val COMMA = "COMMA"                       // ,
    const val LPAREN = "LPAREN"                     // (
    const val RPAREN = "RPAREN"                     // )
    const val LBRACKET = "LBRACKET"                 // [
    const val RBRACKET = "RBRACKET"                 // ]
    const val OPEN_BRACE = "OPEN_BRACE"             // {
    const val CLOSE_BRACE = "CLOSE_BRACE"           // }
    const val PIPE = "PIPE"                         // |
    const val DOT = "DOT"                           // .
    const val ARROW = "ARROW"                       // =>
    const val ACCESS = "ACCESS"                     // ::
    const val PROCESS = "PROCESS"                   // ->
    
    // Comparison operators
    const val EQEQ = "EQEQ"                         // ==
    const val NEQ = "NEQ"                           // !=
    const val LT = "LT"                             // <
    const val GT = "GT"                             // >
    const val LTE = "LTE"                           // <=
    const val GTE = "GTE"                           // >=
    const val ANDAND = "ANDAND"                     // &&
    const val OROR = "OROR"                         // ||
    const val NOT = "NOT"                           // !
    
    // Keywords
    const val IF = "IF"
    const val ELSE = "ELSE"
    const val ELSEIF = "ELSEIF"
    const val END = "END"
    const val ENDIF = "ENDIF"
    const val CASE = "CASE"
    const val DEFAULT = "DEFAULT"
    const val WHEN = "WHEN"
    const val FROM = "FROM"
    const val WHERE = "WHERE"
    const val SELECT = "SELECT"
    const val CONDITION = "CONDITION"
    const val FUNCTIONS = "FUNCTIONS"
    
    // Lifecycle keywords
    const val ON_STREAMING = "ON_STREAMING"
    const val BEFORE_STREAMING = "BEFORE_STREAMING"
    const val ON_STREAMING_END = "ON_STREAMING_END"
    const val AFTER_STREAMING = "AFTER_STREAMING"
    
    // Comments
    const val COMMENTS = "COMMENTS"
    const val BLOCK_COMMENT = "BLOCK_COMMENT"
    const val CONTENT_COMMENTS = "CONTENT_COMMENTS"
    
    // Command properties
    const val COMMAND_PROP = "COMMAND_PROP"
    const val LINE_INFO = "LINE_INFO"
    
    // Pattern and regex
    const val PATTERN_EXPR = "PATTERN_EXPR"
    const val REGEX = "REGEX"
    
    // Error token
    const val ERROR = "ERROR"
    const val EOF = "EOF"
}

/**
 * DevIn Lexer interface
 */
interface DevInLexer {
    /**
     * Reset the lexer with new input
     */
    fun reset(input: String)
    
    /**
     * Get the next token
     */
    fun next(): DevInToken?
    
    /**
     * Check if there are more tokens
     */
    fun hasNext(): Boolean
    
    /**
     * Get current lexer state
     */
    fun getCurrentState(): String
    
    /**
     * Set lexer state
     */
    fun setState(state: String)
    
    /**
     * Get all tokens as a list
     */
    fun tokenize(input: String): List<DevInToken>
}

/**
 * Expected DevIn Lexer implementation
 */
expect class DevInLexerImpl() : DevInLexer {
    override fun reset(input: String)
    override fun next(): DevInToken?
    override fun hasNext(): Boolean
    override fun getCurrentState(): String
    override fun setState(state: String)
    override fun tokenize(input: String): List<DevInToken>
}
