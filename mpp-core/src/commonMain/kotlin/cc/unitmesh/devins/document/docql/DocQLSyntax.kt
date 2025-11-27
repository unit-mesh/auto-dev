package cc.unitmesh.devins.document.docql

/**
 * DocQL - A JSONPath-like query language for documents
 * 
 * Syntax Examples:
 * - `$.toc[*]`                     - All TOC items
 * - `$.toc[0]`                     - First TOC item
 * - `$.toc[?(@.level==1)]`         - TOC items with level 1
 * - `$.toc[?(@.level!=2)]`         - TOC items where level is not 2
 * - `$.toc[?(@.title~="架构")]`     - TOC items where title contains "架构"
 * - `$.toc[?(@.title =~ /MCP/i)]`  - TOC items where title matches regex "MCP" (case-insensitive)
 * - `$.toc[?(@.title =~ /^Chapter\d+/)]` - Regex with pattern anchors
 * - `$.toc[?(@.title startsWith "Ch")]` - Title starts with "Ch"
 * - `$.toc[?(@.title endsWith "ion")]`  - Title ends with "ion"
 * - `$.entities[*]`                - All entities
 * - `$.entities[?(@.type=='Term')]` - Term entities (single quotes supported)
 * - `$.entities[?(@.name =~ /API/)]` - Entities where name matches regex
 * - `$.content.heading("架构")`     - Content with heading matching "架构"
 * - `$.content.chapter("1.2")`     - Chapter 1.2 content
 * - `$.content.h1("介绍")`         - H1 heading matching "介绍"
 * - `$.content.h2("设计")`         - H2 heading matching "设计"
 * - `$.content.codeblock[*]`       - All code blocks from markdown
 * - `$.content.codeblock[?(@.language=="kotlin")]` - Code blocks by language
 * - `$.content.table[*]`           - All tables
 * - `$.content.grep("关键词")`      - Grep search for "关键词"
 * 
 * File Structure Queries:
 * - `$.files[*]`                   - List all registered files
 * - `$.files[?(@.extension=="kt")]` - Files by extension
 * - `$.files[?(@.path~="src/")]`   - Files containing path pattern
 * - `$.structure`                  - Get file structure as tree
 * - `$.structure.tree()`           - Get formatted tree structure
 * - `$.structure.flat()`           - Get flat list of all paths
 * 
 * Filter Operators:
 * - `==`  Equality: `@.property == "value"` or `@.property == 'value'`
 * - `!=`  Not equals: `@.property != "value"`
 * - `~=`  Contains: `@.property ~= "value"`
 * - `=~`  Regex match (JSONPath style): `@.property =~ /pattern/flags`
 * - `>`   Greater than: `@.property > number`
 * - `>=`  Greater than or equals: `@.property >= number`
 * - `<`   Less than: `@.property < number`
 * - `<=`  Less than or equals: `@.property <= number`
 * - `startsWith` / `starts with`: `@.property startsWith "prefix"`
 * - `endsWith` / `ends with`: `@.property endsWith "suffix"`
 * 
 * String Literals:
 * - Double quotes: `"value"`
 * - Single quotes: `'value'`
 * 
 * Regex Flags (for =~ operator):
 * - `i`  Case-insensitive matching
 * - `m`  Multiline mode
 * - `s`  Dotall mode (. matches newlines)
 */

/**
 * DocQL AST (Abstract Syntax Tree)
 */
sealed class DocQLNode {
    /**
     * Root node: $
     */
    object Root : DocQLNode()
    
    /**
     * Property access: .property
     */
    data class Property(val name: String) : DocQLNode()
    
    /**
     * Array access: [n] or [*]
     */
    sealed class ArrayAccess : DocQLNode() {
        data class Index(val index: Int) : ArrayAccess()
        object All : ArrayAccess()
        data class Filter(val condition: FilterCondition) : ArrayAccess()
    }
    
