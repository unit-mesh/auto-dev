package cc.unitmesh.codegraph.parser.wasm

import cc.unitmesh.codegraph.model.*
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * WASM-JS implementation of CodeParser using web-tree-sitter.
 *
 * This implementation uses TreeSitterInterop.kt interfaces to interact with the web-tree-sitter JavaScript API.
 * It loads TreeSitter WASM grammars from @unit-mesh/treesitter-artifacts npm package.
 *
 * Reference: https://github.com/tree-sitter/tree-sitter/tree/master/lib/binding_web
 */
class WasmJsCodeParser : CodeParser {

    private var isInitialized = false
    private val parsers = mutableMapOf<Language, WebTreeSitter.Parser>()
    private val languages = mutableMapOf<Language, WebTreeSitter.Parser.Language>()

    /**
     * Initialize TreeSitter WASM runtime
     */
    suspend fun initialize() {
        if (isInitialized) return

        try {
            console.log("Initializing TreeSitter WASM...")
            val initPromise: Promise<JsAny> = WebTreeSitter.Parser.init().unsafeCast()
            initPromise.await<JsAny>()
            console.log("TreeSitter WASM initialized successfully")
            isInitialized = true
        } catch (e: Throwable) {
            console.error("Failed to initialize TreeSitter: ${e.message ?: "Unknown error"}")
            throw e
        }
    }

    override suspend fun parseNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        initialize()

        val parser = getOrCreateParser(language)
        val tree = parser.parse(sourceCode)
        val rootNode = tree.rootNode

