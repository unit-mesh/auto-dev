package cc.unitmesh.codegraph.parser.wasm

import kotlin.js.Promise

/**
 * External interfaces for web-tree-sitter API.
 *
 * These interfaces match the TypeScript definitions from:
 * https://github.com/tree-sitter/tree-sitter/blob/master/lib/binding_web/tree-sitter-web.d.ts
 *
 * Note: In Kotlin/Wasm, all external interfaces must extend JsAny,
 * and we cannot use @JsNonModule or companion objects in external interfaces.
 */

/**
 * TreeSitter module functions
 */
@JsModule("web-tree-sitter")
external fun init(): Promise<JsAny>

@JsModule("web-tree-sitter")
external fun loadLanguage(path: String): Promise<TSLanguageGrammar>

/**
 * Parser interface for tree-sitter
 */
@JsModule("web-tree-sitter")
external class Parser : JsAny {
    fun parse(input: String): Tree
    fun setLanguage(language: TSLanguageGrammar)
    fun getLanguage(): TSLanguageGrammar?
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

// Type aliases for cleaner code
typealias TSParser = Parser
typealias TSLanguage = TSLanguageGrammar
typealias TSTree = Tree
typealias TSNode = Node
typealias TSPoint = Point

