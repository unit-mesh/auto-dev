package cc.unitmesh.diagram.graphviz.parser.mermaid

/**
 * Represents a token in Mermaid class diagram syntax
 * Based on the tokens defined in classDiagram.jison
 */
data class MermaidToken(
    val type: TokenType,
    val value: String,
    val line: Int,
    val column: Int
) {
    override fun toString(): String = "${type.name}('$value')"
}

/**
 * Token types based on Mermaid classDiagram.jison lexical rules
 */
enum class TokenType {
    // Keywords
    CLASS_DIAGRAM,      // classDiagram, classDiagram-v2
    CLASS,              // class
    NAMESPACE,          // namespace
    NOTE,               // note
    NOTE_FOR,           // note for
    STYLE,              // style
    CLASSDEF,           // classDef
    CALLBACK,           // callback
    LINK,               // link
    CLICK,              // click
    HREF,               // href
    CSSCLASS,           // cssClass
    
    // Directions
    DIRECTION_TB,       // direction TB
    DIRECTION_BT,       // direction BT
    DIRECTION_RL,       // direction RL
    DIRECTION_LR,       // direction LR
    
    // Structural tokens
    STRUCT_START,       // {
    STRUCT_STOP,        // }
    SQS,                // [
    SQE,                // ]
    COLON,              // :
    COMMA,              // ,
    DOT,                // .
    
    // Relation types
    EXTENSION,          // <|, |>
    DEPENDENCY,         // <, >
    COMPOSITION,        // *
    AGGREGATION,        // o
    LOLLIPOP,           // ()
    
    // Line types
    LINE,               // --
    DOTTED_LINE,        // ..
    
    // Visibility modifiers
    PLUS,               // +
    MINUS,              // -
    BRKT,               // #
    PCT,                // %
    
    // Labels and strings
    LABEL,              // :text
    STR,                // "string"
    BQUOTE_STR,         // `string`
    
    // Annotations
    ANNOTATION_START,   // <<
    ANNOTATION_END,     // >>
    
    // Style separators
    STYLE_SEPARATOR,    // :::
    
    // Generic types
    GENERICTYPE,        // ~type~
    
    // Basic tokens
    ALPHA,              // alphanumeric
    NUM,                // numbers
    UNICODE_TEXT,       // unicode text
    SPACE,              // whitespace
    NEWLINE,            // newline
    PUNCTUATION,        // punctuation
    EQUALS,             // =
    
    // Special tokens
    EDGE_STATE,         // [*]
    LINK_TARGET,        // _self, _blank, _parent, _top
    CALLBACK_NAME,      // callback name
    CALLBACK_ARGS,      // callback arguments
    
    // Accessibility
    ACC_TITLE,          // accTitle
    ACC_DESCR,          // accDescr
    ACC_TITLE_VALUE,    // title value
    ACC_DESCR_VALUE,    // description value
    ACC_DESCR_MULTILINE_VALUE, // multiline description
    
    // Special
    MEMBER,             // class member
    SEPARATOR,          // separator
    EOF,                // end of file
    UNKNOWN             // unknown token
}

/**
 * Represents the position of a token in the source text
 */
data class TokenPosition(
    val line: Int,
    val column: Int,
    val offset: Int
)
