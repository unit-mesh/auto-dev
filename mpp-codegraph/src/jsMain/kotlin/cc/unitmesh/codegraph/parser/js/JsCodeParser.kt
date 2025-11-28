package cc.unitmesh.codegraph.parser.js

import cc.unitmesh.codegraph.model.*
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
import kotlinx.coroutines.await
import kotlin.js.Promise

// External declarations for Node.js modules
@JsModule("path")
@JsNonModule
external object NodePath {
    fun join(vararg paths: String): String
    fun dirname(path: String): String
}

/**
 * JS implementation of CodeParser using web-tree-sitter.
 * Based on autodev-workbench implementation.
 */
class JsCodeParser : CodeParser {
    
    companion object {
        // Global TreeSitter initialization state - shared across all instances
        private var globalInitialized = false
        private var globalTreeSitter: dynamic = null
        private val globalParsers = mutableMapOf<Language, dynamic>()
        private var globalWasmBasePath: String = ""
        
        @Suppress("UNUSED_PARAMETER")
        private fun createInitOptions(webTreeSitterPath: String): dynamic {
            return js("""({
                locateFile: function(scriptName, scriptDirectory) {
                    if (scriptName.endsWith('.wasm')) {
                        return webTreeSitterPath + '/' + scriptName;
                    }
                    return scriptDirectory + scriptName;
                }
            })""")
        }
        
        @Suppress("UNUSED_PARAMETER")
        internal fun createParser(TreeSitterClass: dynamic): dynamic {
            return js("new TreeSitterClass()")
        }
    }
    
