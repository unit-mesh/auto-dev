package cc.unitmesh.codegraph.parser.wasm

import kotlin.js.Promise

/**
 * Represents a position in a text document, expressed as a zero-based line and column
 */
external interface Point : JsAny {
    /** The zero-based line number */
    val row: Int

    /** The zero-based character offset on the line */
    val column: Int
}

/**
 * Represents a range in a text document, expressed by its start and end positions
 */
external interface Range : JsAny {
    /** The start byte index of the range */
    val startIndex: Int

    /** The end byte index of the range */
    val endIndex: Int

    /** The start position of the range */
    val startPosition: Point

    /** The end position of the range */
    val endPosition: Point
}

/**
 * Represents an edit operation that can be applied to a syntax tree
 */
external interface Edit : JsAny {
    /** The start byte index of the edit */
    val startIndex: Int

    /** The previous end byte index of the edit */
    val oldEndIndex: Int

    /** The new end byte index of the edit */
    val newEndIndex: Int

    /** The start position of the edit */
    val startPosition: Point

    /** The previous end position of the edit */
    val oldEndPosition: Point

    /** The new end position of the edit */
    val newEndPosition: Point
}

// Logger
typealias Logger = (message: String, params: JsAny, type: String) -> Unit

// Input
typealias Input = (index: Int, position: Point?) -> String?

/**
 * Options for parsing
 *
 * The `includedRanges` property is an array of {@link Range} objects that
 * represent the ranges of text that the parser should include when parsing.
 *
 * See {@link Parser#parse} for more information.
 */
external interface Options : JsAny {
    /**
     * An array of {@link Range} objects that
     * represent the ranges of text that the parser should include when parsing.
     *
     * This sets the ranges of text that the parser should include when parsing.
     * By default, the parser will always include entire documents. This
     * function allows you to parse only a *portion* of a document but
     * still return a syntax tree whose ranges match up with the document
     * as a whole. You can also pass multiple disjoint ranges.
     * If `ranges` is empty, then the entire document will be parsed.
     * Otherwise, the given ranges must be ordered from earliest to latest
     * in the document, and they must not overlap.
     */
    var includedRanges: JsArray<Range>?
}

/**
 * Options for query execution
 */
external interface QueryOptions : JsAny {
    /** The start position of the range to query */
    var startPosition: Point?

    /** The end position of the range to query */
    var endPosition: Point?

    /** The start index of the range to query */
    var startIndex: Int?

    /** The end index of the range to query */
    var endIndex: Int?

    /**
     * The maximum number of in-progress matches for this query.
     * The limit must be > 0 and <= 65536.
     */
    var matchLimit: Int?

    /**
     * The maximum start depth for a query cursor.
     *
     * This prevents cursors from exploring children nodes at a certain depth.
     * Note if a pattern includes many children, then they will still be
     * checked.
     *
     * The zero max start depth value can be used as a special behavior and
     * it helps to destructure a subtree by staying on a node and using
     * captures for interested parts. Note that the zero max start depth
     * only limit a search depth for a pattern's root node but other nodes
     * that are parts of the pattern may be searched at any depth what
     * defined by the pattern structure.
     *
     * Set to `null` to remove the maximum start depth.
     */
    var maxStartDepth: Int?
}

/**
 * A particular {@link Node} that has been captured with a particular name within a
 * {@link Query}.
 */
external interface QueryCapture : JsAny {
    /** The name of the capture */
    val name: String

    val text: String?

    /** The captured node */
    val node: SyntaxNode

    /** The properties for predicates declared with the operator `set!`. */
    val setProperties: JsAny?

    /** The properties for predicates declared with the operator `is?`. */
    val assertedProperties: JsAny?

    /** The properties for predicates declared with the operator `is-not?`. */
    val refutedProperties: JsAny?
}

/** A match of a {@link Query} to a particular set of {@link Node}s. */
external interface QueryMatch : JsAny {
    /** The index of the pattern that matched. */
    val pattern: Int

    /** The captures associated with the match. */
    val captures: JsArray<QueryCapture>
}

