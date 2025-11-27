package cc.unitmesh.codegraph.parser.jvm

import cc.unitmesh.codegraph.model.*
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterCSharp
import org.treesitter.TreeSitterJava
import org.treesitter.TreeSitterKotlin
import org.treesitter.TreeSitterJavascript
import org.treesitter.TreeSitterPython
import org.treesitter.TreeSitterRust
import java.util.*

/**
 * JVM implementation of CodeParser using TreeSitter Java bindings.
 * Based on SASK project implementation.
 */
class JvmCodeParser : CodeParser {
    
    override suspend fun parseNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val parser = createParser(language)
        val tree = parser.parseString(null, sourceCode)
        val rootNode = tree.rootNode
        
        return buildNodesFromTreeNode(rootNode, sourceCode, filePath, language)
    }
    
    override suspend fun parseNodesAndRelationships(
        sourceCode: String,
        filePath: String,
        language: Language
    ): Pair<List<CodeNode>, List<CodeRelationship>> {
        val parser = createParser(language)
        val tree = parser.parseString(null, sourceCode)
        val rootNode = tree.rootNode
        
        val nodes = buildNodesFromTreeNode(rootNode, sourceCode, filePath, language)
        val relationships = buildRelationships(nodes, rootNode, sourceCode)
        
        return Pair(nodes, relationships)
    }
    
    override suspend fun parseCodeGraph(
        files: Map<String, String>,
        language: Language
    ): CodeGraph {
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
    
    private fun createParser(language: Language): TSParser {
        val parser = TSParser()
        parser.language = when (language) {
            Language.JAVA -> TreeSitterJava()
            Language.KOTLIN -> TreeSitterKotlin()
            Language.JAVASCRIPT, Language.TYPESCRIPT -> TreeSitterJavascript()
            Language.CSHARP -> TreeSitterCSharp()
            Language.RUST -> TreeSitterRust()
            Language.PYTHON -> TreeSitterPython()
            else -> throw IllegalArgumentException("Unsupported language: $language")
        }
        return parser
    }
    
    private fun buildNodesFromTreeNode(
        node: TSNode,
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val packageName = extractPackageName(node, sourceCode)
        
        // First try to handle fragmented class declarations (when Tree-sitter fails to parse properly)
        // This happens with large Kotlin files where class, identifier, constructor are separate nodes
        handleFragmentedClasses(node, sourceCode, filePath, packageName, language, nodes)
        
        processNode(node, sourceCode, filePath, packageName, language, nodes)
        
        return nodes
    }
    
    /**
     * Handle fragmented class declarations in Kotlin.
     * Sometimes Tree-sitter fails to parse a class properly and outputs:
     *   class (keyword) -> simple_identifier -> primary_constructor -> {
     * instead of a proper class_declaration node.
     * 
     * This function scans root children and reconstructs class declarations from fragments.
     */
    private fun handleFragmentedClasses(
        rootNode: TSNode,
        sourceCode: String,
        filePath: String,
        packageName: String,
        language: Language,
        nodes: MutableList<CodeNode>
    ) {
        if (language != Language.KOTLIN) return
        
        var i = 0
        while (i < rootNode.childCount) {
            val child = rootNode.getChild(i) ?: break
            
            // Look for standalone "class" keyword at root level
            if (child.type == "class") {
                // Check if next sibling is an identifier (class name)
                val nextNode = if (i + 1 < rootNode.childCount) rootNode.getChild(i + 1) else null
                
                if (nextNode != null && (nextNode.type == "simple_identifier" || nextNode.type == "type_identifier")) {
                    val className = extractNodeText(nextNode, sourceCode)
                    
                    // Find the class body end (look for matching closing brace)
                    var endLine = child.endPoint.row + 1
                    var endColumn = child.endPoint.column
                    
                    // Scan forward to find the class body and its end
                    for (j in i + 2 until rootNode.childCount) {
                        val scanNode = rootNode.getChild(j) ?: continue
                        
                        // Skip primary_constructor, but track its end
                        if (scanNode.type == "primary_constructor") {
                            endLine = scanNode.endPoint.row + 1
                            endColumn = scanNode.endPoint.column
                            continue
                        }
                        
                        // Look for the opening brace of class body
                        if (scanNode.type == "{") {
                            // Now we need to find the matching closing brace
                            // For simplicity, we'll look for function_declaration children
                            // and take the last one's end as an approximation
                            // (This is a heuristic - in practice we'd need to track braces)
                            endLine = scanNode.endPoint.row + 1
                            endColumn = scanNode.endPoint.column
                            
                            // Scan further for class members to find class end
                            for (k in j + 1 until rootNode.childCount) {
                                val memberNode = rootNode.getChild(k) ?: continue
                                // If we hit another class or package-level element, stop
                                if (memberNode.type == "class" || 
                                    memberNode.type == "class_declaration" ||
                                    memberNode.type == "object_declaration") {
                                    break
                                }
                                // Update end position with each class member
                                if (memberNode.type == "function_declaration" ||
                                    memberNode.type == "property_declaration") {
                                    endLine = memberNode.endPoint.row + 1
                                    endColumn = memberNode.endPoint.column
                                }
                            }
                            break
                        }
                        
                        // If we encounter another class or function, we've gone too far
                        if (scanNode.type == "class" || 
                            scanNode.type == "class_declaration" ||
                            scanNode.type == "function_declaration") {
                            break
                        }
                    }
                    
                    // Create the class node
                    val classNode = CodeNode(
                        id = java.util.UUID.randomUUID().toString(),
                        type = CodeElementType.CLASS,
                        name = className,
                        packageName = packageName,
                        filePath = filePath,
                        startLine = child.startPoint.row + 1,
                        endLine = endLine,
                        startColumn = child.startPoint.column,
                        endColumn = endColumn,
                        qualifiedName = "$packageName.$className",
                        content = "",  // Content extraction can be added if needed
                        metadata = mapOf(
                            "language" to language.name,
                            "nodeType" to "fragmented_class",
                            "parent" to ""
                        )
                    )
                    nodes.add(classNode)
                }
            }
            i++
        }
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
        when (node.type) {
            // Java/Kotlin class-like structures
            "class_declaration", "interface_declaration", "enum_declaration",
            // JavaScript/TypeScript class (note: "class" is a keyword in Kotlin, not a declaration)
            "class_definition" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)

                // Process children with this node as parent
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    processNode(child, sourceCode, filePath, packageName, language, nodes, codeNode.name)
                }
            }
            // Java/Kotlin constructors
            "constructor_declaration",
            // Kotlin primary/secondary constructors
            "primary_constructor", "secondary_constructor" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }
            // Java/Kotlin methods
            "method_declaration", "function_declaration",
            // JavaScript/TypeScript functions
            "function", "function_declaration", "method_definition",
            // Python functions
            "function_definition" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }
            // Java/Kotlin fields
            "field_declaration", "property_declaration",
            // JavaScript/TypeScript fields
            "field_definition", "public_field_definition" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)
            }
            else -> {
                // Recursively process children
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    processNode(child, sourceCode, filePath, packageName, language, nodes, parentName)
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
        
        val qualifiedName = if (parentName.isNotEmpty()) {
            "$packageName.$parentName.$name"
        } else {
            "$packageName.$name"
        }
        
        return CodeNode(
            id = UUID.randomUUID().toString(),
            type = type,
            name = name,
            packageName = packageName,
            filePath = filePath,
            startLine = node.startPoint.row + 1,
            endLine = node.endPoint.row + 1,
            startColumn = node.startPoint.column,
            endColumn = node.endPoint.column,
            qualifiedName = qualifiedName,
            content = content,
            metadata = mapOf(
                "language" to language.name,
                "nodeType" to node.type,
                "parent" to parentName
            )
        )
    }
    
    private fun extractPackageName(node: TSNode, sourceCode: String): String {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            when (child.type) {
                // Java/Kotlin package
                "package_declaration" -> {
                    return extractNodeText(child, sourceCode)
                        .removePrefix("package")
                        .removeSuffix(";")
                        .trim()
                }
                // JavaScript/TypeScript module (we'll use empty string for now)
                // Python doesn't have explicit package declarations in the file
            }
        }
        return ""
    }
    
    private fun extractName(node: TSNode, sourceCode: String): String {
        // Handle constructors - they use special naming
        when (node.type) {
            "constructor_declaration" -> return "<init>"
            "primary_constructor" -> return "<init>"
            "secondary_constructor" -> return "<init>"
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (child.type == "identifier" || child.type == "type_identifier" || child.type == "simple_identifier") {
                return extractNodeText(child, sourceCode)
            }
        }
        return "unknown"
    }
    
    private fun extractNodeText(node: TSNode, sourceCode: String): String {
        val startByte = node.startByte
        val endByte = node.endByte
        
        // TreeSitter returns byte offsets, but String.substring uses character indices
        // Convert byte offsets to character offsets
        val bytes = sourceCode.toByteArray(Charsets.UTF_8)
        
        // Validate byte offsets
        if (startByte < 0 || endByte > bytes.size || startByte > endByte) {
            return ""
        }
        
        // Extract the byte range and convert back to string
        return String(bytes, startByte, endByte - startByte, Charsets.UTF_8)
    }
    
    private fun mapNodeTypeToCodeElementType(nodeType: String): CodeElementType {
        return when (nodeType) {
            // Java/Kotlin
            "class_declaration" -> CodeElementType.CLASS
            "interface_declaration" -> CodeElementType.INTERFACE
            "enum_declaration" -> CodeElementType.ENUM
            // Java/Kotlin constructors
            "constructor_declaration", "primary_constructor", "secondary_constructor" -> CodeElementType.CONSTRUCTOR
            "method_declaration", "function_declaration" -> CodeElementType.METHOD
            "field_declaration" -> CodeElementType.FIELD
            "property_declaration" -> CodeElementType.PROPERTY
            // JavaScript/TypeScript
            "class" -> CodeElementType.CLASS
            "function", "method_definition" -> CodeElementType.METHOD
            "field_definition", "public_field_definition" -> CodeElementType.FIELD
            // Python
            "class_definition" -> CodeElementType.CLASS
            "function_definition" -> CodeElementType.METHOD
            else -> CodeElementType.UNKNOWN
        }
    }
    
    private fun buildRelationships(
        nodes: List<CodeNode>,
        rootNode: TSNode,
        sourceCode: String
    ): List<CodeRelationship> {
        val relationships = mutableListOf<CodeRelationship>()
        
        // Build EXTENDS and IMPLEMENTS relationships
        for (node in nodes) {
            if (node.type == CodeElementType.CLASS || node.type == CodeElementType.INTERFACE) {
                // This is simplified - in real implementation, we'd parse the AST more carefully
                // to extract extends/implements information
            }
        }
        
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

