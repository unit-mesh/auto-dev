package cc.unitmesh.codegraph.parser.wasm

import cc.unitmesh.codegraph.model.*
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
import kotlinx.coroutines.await

/**
 * WASM-JS implementation of CodeParser using web-tree-sitter.
 * 
 * This implementation uses external interfaces to interact with the web-tree-sitter JavaScript API.
 * It loads TreeSitter WASM grammars from @unit-mesh/treesitter-artifacts npm package.
 * 
 * Reference: https://github.com/tree-sitter/tree-sitter/tree/master/lib/binding_web
 */
class WasmJsCodeParser : CodeParser {
    
    private var initialized = false
    private val parsers = mutableMapOf<Language, TSParser>()
    
    /**
     * Initialize TreeSitter WASM runtime
     */
    suspend fun initialize() {
        if (initialized) return

        try {
            ParserModule.init().await<JsAny>()
            initialized = true
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
        val relationships = buildRelationships(nodes)
        
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
    
    private suspend fun getOrCreateParser(language: Language): TSParser {
        if (parsers.containsKey(language)) {
            return parsers[language]!!
        }

        val parser = Parser()

        // Load language grammar from WASM artifacts
        val languageName = getLanguageWasmPath(language)
        val languageGrammar = LanguageModule.load(languageName).await<TSLanguageGrammar>()

        parser.setLanguage(languageGrammar)
        parsers[language] = parser

        return parser
    }
    
    private fun getLanguageWasmPath(language: Language): String {
        return when (language) {
            Language.JAVA -> "node_modules/@unit-mesh/treesitter-artifacts/tree-sitter-java.wasm"
            Language.KOTLIN -> "node_modules/@unit-mesh/treesitter-artifacts/tree-sitter-kotlin.wasm"
            Language.JAVASCRIPT -> "node_modules/@unit-mesh/treesitter-artifacts/tree-sitter-javascript.wasm"
            Language.TYPESCRIPT -> "node_modules/@unit-mesh/treesitter-artifacts/tree-sitter-typescript.wasm"
            Language.PYTHON -> "node_modules/@unit-mesh/treesitter-artifacts/tree-sitter-python.wasm"
            Language.GO -> "node_modules/@unit-mesh/treesitter-artifacts/tree-sitter-go.wasm"
            Language.RUST -> "node_modules/@unit-mesh/treesitter-artifacts/tree-sitter-rust.wasm"
            Language.CSHARP -> "node_modules/@unit-mesh/treesitter-artifacts/tree-sitter-c-sharp.wasm"
            else -> throw IllegalArgumentException("Unsupported language: $language")
        }
    }
    
    private fun buildNodesFromTreeNode(
        rootNode: TSNode,
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val packageName = extractPackageName(rootNode, sourceCode)
        
        processNode(rootNode, sourceCode, filePath, packageName, language, nodes)
        
        return nodes
    }
    
    private fun processNode(
        node: TSNode,
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
                for (i in 0 until node.childCount) {
                    val child = node.child(i)
                    if (child != null) {
                        processNode(child, sourceCode, filePath, packageName, language, nodes, codeNode.name)
                    }
                }
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
                for (i in 0 until node.childCount) {
                    val child = node.child(i)
                    if (child != null) {
                        processNode(child, sourceCode, filePath, packageName, language, nodes, parentName)
                    }
                }
            }
        }
    }
    
    private fun createCodeNode(
        node: TSNode,
        sourceCode: String,
        filePath: String,
        packageName: String,
        language: Language,
        parentName: String
    ): CodeNode {
        val name = extractName(node, sourceCode)
        val type = mapNodeTypeToCodeElementType(node.type)
        val content = extractNodeText(node, sourceCode)
        
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
    
    private fun extractPackageName(node: TSNode, sourceCode: String): String {
        for (i in 0 until node.childCount) {
            val child = node.child(i)
            if (child != null && child.type == "package_declaration") {
                return extractNodeText(child, sourceCode)
                    .removePrefix("package")
                    .removeSuffix(";")
                    .trim()
            }
        }
        return ""
    }
    
    private fun extractName(node: TSNode, sourceCode: String): String {
        for (i in 0 until node.childCount) {
            val child = node.child(i)
            if (child != null && child.type == "identifier") {
                return extractNodeText(child, sourceCode)
            }
        }
        return "unknown"
    }
    
    private fun extractNodeText(node: TSNode, sourceCode: String): String {
        val startByte = node.startIndex
        val endByte = node.endIndex
        return sourceCode.substring(startByte, endByte)
    }
    
    private fun mapNodeTypeToCodeElementType(nodeType: String): CodeElementType {
        return when (nodeType) {
            "class_declaration", "class_body", "object_declaration", "class", "class_definition" -> CodeElementType.CLASS
            "interface_declaration" -> CodeElementType.INTERFACE
            "enum_declaration" -> CodeElementType.ENUM
            "method_declaration", "function_declaration", "function", "function_definition", "method_definition" -> CodeElementType.METHOD
            "field_declaration", "field_definition", "public_field_definition" -> CodeElementType.FIELD
            "property_declaration" -> CodeElementType.PROPERTY
            "variable_declaration" -> CodeElementType.VARIABLE
            else -> CodeElementType.UNKNOWN
        }
    }
    
    private fun buildRelationships(nodes: List<CodeNode>): List<CodeRelationship> {
        val relationships = mutableListOf<CodeRelationship>()
        // Simplified - in real implementation, we'd parse extends/implements
        return relationships
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
}