/**
 * A predicate that contains an operator and list of operands.
 */
external interface PredicateResult : JsAny {
    /** The operator of the predicate, like `match?`, `eq?`, `set!`, etc. */
    val operator: String

    /** The operands of the predicate, which are either captures or strings. */
    val operands: JsArray<JsAny>
}

external interface Query : JsAny {
    /** The names of the captures used in the query. */
    val captureNames: JsArray<JsAny>

    /**
     * The other user-defined predicates associated with the given index.
     *
     * This includes predicates with operators other than:
     * - `match?`
     * - `eq?` and `not-eq?`
     * - `any-of?` and `not-any-of?`
     * - `is?` and `is-not?`
     * - `set!`
     */
    val predicates: JsArray<JsAny>

    /** The properties for predicates with the operator `set!`. */
    val setProperties: JsArray<JsAny>

    /** The properties for predicates with the operator `is?`. */
    val assertedProperties: JsArray<JsAny>

    /** The properties for predicates with the operator `is-not?`. */
    val refutedProperties: JsArray<JsAny>

    /** The maximum number of in-progress matches for this cursor. */
    val matchLimit: Int

    /** Delete the query, freeing its resources. */
    fun delete()

    /// 如果一个方法是 optional，在 Kotlin 不要传 null 过来，简单一点，再复制一个不带 null 的

    /**
     * Iterate over all of the individual captures in the order that they
     * appear.
     *
     * This is useful if you don't care about which pattern matched, and just
     * want a single, ordered sequence of captures.
     *
     * @param node - The node to execute the query on.
     *
     * @param options - Options for query execution.
     */
    fun captures(node: SyntaxNode, options: QueryOptions?): JsArray<QueryCapture>

    /**
     * Iterate over all of the individual captures in the order that they
     * appear.
     *
     * This is useful if you don't care about which pattern matched, and just
     * want a single, ordered sequence of captures.
     *
     * @param node - The node to execute the query on.
     */
    fun captures(node: SyntaxNode): JsArray<QueryCapture>

    /**
     * Iterate over all of the matches in the order that they were found.
     *
     * Each match contains the index of the pattern that matched, and a list of
     * captures. Because multiple patterns can match the same set of nodes,
     * one match may contain captures that appear *before* some of the
     * captures from a previous match.
     *
     * @param node - The node to execute the query on.
     */
    fun matches(node: SyntaxNode): JsArray<QueryMatch>

    /**
     * Iterate over all of the matches in the order that they were found.
     *
     * Each match contains the index of the pattern that matched, and a list of
     * captures. Because multiple patterns can match the same set of nodes,
     * one match may contain captures that appear *before* some of the
     * captures from a previous match.
     *
     * @param node - The node to execute the query on.
     *
     * @param options - Options for query execution.
     */
    fun matches(node: SyntaxNode, options: QueryOptions?): JsArray<QueryMatch>

    /** Get the predicates for a given pattern. */
    fun predicatesForPattern(patternIndex: Int): JsArray<PredicateResult>

    /**
     * Disable a certain capture within a query.
     *
     * This prevents the capture from being returned in matches, and also
     * avoids any resource usage associated with recording the capture.
     */
    fun disableCapture(captureName: String)

    /**
     * Disable a certain pattern within a query.
     *
     * This prevents the pattern from matching, and also avoids any resource
     * usage associated with the pattern. This throws an error if the pattern
     * index is out of bounds.
     */
    fun disablePattern(patternIndex: Int)

    /**
     * Check if a given step in a query is 'definite'.
     *
     * A query step is 'definite' if its parent pattern will be guaranteed to
     * match successfully once it reaches the step.
     */
    fun isPatternGuaranteedAtStep(byteOffset: Int): Boolean

    /** Check if a given pattern within a query has a single root node. */
    fun isPatternRooted(patternIndex: Int): Boolean

    /** Check if a given pattern within a query has a single root node. */
    fun isPatternNonLocal(patternIndex: Int): Boolean

    /** Get the byte offset where the given pattern starts in the query's source. */
    fun startIndexForPattern(patternIndex: Int): Int

    /**
     * Check if, on its last execution, this cursor exceeded its maximum number
     * of in-progress matches.
     */
    fun didExceedMatchLimit(): Boolean
}