    /**
     * Initialize TreeSitter and load language grammars
     */
    suspend fun initialize() {
        if (globalInitialized) return
        
        // Initialize TreeSitter - require returns the TreeSitter class
        globalTreeSitter = js("require('web-tree-sitter')")
        
        // Find the base path for web-tree-sitter WASM file
        val webTreeSitterWasm = js("require.resolve('web-tree-sitter/tree-sitter.wasm')") as String
        val webTreeSitterPath = NodePath.dirname(webTreeSitterWasm)
        
        // Find the base path for treesitter-artifacts WASM files
        val artifactsPackageJson = js("require.resolve('@unit-mesh/treesitter-artifacts/package.json')") as String
        globalWasmBasePath = NodePath.join(NodePath.dirname(artifactsPackageJson), "wasm")
        
        // Create init options with locateFile to properly locate the tree-sitter.wasm file
        val initOptions = createInitOptions(webTreeSitterPath)
        
        val initPromise = globalTreeSitter.init(initOptions) as Promise<Unit>
        initPromise.await()
        
        globalInitialized = true
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
                "fileCount" to files.size.toString()
            )
        )
    }
    
    override suspend fun parseImports(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<ImportInfo> {
        initialize()
        
        val parser = getOrCreateParser(language)
        val tree = parser.parse(sourceCode)
        val rootNode = tree.rootNode
        
        return extractImportsFromNode(rootNode, sourceCode, filePath, language)
    }
    
    private fun extractImportsFromNode(
        rootNode: dynamic,
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<ImportInfo> {
        val imports = mutableListOf<ImportInfo>()
        
        fun traverse(node: dynamic) {
            val nodeType = node.type as String
            val startLine = (node.startPosition.row as Int) + 1
            val endLine = (node.endPosition.row as Int) + 1
            
            when (nodeType) {
                // Java/Kotlin imports
                "import_declaration" -> {
                    val rawText = extractNodeText(node, sourceCode)
                    val importText = rawText
                        .removePrefix("import")
                        .removeSuffix(";")
                        .trim()
                    
                    val isStatic = importText.startsWith("static ")
                    val cleanImport = if (isStatic) importText.removePrefix("static ").trim() else importText
                    val isWildcard = cleanImport.endsWith(".*")
                    
                    imports.add(ImportInfo(
                        path = cleanImport.removeSuffix(".*"),
                        type = ImportType.MODULE,
                        alias = null,
                        isWildcard = isWildcard,
                        isStatic = isStatic,
                        filePath = filePath,
                        startLine = startLine,
                        endLine = endLine,
                        rawText = rawText
                    ))
                }
                // TypeScript/JavaScript imports
                "import_statement" -> {
                    val rawText = extractNodeText(node, sourceCode)
                    val fromMatch = Regex("""from\s+['"]([^'"]+)['"]""").find(rawText)
                    if (fromMatch != null) {
                        val importPath = fromMatch.groupValues[1]
                        val isRelative = importPath.startsWith(".")
                        imports.add(ImportInfo(
                            path = importPath,
                            type = if (isRelative) ImportType.RELATIVE else ImportType.MODULE,
                            alias = null,
                            isWildcard = rawText.contains("* as"),
                            filePath = filePath,
                            startLine = startLine,
                            endLine = endLine,
                            rawText = rawText
                        ))
                    }
                }
                // Python imports
                "import_from_statement" -> {
                    val rawText = extractNodeText(node, sourceCode)
                    val fromMatch = Regex("""from\s+([\w.]+)\s+import""").find(rawText)
                    val path = fromMatch?.groupValues?.get(1) ?: ""
                    if (path.isNotEmpty()) {
                        imports.add(ImportInfo(
                            path = path,
                            type = ImportType.SELECTIVE,
                            alias = null,
                            isWildcard = rawText.contains("*"),
                            filePath = filePath,
                            startLine = startLine,
                            endLine = endLine,
                            rawText = rawText
                        ))
                    }
                }
            }
            
            // Traverse children
            val childCount = node.childCount as Int
            for (i in 0 until childCount) {
                val child = node.child(i)
                if (child != null) {
                    traverse(child)
                }
            }
        }
        
        traverse(rootNode)
        return imports
    }
    
    private suspend fun getOrCreateParser(language: Language): dynamic {
        if (globalParsers.containsKey(language)) {
            return globalParsers[language]
        }
        
        // Create a new parser instance from the initialized TreeSitter
        // After TreeSitter.init(), new TreeSitter() creates a Parser instance
        val parser = createParser(globalTreeSitter)
        
        // Load language grammar from the treesitter-artifacts package
        val wasmFileName = getLanguageWasmName(language)
        val wasmPath = NodePath.join(globalWasmBasePath, wasmFileName)
        
        val languagePromise = globalTreeSitter.Language.load(wasmPath) as Promise<dynamic>
        val languageGrammar = languagePromise.await()
        
        parser.setLanguage(languageGrammar)
        globalParsers[language] = parser
        
        return parser
    }
    
    private fun getLanguageWasmName(language: Language): String {
        return when (language) {
            Language.JAVA -> "tree-sitter-java.wasm"
            Language.KOTLIN -> "tree-sitter-kotlin.wasm"
            Language.JAVASCRIPT -> "tree-sitter-typescript.wasm"
            Language.TYPESCRIPT -> "tree-sitter-typescript.wasm"
            Language.PYTHON -> "tree-sitter-python.wasm"
            else -> throw IllegalArgumentException("Unsupported language: $language")
        }
    }
    
    private fun buildNodesFromTreeNode(
        rootNode: dynamic,
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
        node: dynamic,
        sourceCode: String,
        filePath: String,
        packageName: String,
        language: Language,
        nodes: MutableList<CodeNode>,
        parentName: String = ""
    ) {
        val nodeType = node.type as String
        
        when (nodeType) {
            "class_declaration", "interface_declaration", "enum_declaration",
            "class_body", "object_declaration" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
                
                // Process children with this node as parent
                val childCount = node.childCount as Int
                for (i in 0 until childCount) {
                    val child = node.child(i)
                    if (child != null) {
                        processNode(child, sourceCode, filePath, packageName, language, nodes, codeNode.name)
                    }
                }
            }
            // Java/Kotlin constructors
            "constructor_declaration", "primary_constructor", "secondary_constructor" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }
            "method_declaration", "function_declaration", "function" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }
            "field_declaration", "property_declaration", "variable_declaration" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }
            else -> {
                // Recursively process children
                val childCount = node.childCount as Int
                for (i in 0 until childCount) {
                    val child = node.child(i)
                    if (child != null) {
                        processNode(child, sourceCode, filePath, packageName, language, nodes, parentName)
                    }
                }
            }
        }
    }
    
    private fun createCodeNode(
        node: dynamic,
        sourceCode: String,
        filePath: String,
        packageName: String,
        language: Language,
        parentName: String
    ): CodeNode {
        val name = extractName(node, sourceCode)
        val type = mapNodeTypeToCodeElementType(node.type as String)
        val content = extractNodeText(node, sourceCode)
        
        val qualifiedName = if (parentName.isNotEmpty()) {
            "$packageName.$parentName.$name"
        } else {
            "$packageName.$name"
        }
        
        // Generate a simple ID based on qualified name
        val id = qualifiedName.hashCode().toString()
        
        return CodeNode(
            id = id,
            type = type,
            name = name,
            packageName = packageName,
            filePath = filePath,
            startLine = (node.startPosition.row as Int) + 1,
            endLine = (node.endPosition.row as Int) + 1,
            startColumn = node.startPosition.column as Int,
            endColumn = node.endPosition.column as Int,
            qualifiedName = qualifiedName,
            content = content,
            metadata = mapOf(
                "language" to language.name,
                "nodeType" to (node.type as String),
                "parent" to parentName
            )
        )
    }
    
    private fun extractPackageName(node: dynamic, sourceCode: String): String {
        val childCount = node.childCount as Int
        for (i in 0 until childCount) {
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
    
    private fun extractName(node: dynamic, sourceCode: String): String {
        val nodeType = node.type as String
        
        // Handle constructors - they use special naming
        when (nodeType) {
            "constructor_declaration", "primary_constructor", "secondary_constructor" -> return "<init>"
        }
        
        val childCount = node.childCount as Int
        for (i in 0 until childCount) {
            val child = node.child(i)
            if (child != null && child.type == "identifier") {
                return extractNodeText(child, sourceCode)
            }
        }
        return "unknown"
    }
    
    private fun extractNodeText(node: dynamic, sourceCode: String): String {
        val startByte = node.startIndex as Int
        val endByte = node.endIndex as Int
        
        // TreeSitter returns byte offsets, but String.substring uses character indices
        // Convert byte offsets to character offsets
        val bytes = sourceCode.encodeToByteArray()
        
        // Validate byte offsets
        if (startByte < 0 || endByte > bytes.size || startByte > endByte) {
            return ""
        }
        
        // Extract the byte range and convert back to string
        return bytes.sliceArray(startByte until endByte).decodeToString()
    }
    
    private fun mapNodeTypeToCodeElementType(nodeType: String): CodeElementType {
        return when (nodeType) {
            "class_declaration", "class_body", "object_declaration" -> CodeElementType.CLASS
            "interface_declaration" -> CodeElementType.INTERFACE
            "enum_declaration" -> CodeElementType.ENUM
            // Java/Kotlin constructors
            "constructor_declaration", "primary_constructor", "secondary_constructor" -> CodeElementType.CONSTRUCTOR
            "method_declaration", "function_declaration", "function" -> CodeElementType.METHOD
            "field_declaration" -> CodeElementType.FIELD
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

