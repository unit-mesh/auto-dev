package cc.unitmesh.codegraph.parser.wasm

import kotlin.js.Promise

/**
 * External declarations for web-tree-sitter API using @JsFun for dynamic imports.
 *
 * This approach is more reliable in WASM test environments than @JsModule.
 *
 * Reference JS implementation:
 * ```js
 * const TreeSitter = await import('web-tree-sitter');
 * await TreeSitter.default.init();
 * const parser = new TreeSitter.default();
 * const language = await TreeSitter.default.Language.load(wasmPath);
 * parser.setLanguage(language);
 * const tree = parser.parse(sourceCode);
 * ```
 */

/**
 * Initialize TreeSitter and return the TreeSitter module
 *
 * JavaScript: `const ts = await import('web-tree-sitter'); await ts.default.init(); return ts.default;`
 */
@JsFun("async () => { const ts = await import('web-tree-sitter'); await ts.default.init(); return ts.default; }")
external fun initTreeSitter(): Promise<TreeSitterModule>

/**
 * TreeSitter module interface (the default export from web-tree-sitter)
 */
external interface TreeSitterModule : JsAny {
    /**
     * Language class for loading grammar WASM files
     */
    val Language: LanguageClass
}

/**
 * Language class with static load method
 */
external interface LanguageClass : JsAny {
    /**
     * Load a language grammar from a WASM file path
     *
     * JavaScript: `TreeSitter.Language.load(path)`
     */
    fun load(path: String): Promise<TSLanguageGrammar>
    
    /**
     * Load a language grammar from binary data (Uint8Array)
     *
     * JavaScript: `TreeSitter.Language.load(bits)`
     */
    fun load(bits: JsAny): Promise<TSLanguageGrammar>
}

/**
 * Initialize Parser and load language from WASM file
 * 
 * This implementation works in both Node.js and browser environments:
 * - In Node.js: uses fs.readFileSync to read the file as buffer
 * - In browser/WASM: uses fetch to load the file as ArrayBuffer
 * 
 * JavaScript equivalent to the TypeScript getLanguage method
 */
@JsFun("""async (wasmPath) => {
    const Parser = await import('web-tree-sitter');
    await Parser.default.init();
    
    let bits;
    // Check if we're in Node.js environment
    if (typeof process !== 'undefined' && process.versions && process.versions.node) {
        // Node.js environment - use fs
        const fs = await import('fs');
        bits = fs.readFileSync(wasmPath);
    } else {
        // Browser/WASM environment - use fetch
        const response = await fetch(wasmPath);
        bits = await response.arrayBuffer();
    }
    
    return await Parser.default.Language.load(bits);
}
""")
external fun loadLanguageFromWasm(wasmPath: String): Promise<TSLanguageGrammar>

/**
 * Create a new Parser instance
 *
 * JavaScript: `const ts = await import('web-tree-sitter'); await ts.default.init(); return new ts.default();`
 */
@JsFun("""async () => { 
    const ts = await import('web-tree-sitter');
    await ts.default.init(); 
    return new ts.default(); 
}
"""
)
external fun createParser(): Promise<Parser>

/**
 * Parser instance interface
 */
external interface Parser : JsAny {
    fun parse(input: String): Tree
    fun setLanguage(language: TSLanguageGrammar?)
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