/** A single node within a syntax {@link Tree}. */
external interface SyntaxNode : JsAny {
    /** The tree that this node belongs to. */
    val tree: Tree

    /**
     * The numeric id for this node that is unique.
     *
     * Within a given syntax tree, no two nodes have the same id. However:
     *
     * * If a new tree is created based on an older tree, and a node from the old tree is reused in
     *   the process, then that node will have the same id in both trees.
     *
     * * A node not marked as having changes does not guarantee it was reused.
     *
     * * If a node is marked as having changed in the old tree, it will not be reused.
     */
    val id: Int

    /** Get this node's type as a numerical id. */
    val typeId: Int

    /**
     * Get the node's type as a numerical id as it appears in the grammar,
     * ignoring aliases.
     */
    val grammarId: Int

    /** Get this node's type as a string. */
    val type: String

    /**
     * Get this node's symbol name as it appears in the grammar, ignoring
     * aliases as a string.
     */
    val grammarType: String

    /**
     * Check if this node is *named*.
     *
     * Named nodes correspond to named rules in the grammar, whereas
     * *anonymous* nodes correspond to string literals in the grammar.
     */
    val isNamed: Boolean

    /**
     * Check if this node is *missing*.
     *
     * Missing nodes are inserted by the parser in order to recover from
     * certain kinds of syntax errors.
     */
    val isMissing: Boolean

    /**
     * Check if this node is *extra*.
     *
     * Extra nodes represent things like comments, which are not required
     * by the grammar, but can appear anywhere.
     */
    val isExtra: Boolean

    /** Check if this node has been edited. */
    val hasChanges: Boolean

    /**
     * Check if this node represents a syntax error or contains any syntax
     * errors anywhere within it.
     */
    val hasError: Boolean

    /**
     * Check if this node represents a syntax error.
     *
     * Syntax errors represent parts of the code that could not be incorporated
     * into a valid syntax tree.
     */
    val isError: Boolean

    /** Get the string content of this node. */
    val text: String

    /** Get this node's parse state. */
    val parseState: Int

    /** Get the parse state after this node. */
    val nextParseState: Int

    /** The position where this node starts. */
    val startPosition: Point

    /** The position where this node ends. */
    val endPosition: Point

    /** The byte index where this node starts. */
    val startIndex: Int

    /** The byte index where this node ends. */
    val endIndex: Int

    /**
     * Get this node's immediate parent.
     * Prefer {@link Node#childWithDescendant} for iterating over this node's ancestors.
     */
    val parent: SyntaxNode?

    /**
     * Iterate over this node's children.
     *
     * If you're walking the tree recursively, you may want to use the
     * {@link TreeCursor} APIs directly instead.
     */
    val children: JsArray<SyntaxNode>

    /**
     * Iterate over this node's named children.
     *
     * See also {@link Node#children}.
     */
    val namedChildren: JsArray<SyntaxNode>

    /** Get this node's number of children. */
    val childCount: Int

    /**
     * Get this node's number of *named* children.
     *
     * See also {@link Node#isNamed}.
     */
    val namedChildCount: Int

    /** Get this node's first child. */
    val firstChild: SyntaxNode?

    /**
     * Get this node's first named child.
     *
     * See also {@link Node#isNamed}.
     */
    val firstNamedChild: SyntaxNode?

    /** Get this node's last child. */
    val lastChild: SyntaxNode?

    /**
     * Get this node's last named child.
     *
     * See also {@link Node#isNamed}.
     */
    val lastNamedChild: SyntaxNode?

    /** Get this node's next sibling. */
    val nextSibling: SyntaxNode?

    /**
     * Get this node's next *named* sibling.
     *
     * See also {@link Node#isNamed}.
     */
    val nextNamedSibling: SyntaxNode?

    /** Get this node's previous sibling. */
    val previousSibling: SyntaxNode?

    /**
     * Get this node's previous *named* sibling.
     *
     * See also {@link Node#isNamed}.
     */
    val previousNamedSibling: SyntaxNode?

    /** Get the node's number of descendants, including one for the node itself. */
    val descendantCount: Int

    /** Check if this node is equal to another node. */
    fun equals(other: SyntaxNode): Boolean

    /**
     * Get the node's child at the given index, where zero represents the first child.
     *
     * This method is fairly fast, but its cost is technically log(n), so if
     * you might be iterating over a long list of children, you should use
     * {@link Node#children} instead.
     */
    fun child(index: Int): SyntaxNode?

    /**
     * Get this node's *named* child at the given index.
     *
     * See also {@link Node#isNamed}.
     * This method is fairly fast, but its cost is technically log(n), so if
     * you might be iterating over a long list of children, you should use
     * {@link Node#namedChildren} instead.
     */
    fun namedChild(index: Int): SyntaxNode?

    /**
     * Get the first child with the given field name.
     *
     * If multiple children may have the same field name, access them using
     * {@link Node#childrenForFieldName}.
     */
    fun childForFieldName(fieldName: String): SyntaxNode?

    /**
     * Get this node's child with the given numerical field id.
     *
     * See also {@link Node#childForFieldName}. You can
     * convert a field name to an id using {@link Language#fieldIdForName}.
     */
    fun childForFieldId(fieldId: Int): SyntaxNode?

    /** Get the field name of this node's child at the given index. */
    fun fieldNameForChild(childIndex: Int): String?

    /**
     * Get an array of this node's children with a given field name.
     *
     * See also {@link Node#children}.
     */
    fun childrenForFieldName(fieldName: String): JsArray<SyntaxNode>

    /**
     * Get an array of this node's children with a given field id.
     *
     * See also {@link Node#childrenForFieldName}.
     */
    fun childrenForFieldId(fieldId: Int): JsArray<SyntaxNode>

    /** Get the node's first child that contains or starts after the given byte offset. */
    fun firstChildForIndex(index: Int): SyntaxNode?

    /** Get the node's first named child that contains or starts after the given byte offset. */
    fun firstNamedChildForIndex(index: Int): SyntaxNode?

    /** Get the smallest node within this node that spans the given byte range. */
    fun descendantForIndex(index: Int): SyntaxNode

    /** Get the smallest node within this node that spans the given byte range. */
    fun descendantForIndex(startIndex: Int, endIndex: Int): SyntaxNode

    /** Get the smallest named node within this node that spans the given byte range. */
    fun namedDescendantForIndex(index: Int): SyntaxNode

    /** Get the smallest named node within this node that spans the given byte range. */
    fun namedDescendantForIndex(startIndex: Int, endIndex: Int): SyntaxNode

    /** Get the smallest node within this node that spans the given point range. */
    fun descendantForPosition(position: Point): SyntaxNode

    /** Get the smallest node within this node that spans the given point range. */
    fun descendantForPosition(startPosition: Point, endPosition: Point): SyntaxNode

    /** Get the smallest named node within this node that spans the given point range. */
    fun namedDescendantForPosition(position: Point): SyntaxNode

    /** Get the smallest named node within this node that spans the given point range. */
    fun namedDescendantForPosition(startPosition: Point, endPosition: Point): SyntaxNode

    /**
     * Get the descendants of this node that are the given type, or in the given types array.
     *
     * The types array should contain node type strings, which can be retrieved from {@link Language#types}.
     *
     * Additionally, a `startPosition` and `endPosition` can be passed in to restrict the search to a byte range.
     */
    fun descendantsOfType(types: JsAny, startPosition: Point?, endPosition: Point?): JsArray<SyntaxNode>

    /**
     * Create a new {@link TreeCursor} starting from this node.
     *
     * Note that the given node is considered the root of the cursor,
     * and the cursor cannot walk outside this node.
     */
    fun walk(): TreeCursor
}

