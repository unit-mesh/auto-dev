package cc.unitmesh.codegraph.parser.wasm

import kotlin.js.Promise

/**
 * Parser module object for accessing static methods
 */
@JsModule("web-tree-sitter")
external object ParserModule : JsAny {
    fun init(): Promise<JsAny>
}

/**
 * Parser constructor function
 */
@JsModule("web-tree-sitter")
external fun Parser(): Parser

/**
 * Parser instance interface
 */
external interface Parser : JsAny {
    fun parse(input: String): Tree
    fun setLanguage(language: TSLanguageGrammar?)
    fun getLanguage(): TSLanguageGrammar?
}

/**
 * Language module object for accessing static load method
 */
@JsModule("web-tree-sitter")
@JsName("Parser.Language")
external object LanguageModule : JsAny {
    fun load(path: String): Promise<TSLanguageGrammar>
}

/**
 * Language grammar interface (renamed to avoid conflict with cc.unitmesh.codegraph.parser.Language)
 */
external interface TSLanguageGrammar : JsAny

/**
 * Parse tree interface
 */
external interface Tree : JsAny {
    val rootNode: Node
    fun edit(edit: Edit)
    fun walk(): TreeCursor
    fun copy(): Tree
    fun delete()
}

/**
 * Syntax tree node interface
 */
external interface Node : JsAny {
    val type: String
    val typeId: Int
    val isNamed: Boolean
    val text: String
    val startPosition: Point
    val endPosition: Point
    val startIndex: Int
    val endIndex: Int
    val parent: Node?
    val childCount: Int
    val namedChildCount: Int
    val firstChild: Node?
    val firstNamedChild: Node?
    val lastChild: Node?
    val lastNamedChild: Node?
    val nextSibling: Node?
    val nextNamedSibling: Node?
    val previousSibling: Node?
    val previousNamedSibling: Node?

    fun child(index: Int): Node?
    fun namedChild(index: Int): Node?
    fun childForFieldName(fieldName: String): Node?
    fun childForFieldId(fieldId: Int): Node?
    fun descendantForIndex(index: Int): Node?
    fun descendantForPosition(position: Point): Node?
    fun walk(): TreeCursor
}

/**
 * Position in source code
 */
external interface Point : JsAny {
    val row: Int
    val column: Int
}

/**
 * Edit operation interface
 */
external interface Edit : JsAny {
    val startIndex: Int
    val oldEndIndex: Int
    val newEndIndex: Int
    val startPosition: Point
    val oldEndPosition: Point
    val newEndPosition: Point
}

/**
 * Tree cursor for traversing syntax tree
 */
external interface TreeCursor : JsAny {
    val nodeType: String
    val nodeTypeId: Int
    val nodeText: String
    val nodeIsNamed: Boolean
    val startPosition: Point
    val endPosition: Point
    val startIndex: Int
    val endIndex: Int

    fun reset(node: Node)
    fun gotoParent(): Boolean
    fun gotoFirstChild(): Boolean
    fun gotoFirstChildForIndex(index: Int): Boolean
    fun gotoNextSibling(): Boolean
    fun currentNode(): Node
}

// Console for debugging
external object console : JsAny {
    fun log(message: String)
    fun error(message: String)
    fun warn(message: String)
}

// Type aliases for cleaner code
typealias TSParser = Parser
typealias TSLanguage = TSLanguageGrammar
typealias TSTree = Tree
typealias TSNode = Node
typealias TSPoint = Point