    /**
     * Function call: .function("arg")
     */
    /**
     * Function call node (e.g., .heading("Introduction") or .chunks())
     */
    data class FunctionCall(val name: String, val argument: String = "") : DocQLNode()
}

/**
 * Filter conditions for array filtering
 */
sealed class FilterCondition {
    /**
     * Equality: @.property == "value"
     */
    data class Equals(val property: String, val value: String) : FilterCondition()
    
    /**
     * Not equals: @.property != "value"
     */
    data class NotEquals(val property: String, val value: String) : FilterCondition()
    
    /**
     * Contains: @.property ~= "value"
     */
    data class Contains(val property: String, val value: String) : FilterCondition()

    /**
     * Regex match: @.property =~ /pattern/flags (JSONPath-style)
     * Flags can include: i (case-insensitive), m (multiline), s (dotall)
     */
    data class RegexMatch(val property: String, val pattern: String, val flags: String = "") : FilterCondition()

    /**
     * Greater than: @.property > value
     */
    data class GreaterThan(val property: String, val value: Int) : FilterCondition()
    
    /**
     * Greater than or equals: @.property >= value
     */
    data class GreaterThanOrEquals(val property: String, val value: Int) : FilterCondition()
    
    /**
     * Less than: @.property < value
     */
    data class LessThan(val property: String, val value: Int) : FilterCondition()
    
    /**
     * Less than or equals: @.property <= value
     */
    data class LessThanOrEquals(val property: String, val value: Int) : FilterCondition()
    
    /**
     * Starts with: @.property starts with "value" or @.property startsWith "value"
     */
    data class StartsWith(val property: String, val value: String) : FilterCondition()
    
    /**
     * Ends with: @.property ends with "value" or @.property endsWith "value"
     */
    data class EndsWith(val property: String, val value: String) : FilterCondition()
}

/**
 * Parsed DocQL query
 */
data class DocQLQuery(
    val nodes: List<DocQLNode>
) {
    override fun toString(): String {
        return nodes.joinToString("") { node ->
            when (node) {
                is DocQLNode.Root -> "$"
                is DocQLNode.Property -> ".${node.name}"
                is DocQLNode.ArrayAccess.All -> "[*]"
                is DocQLNode.ArrayAccess.Index -> "[${node.index}]"
                is DocQLNode.ArrayAccess.Filter -> "[?(${node.condition})]"
                is DocQLNode.FunctionCall -> ".${node.name}(\"${node.argument}\")"
            }
        }
    }
}

/**
 * Token types for lexical analysis
 */
sealed class DocQLToken {
    object Root : DocQLToken()                              // $
    object Dot : DocQLToken()                               // .
    object LeftBracket : DocQLToken()                       // [
    object RightBracket : DocQLToken()                      // ]
    object LeftParen : DocQLToken()                         // (
    object RightParen : DocQLToken()                        // )
    object Star : DocQLToken()                              // *
    object Question : DocQLToken()                          // ?
    object At : DocQLToken()                                // @
    object Equals : DocQLToken()                            // ==
    object NotEquals : DocQLToken()                         // !=
    object Contains : DocQLToken()                          // ~=
    object RegexMatch : DocQLToken()                        // =~
    object GreaterThan : DocQLToken()                       // >
    object GreaterThanOrEquals : DocQLToken()               // >=
    object LessThan : DocQLToken()                          // <
    object LessThanOrEquals : DocQLToken()                  // <=
    object StartsWith : DocQLToken()                        // starts with / startsWith
    object EndsWith : DocQLToken()                          // ends with / endsWith
    data class Identifier(val value: String) : DocQLToken() // property names, function names
    data class StringLiteral(val value: String) : DocQLToken() // "string" or 'string'
    data class RegexLiteral(val pattern: String, val flags: String) : DocQLToken() // /pattern/flags
    data class Number(val value: Int) : DocQLToken()        // 123
    object EOF : DocQLToken()
}