/** A stateful object for walking a syntax {@link Tree} efficiently. */
// TreeCursor
external interface TreeCursor : JsAny {
    /** Get the type of the cursor's current node. */
    val nodeType: String

    /** Get the type id of the cursor's current node. */
    val nodeTypeId: Int

    /** Get the state id of the cursor's current node. */
    val nodeStateId: Int

    /** Get the string content of the cursor's current node. */
    val nodeText: String

    /** Get the id of the cursor's current node. */
    val nodeId: Int

    /**
     * Check if the cursor's current node is *named*.
     *
     * Named nodes correspond to named rules in the grammar, whereas
     * *anonymous* nodes correspond to string literals in the grammar.
     */
    val nodeIsNamed: Boolean

    /**
     * Check if the cursor's current node is *missing*.
     *
     * Missing nodes are inserted by the parser in order to recover from
     * certain kinds of syntax errors.
     */
    val nodeIsMissing: Boolean

    /** Get the start position of the cursor's current node. */
    val startPosition: Point

    /** Get the end position of the cursor's current node. */
    val endPosition: Point

    /** Get the start index of the cursor's current node. */
    val startIndex: Int

    /** Get the end index of the cursor's current node. */
    val endIndex: Int

    /** Get the tree cursor's current {@link Node}. */
    val currentNode: SyntaxNode

    /** Get the field name of this tree cursor's current node. */
    val currentFieldName: String

    /**
     * Get the numerical field id of this tree cursor's current node.
     *
     * See also {@link TreeCursor#currentFieldName}.
     */
    val currentFieldId: Int

    /**
     * Get the depth of the cursor's current node relative to the original
     * node that the cursor was constructed with.
     */
    val currentDepth: Int

    /**
     * Get the index of the cursor's current node out of all of the
     * descendants of the original node that the cursor was constructed with.
     */
    val currentDescendantIndex: Int

    /**
     * Re-initialize this tree cursor to start at the original node that the
     * cursor was constructed with.
     */
    fun reset(node: SyntaxNode)

    /**
     * Re-initialize a tree cursor to the same position as another cursor.
     *
     * Unlike {@link TreeCursor#reset}, this will not lose parent
     * information and allows reusing already created cursors.
     */
    fun resetTo(cursor: TreeCursor)

    /** Delete the tree cursor, freeing its resources. */
    fun delete()

    /**
     * Move this cursor to the parent of its current node.
     *
     * This returns `true` if the cursor successfully moved, and returns
     * `false` if there was no parent node (the cursor was already on the
     * root node).
     *
     * Note that the node the cursor was constructed with is considered the root
     * of the cursor, and the cursor cannot walk outside this node.
     */
    fun gotoParent(): Boolean

    /**
     * Move this cursor to the first child of its current node.
     *
     * This returns `true` if the cursor successfully moved, and returns
     * `false` if there were no children.
     */
    fun gotoFirstChild(): Boolean

    /**
     * Move this cursor to the last child of its current node.
     *
     * This returns `true` if the cursor successfully moved, and returns
     * `false` if there were no children.
     *
     * Note that this function may be slower than
     * {@link TreeCursor#gotoFirstChild} because it needs to
     * iterate through all the children to compute the child's position.
     */
    fun gotoLastChild(): Boolean

    /**
     * Move this cursor to the first child of its current node that contains or
     * starts after the given byte offset.
     *
     * This returns `true` if the cursor successfully moved to a child node, and returns
     * `false` if no such child was found.
     */
    fun gotoFirstChildForIndex(goalIndex: Int): Boolean

    /**
     * Move this cursor to the first child of its current node that contains or
     * starts after the given byte offset.
     *
     * This returns the index of the child node if one was found, and returns
     * `null` if no such child was found.
     */
    fun gotoFirstChildForPosition(goalPosition: Point): Boolean

    /**
     * Move this cursor to the next sibling of its current node.
     *
     * This returns `true` if the cursor successfully moved, and returns
     * `false` if there was no next sibling node.
     *
     * Note that the node the cursor was constructed with is considered the root
     * of the cursor, and the cursor cannot walk outside this node.
     */
    fun gotoNextSibling(): Boolean

    /**
     * Move this cursor to the previous sibling of its current node.
     *
     * This returns `true` if the cursor successfully moved, and returns
     * `false` if there was no previous sibling node.
     *
     * Note that this function may be slower than
     * {@link TreeCursor#gotoNextSibling} due to how node
     * positions are stored. In the worst case, this will need to iterate
     * through all the children up to the previous sibling node to recalculate
     * its position. Also note that the node the cursor was constructed with is
     * considered the root of the cursor, and the cursor cannot walk outside this node.
     */
    fun gotoPreviousSibling(): Boolean

    /**
     * Move the cursor to the node that is the nth descendant of
     * the original node that the cursor was constructed with, where
     * zero represents the original node itself.
     */
    fun gotoDescendant(goalDescendantIndex: Int)
}

