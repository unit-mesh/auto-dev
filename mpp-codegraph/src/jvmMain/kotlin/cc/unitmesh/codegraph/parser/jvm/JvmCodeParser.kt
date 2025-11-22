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
        
        processNode(node, sourceCode, filePath, packageName, language, nodes)
        
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
        when (node.type) {
            // Java class-like structures
            "class_declaration", "interface_declaration", "enum_declaration",
            // JavaScript/TypeScript class
            "class", "class_declaration",
            // Python class
            "class_definition" -> {
                val codeNode = createCodeNode(node, sourceCode, filePath, packageName, language, parentName)
                nodes.add(codeNode)

                // Process children with this node as parent
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    processNode(child, sourceCode, filePath, packageName, language, nodes, codeNode.name)
                }
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