        return buildNodesFromTreeNode(rootNode, sourceCode, filePath, language)
    }

    override suspend fun parseNodesAndRelationships(
        sourceCode: String,
        filePath: String,
        language: Language
    ): Pair<List<CodeNode>, List<CodeRelationship>> {
        initialize()

        val parser = getOrCreateParser(language)
        val tree = parser.parse(sourceCode)
        val rootNode = tree.rootNode

        val nodes = buildNodesFromTreeNode(rootNode, sourceCode, filePath, language)
        val relationships = buildRelationships(nodes, rootNode, language)

        return Pair(nodes, relationships)
    }

    override suspend fun parseCodeGraph(
        files: Map<String, String>,
        language: Language
    ): CodeGraph {
        initialize()

        val allNodes = mutableListOf<CodeNode>()
        val allRelationships = mutableListOf<CodeRelationship>()

        for ((filePath, sourceCode) in files) {
            val (nodes, relationships) = parseNodesAndRelationships(sourceCode, filePath, language)
            allNodes.addAll(nodes)
            allRelationships.addAll(relationships)
        }

        // Generate MADE_OF relationships
        generateMadeOfRelationships(allNodes, allRelationships)

        return CodeGraph(
            nodes = allNodes,
            relationships = allRelationships,
            metadata = mapOf(
                "language" to language.name,
                "fileCount" to files.size.toString(),
                "platform" to "wasm-js"
            )
        )
    }

    private suspend fun getOrCreateParser(language: Language): WebTreeSitter.Parser {
        if (parsers.containsKey(language)) {
            return parsers[language]!!
        }

        if (!isInitialized) {
            throw IllegalStateException("TreeSitter not initialized. Call initialize() first.")
        }

        val wasmPath = language.getWasmPath()
        console.log("Loading language grammar from: $wasmPath")
        // check if wasmPath exists
        if (!wasmPath.startsWith("http")) {
            throw IllegalArgumentException("WASM path must be a URL: $wasmPath")
        }

        try {
            val loadPromise: Promise<WebTreeSitter.Parser.Language> =
                WebTreeSitter.Parser.Language.load(wasmPath).unsafeCast()
            val lang: WebTreeSitter.Parser.Language = loadPromise.await()

            // Create parser and set language
            val parser = WebTreeSitter.Parser()
            parser.setLanguage(lang)

            parsers[language] = parser
            languages[language] = lang

            console.log("Language grammar loaded successfully for ${language.name}")
            return parser
        } catch (e: Throwable) {
            console.error("Failed to load language from $wasmPath: ${e.message ?: "Unknown error"}")
            throw e
        }
    }

    private fun buildNodesFromTreeNode(
        rootNode: SyntaxNode,
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val packageName = extractPackageName(rootNode, sourceCode, language)

        processNode(rootNode, sourceCode, filePath, packageName, language, nodes)

        return nodes
    }

    private fun processNode(
        node: SyntaxNode,
        sourceCode: String,
        filePath: String,
        packageName: String,
        language: Language,
        nodes: MutableList<CodeNode>,
        parentName: String = ""
    ) {
        val nodeType = node.type

        when (nodeType) {
            "class_declaration", "interface_declaration", "enum_declaration",
            "class_body", "object_declaration", "class", "class_definition" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)

                // Process children with this node as parent
                for (child in node.children.toArray()) {
                    processNode(child, sourceCode, filePath, packageName, language, nodes, codeNode.name)
                }
            }

            // Java/Kotlin constructors
            "constructor_declaration", "primary_constructor", "secondary_constructor" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }

            "method_declaration", "function_declaration", "function", "function_definition", "method_definition" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }

            "field_declaration", "property_declaration", "variable_declaration",
            "field_definition", "public_field_definition" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }

            else -> {
                // Recursively process children
                for (child in node.children.toArray()) {
                    processNode(child, sourceCode, filePath, packageName, language, nodes, parentName)
                }
            }
        }
    }

    private fun createCodeNode(
        node: SyntaxNode,
        sourceCode: String,
        filePath: String,
        packageName: String,
        language: Language,
        parentName: String
    ): CodeNode {
        val name = extractName(node, sourceCode)
        val type = mapNodeTypeToCodeElementType(node.type)
        val content = node.text

        val qualifiedName = buildQualifiedName(packageName, parentName, name)

        // Generate a simple ID based on qualified name
        val id = qualifiedName.hashCode().toString()

        return CodeNode(
            id = id,
            type = type,
            name = name,
            packageName = packageName,
            filePath = filePath,
            startLine = node.startPosition.row + 1,
            endLine = node.endPosition.row + 1,
            startColumn = node.startPosition.column,
            endColumn = node.endPosition.column,
            qualifiedName = qualifiedName,
            content = content,
            metadata = mapOf(
                "language" to language.name,
                "nodeType" to node.type,
                "parent" to parentName,
                "platform" to "wasm-js"
            )
        )
    }

    private fun buildQualifiedName(packageName: String, parentName: String, name: String): String {
        return when {
            parentName.isNotEmpty() && packageName.isNotEmpty() -> "$packageName.$parentName.$name"
            parentName.isNotEmpty() -> "$parentName.$name"
            packageName.isNotEmpty() -> "$packageName.$name"
            else -> name
        }
    }

    private fun extractPackageName(node: SyntaxNode, sourceCode: String, language: Language): String {
        val lang = languages[language] ?: return extractPackageNameFallback(node)

        val queryString = when (language) {
            Language.JAVA, Language.KOTLIN -> "(package_declaration (scoped_identifier) @package.name)"
            Language.PYTHON -> "(module) @module"
            Language.JAVASCRIPT, Language.TYPESCRIPT -> "(export_statement) @export"
            else -> return extractPackageNameFallback(node)
        }

        return try {
            val query = lang.query(queryString)
            val captures = query.captures(node).toArray()

            val packageName = captures.firstOrNull { it.name == "package.name" }?.node?.text
                ?: captures.firstOrNull()?.node?.text
                ?: ""

            query.delete()
            packageName.removePrefix("package").removeSuffix(";").trim()
        } catch (e: Throwable) {
            extractPackageNameFallback(node)
        }
    }

    private fun extractPackageNameFallback(node: SyntaxNode): String {
        for (child in node.children.toArray()) {
            if (child.type == "package_declaration") {
                return child.text.removePrefix("package").removeSuffix(";").trim()
            }
        }
        return ""
    }

    private fun extractName(node: SyntaxNode, sourceCode: String): String {
        // Handle constructors - they use special naming
        when (node.type) {
            "constructor_declaration", "primary_constructor", "secondary_constructor" -> return "<init>"
        }
        
        for (child in node.children.toArray()) {
            if (child.type == "identifier") {
                return child.text
            }
        }
        return "unknown"
    }

    private fun mapNodeTypeToCodeElementType(nodeType: String): CodeElementType {
        return when (nodeType) {
            "class_declaration", "class_body", "object_declaration", "class", "class_definition" -> CodeElementType.CLASS
            "interface_declaration" -> CodeElementType.INTERFACE
            "enum_declaration" -> CodeElementType.ENUM
            // Java/Kotlin constructors
            "constructor_declaration", "primary_constructor", "secondary_constructor" -> CodeElementType.CONSTRUCTOR
            "method_declaration", "function_declaration", "function", "function_definition", "method_definition" -> CodeElementType.METHOD
            "field_declaration", "field_definition", "public_field_definition" -> CodeElementType.FIELD
            "property_declaration" -> CodeElementType.PROPERTY
            "variable_declaration" -> CodeElementType.VARIABLE
            else -> CodeElementType.UNKNOWN
        }
    }

    private fun buildRelationships(
        nodes: List<CodeNode>,
        rootNode: SyntaxNode,
        language: Language
    ): List<CodeRelationship> {
        val relationships = mutableListOf<CodeRelationship>()
        val lang = languages[language]

        if (lang != null) {
            try {
                // Extract inheritance relationships (extends/implements)
                val inheritanceQuery = when (language) {
                    Language.JAVA, Language.KOTLIN -> """
                        (class_declaration
                            superclass: (superclass (type_identifier) @superclass))
                        (class_declaration
                            interfaces: (super_interfaces (type_list (type_identifier) @interface)))
                    """.trimIndent()

                    Language.JAVASCRIPT, Language.TYPESCRIPT -> """
                        (class_declaration
                            superclass: (_) @superclass)
                    """.trimIndent()

                    Language.PYTHON -> """
                        (class_definition
                            superclasses: (argument_list) @superclass)
                    """.trimIndent()

                    else -> return relationships
                }

                val query = lang.query(inheritanceQuery)
                val captures = query.captures(rootNode).toArray()

                for (capture in captures) {
                    val relationshipType = when (capture.name) {
                        "superclass" -> RelationshipType.EXTENDS
                        "interface" -> RelationshipType.IMPLEMENTS
                        else -> continue
                    }

                    // Find source and target nodes
                    val targetName = capture.node.text
                    val sourceNode = findNodeContaining(nodes, capture.node)
                    val targetNode = nodes.find { it.name == targetName }

                    if (sourceNode != null && targetNode != null) {
                        relationships.add(
                            CodeRelationship(
                                sourceId = sourceNode.id,
                                targetId = targetNode.id,
                                type = relationshipType
                            )
                        )
                    }
                }

                query.delete()
            } catch (e: Throwable) {
                console.warn("Failed to extract inheritance relationships: ${e.message}")
            }
        }

        return relationships
    }

    private fun findNodeContaining(nodes: List<CodeNode>, syntaxNode: SyntaxNode): CodeNode? {
        val startLine = syntaxNode.startPosition.row + 1
        val endLine = syntaxNode.endPosition.row + 1

        return nodes.find { node ->
            startLine >= node.startLine && endLine <= node.endLine
        }
    }

    private fun generateMadeOfRelationships(
        nodes: List<CodeNode>,
        relationships: MutableList<CodeRelationship>
    ) {
        // Group nodes by parent
        val nodesByParent = nodes.groupBy { it.metadata["parent"] ?: "" }

        for ((parentName, childNodes) in nodesByParent) {
            if (parentName.isEmpty()) continue

            val parentNode = nodes.find { it.name == parentName } ?: continue

            for (childNode in childNodes) {
                relationships.add(
                    CodeRelationship(
                        sourceId = parentNode.id,
                        targetId = childNode.id,
                        type = RelationshipType.MADE_OF
                    )
                )
            }
        }
    }

    /**
     * Parse source code and return the syntax tree
     * This is a low-level method that returns the raw tree for advanced use cases
     */
    suspend fun parse(sourceCode: String, language: Language): Tree? {
        initialize()
        return try {
            val parser = getOrCreateParser(language)
            parser.parse(sourceCode)
        } catch (e: Throwable) {
            console.error("Failed to parse code: ${e.message}")
            null
        }
    }

    /**
     * Parse source code and return a formatted string representation
     */
    suspend fun parseToString(sourceCode: String, language: Language): String {
        initialize()
        val parser = getOrCreateParser(language)
        val tree = parser.parse(sourceCode)
        return formatNode(tree.rootNode, 0)
    }

    /**
     * Extract all method/function names from source code
     */
    suspend fun extractMethodNames(sourceCode: String, language: Language): List<String> {
        initialize()
        val parser = getOrCreateParser(language)
        val tree = parser.parse(sourceCode)
        val lang = languages[language] ?: return emptyList()

        val queryString = when (language) {
            Language.JAVA, Language.KOTLIN -> """
                (method_declaration name: (identifier) @method.name)
                (constructor_declaration name: (identifier) @method.name)
                (function_declaration name: (identifier) @method.name)
            """.trimIndent()

            Language.JAVASCRIPT, Language.TYPESCRIPT -> """
                (function_declaration name: (identifier) @method.name)
                (method_definition name: (property_identifier) @method.name)
            """.trimIndent()

            Language.PYTHON -> """
                (function_definition name: (identifier) @method.name)
            """.trimIndent()

            Language.GO -> """
                (function_declaration name: (identifier) @method.name)
                (method_declaration name: (field_identifier) @method.name)
            """.trimIndent()

            Language.RUST -> """
                (function_item name: (identifier) @method.name)
            """.trimIndent()

            else -> return emptyList()
        }

        return try {
            val query = lang.query(queryString)
            val captures = query.captures(tree.rootNode).toArray()
            val methodNames = captures
                .filter { it.name == "method.name" }
                .map { it.node.text }
                .distinct()
            query.delete()
            methodNames
        } catch (e: Throwable) {
            console.warn("Failed to extract method names: ${e.message}")
            emptyList()
        }
    }

    /**
     * Extract all class names from source code
     */
    suspend fun extractClassNames(sourceCode: String, language: Language): List<String> {
        initialize()
        val parser = getOrCreateParser(language)
        val tree = parser.parse(sourceCode)
        val lang = languages[language] ?: return emptyList()

        val queryString = when (language) {
            Language.JAVA, Language.KOTLIN -> "(class_declaration name: (identifier) @class.name)"
            Language.JAVASCRIPT, Language.TYPESCRIPT -> "(class_declaration name: (type_identifier) @class.name)"
            Language.PYTHON -> "(class_definition name: (identifier) @class.name)"
            Language.GO -> "(type_declaration (type_spec name: (type_identifier) @class.name))"
            Language.RUST -> "(struct_item name: (type_identifier) @class.name)"
            else -> return emptyList()
        }

        return try {
            val query = lang.query(queryString)
            val captures = query.captures(tree.rootNode).toArray()
            val classNames = captures
                .filter { it.name == "class.name" }
                .map { it.node.text }
                .distinct()
            query.delete()
            classNames
        } catch (e: Throwable) {
            console.warn("Failed to extract class names: ${e.message}")
            emptyList()
        }
    }

    /**
     * Extract field/property names from source code
     */
    suspend fun extractFieldNames(sourceCode: String, language: Language): List<String> {
        initialize()
        val parser = getOrCreateParser(language)
        val tree = parser.parse(sourceCode)
        val lang = languages[language] ?: return emptyList()

        val queryString = when (language) {
            Language.JAVA -> """
                (field_declaration 
                    declarator: (variable_declarator name: (identifier) @field.name))
            """.trimIndent()

            Language.KOTLIN -> """
                (property_declaration (variable_declaration (simple_identifier) @field.name))
            """.trimIndent()

            Language.JAVASCRIPT, Language.TYPESCRIPT -> """
                (field_definition name: (property_identifier) @field.name)
                (public_field_definition name: (property_identifier) @field.name)
            """.trimIndent()

            Language.PYTHON -> """
                (expression_statement (assignment left: (identifier) @field.name))
            """.trimIndent()

            else -> return emptyList()
        }

        return try {
            val query = lang.query(queryString)
            val captures = query.captures(tree.rootNode).toArray()
            val fieldNames = captures
                .filter { it.name == "field.name" }
                .map { it.node.text }
                .distinct()
            query.delete()
            fieldNames
        } catch (e: Throwable) {
            console.warn("Failed to extract field names: ${e.message}")
            emptyList()
        }
    }

    override suspend fun parseImports(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<ImportInfo> {
        initialize()
        
        val parser = getOrCreateParser(language)
        val tree = parser.parse(sourceCode)
        val lang = languages[language] ?: return emptyList()
        
        val queryString = when (language) {
            Language.JAVA, Language.KOTLIN -> """
                (import_declaration (identifier) @import.path)
                (import_list (import_header (identifier) @import.path))
            """.trimIndent()
            
            Language.JAVASCRIPT, Language.TYPESCRIPT -> """
                (import_statement source: (string) @import.path)
            """.trimIndent()
            
            Language.PYTHON -> """
                (import_statement name: (dotted_name) @import.path)
                (import_from_statement module_name: (dotted_name) @import.path)
            """.trimIndent()
            
            Language.GO -> """
                (import_declaration (import_spec path: (interpreted_string_literal) @import.path))
            """.trimIndent()
            
            else -> return emptyList()
        }
        
        return try {
            val query = lang.query(queryString)
            val captures = query.captures(tree.rootNode).toArray()
            val imports = captures
                .filter { it.name == "import.path" }
                .map { capture ->
                    val importPath = capture.node.text.trim('"', '\'')
                    ImportInfo(
                        path = importPath,
                        type = ImportType.MODULE,
                        filePath = filePath,
                        startLine = capture.node.startPosition.row + 1,
                        endLine = capture.node.endPosition.row + 1,
                        isStatic = false,
                        alias = null,
                        importedNames = emptyList(),
                        rawText = capture.node.text
                    )
                }
                .distinct()
            query.delete()
            imports
        } catch (e: Throwable) {
            console.warn("Failed to extract imports: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if code contains syntax errors
     */
    suspend fun hasSyntaxErrors(sourceCode: String, language: Language): Boolean {
        initialize()
        return try {
            val parser = getOrCreateParser(language)
            val tree = parser.parse(sourceCode)
            tree.rootNode.hasError || tree.rootNode.isError
        } catch (e: Throwable) {
            true
        }
    }

    /**
     * Format a node and its children as a readable string
     */
    private fun formatNode(node: SyntaxNode, depth: Int): String {
        val indent = "  ".repeat(depth)
        val result = StringBuilder()

        result.appendLine("$indent${node.type} [${node.startPosition.row}:${node.startPosition.column} - ${node.endPosition.row}:${node.endPosition.column}]")

        if (node.text.isNotEmpty() && node.isNamed) {
            val textPreview = node.text.take(50).replace("\n", "\\n")
            if (node.text.length > 50) {
                result.appendLine("$indent  Text: \"$textPreview...\"")
            } else {
                result.appendLine("$indent  Text: \"$textPreview\"")
            }
        }

        for (child in node.children.toArray()) {
            result.append(formatNode(child, depth + 1))
        }

        return result.toString()
    }
}

/**
 * Extension function to get WASM path for a language
 */
private fun Language.getWasmPath(): String {
    val langId = when (this) {
        Language.CSHARP -> "c_sharp"
        Language.JAVASCRIPT -> "typescript"
        Language.TYPESCRIPT -> "typescript"
        else -> this.name.lowercase()
    }

    return "node_modules/@unit-mesh/treesitter-artifacts/wasm/tree-sitter-$langId.wasm"
}