/** A tree that represents the syntactic structure of a source code file. */
// Tree
external interface Tree : JsAny {
    /** Get the root node of the syntax tree. */
    val rootNode: SyntaxNode

    /**
     * Get the root node of the syntax tree, but with its position shifted
     * forward by the given offset.
     */
    fun rootNodeWithOffset(offsetBytes: Int, offsetExtent: Point): SyntaxNode

    /** Create a shallow copy of the syntax tree. This is very fast. */
    fun copy(): Tree

    /** Delete the syntax tree, freeing its resources. */
    fun delete()

    /**
     * Edit the syntax tree to keep it in sync with source code that has been
     * edited.
     *
     * You must describe the edit both in terms of byte offsets and in terms of
     * row/column coordinates.
     */
    fun edit(edit: Edit): Tree

    /** Create a new {@link TreeCursor} starting from the root of the tree. */
    fun walk(): TreeCursor

    /**
     * Compare this old edited syntax tree to a new syntax tree representing
     * the same document, returning a sequence of ranges whose syntactic
     * structure has changed.
     *
     * For this to work correctly, this syntax tree must have been edited such
     * that its ranges match up to the new tree. Generally, you'll want to
     * call this method right after calling one of the [`Parser::parse`]
     * functions. Call it on the old tree that was passed to parse, and
     * pass the new tree that was returned from `parse`.
     */
    fun getChangedRanges(other: Tree): JsArray<Range>

    /** Get the included ranges that were used to parse the syntax tree. */
    fun getIncludedRanges(): JsArray<Range>

    fun getEditedRange(other: Tree): Range
}

// LookaheadIterable
external interface LookaheadIterable : JsAny {
    /** Get the current symbol of the lookahead iterator. */
    val currentTypeId: Int

    /** Get the current symbol name of the lookahead iterator. */
    val currentType: String

    /** Delete the lookahead iterator, freeing its resources. */
    fun delete()

    /**
     * Reset the lookahead iterator to another state.
     *
     * This returns `true` if the iterator was reset to the given state and
     * `false` otherwise.
     */
    fun resetState(stateId: Int): Boolean
}

/**
 * A stateful object that is used to produce a {@link Tree} based on some
 * source code.
 */
// Parser - 保持与原 WebTreeSitter 结构兼容
@JsModule("web-tree-sitter")
external object WebTreeSitter {
    @JsName("default")
    class Parser : JsAny {
        companion object Companion {
            /**
             * This must always be called before creating a Parser.
             *
             * You can optionally pass in options to configure the Wasm module, the most common
             * one being `locateFile` to help the module find the `.wasm` file.
             */
            fun init(): Promise<JsAny>
        }

        /**
         * Delete the parser, freeing its resources.
         */
        fun delete()

        /**
         * Parse a slice of UTF8 text.
         */
        fun parse(input: String): Tree

        /**
         * Get the ranges of text that the parser will include when parsing.
         */
        fun getIncludedRanges(): JsArray<Range>

        fun getTimeoutMicros(): Int

        fun setTimeoutMicros(timeout: Int)

        /**
         * Instruct the parser to start the next parse from the beginning.
         *
         * If the parser previously failed because of a callback,
         * then by default, it will resume where it left off on the
         * next call to {@link Parser#parse} or other parsing functions.
         * If you don't want to resume, and instead intend to use this parser to
         * parse some other document, you must call `reset` first.
         */
        fun reset()

        /**
         * Set the language that the parser should use for parsing.
         *
         * If the language was not successfully assigned, an error will be thrown.
         * This happens if the language was generated with an incompatible
         * version of the Tree-sitter CLI. Check the language's version using
         * {@link Language#version} and compare it to this library's
         * {@link LANGUAGE_VERSION} and {@link MIN_COMPATIBLE_VERSION} constants.
         */
        fun setLanguage(language: Language?)

        /**
         * An opaque object that defines how to parse a particular language.
         * The code for each `Language` is generated by the Tree-sitter CLI.
         */
        class Language : JsAny {
            /**
             * Gets the ABI version of the language.
             */
            val version: Int

            /**
             * Gets the number of fields in the language.
             */
            val fieldCount: Int

            /**
             * Gets the number of states in the language.
             */
            val stateCount: Int

            /**
             * Gets the number of node types in the language.
             */
            val nodeTypeCount: Int

            /**
             * Get the field name for a field id.
             */
            fun fieldNameForId(fieldId: Int): String?

            /**
             * Get the field id for a field name.
             */
            fun fieldIdForName(fieldName: String): Int?

            /**
             * Get the node type id for a node type name.
             */
            fun idForNodeType(type: String, named: Boolean): Int

            /**
             * Get the node type name for a node type id.
             */
            fun nodeTypeForId(typeId: Int): String?

            /**
             * Check if a node type is named.
             *
             * @see {@link https://tree-sitter.github.io/tree-sitter/using-parsers/2-basic-parsing.html#named-vs-anonymous-nodes}
             */
            fun nodeTypeIsNamed(typeId: Int): Boolean

            /**
             * Check if a node type is visible.
             */
            fun nodeTypeIsVisible(typeId: Int): Boolean

            /**
             * Get the next state id for a given state id and node type id.
             */
            fun nextState(stateId: Int, typeId: Int): Int

            /**
             * Create a new query from a string containing one or more S-expression
             * patterns.
             *
             * The query is associated with a particular language, and can only be run
             * on syntax nodes parsed with that language. References to Queries can be
             * shared between multiple threads.
             *
             * @link {@see https://tree-sitter.github.io/tree-sitter/using-parsers/queries}
             */
            fun query(source: String): Query

            /**
             * Create a new lookahead iterator for this language and parse state.
             *
             * This returns `null` if state is invalid for this language.
             *
             * Iterating {@link LookaheadIterator} will yield valid symbols in the given
             * parse state. Newly created lookahead iterators will return the `ERROR`
             * symbol from {@link LookaheadIterator#currentType}.
             *
             * Lookahead iterators can be useful for generating suggestions and improving
             * syntax error diagnostics. To get symbols valid in an `ERROR` node, use the
             * lookahead iterator on its first leaf node state. For `MISSING` nodes, a
             * lookahead iterator created on the previous non-extra leaf node may be
             * appropriate.
             */
            fun lookaheadIterator(stateId: Int): LookaheadIterable?

            companion object Companion {
                /**
                 * Load a language from a WebAssembly module.
                 * The module can be provided as a path to a file or as a buffer.
                 */
                fun load(input: String): Promise<Language>
            }
        }
    }
}

typealias TreeSitterParser = WebTreeSitter.Parser
typealias TreeSitterTree = Tree
typealias TreeSitterNode = SyntaxNode
typealias TreeSitterPoint = Point

// Console for debugging
external object console : JsAny {
    fun log(message: String)
    fun error(message: String)
    fun warn(message: String)
}
