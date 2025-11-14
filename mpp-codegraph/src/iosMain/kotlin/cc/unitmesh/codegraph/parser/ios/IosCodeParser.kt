package cc.unitmesh.codegraph.parser.ios

import cc.unitmesh.codegraph.model.*
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language

/**
 * iOS implementation of CodeParser.
 * 
 * Note: This is a simplified implementation that provides basic parsing capabilities.
 * TreeSitter native bindings are not available for iOS, so this implementation
 * uses regex-based parsing to extract basic code structure information.
 * 
 * For production use cases, consider using server-side parsing or JS-based parsing
 * via web views if full TreeSitter functionality is required.
 */
class IosCodeParser : CodeParser {
    
    override suspend fun parseNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        return when (language) {
            Language.JAVA, Language.KOTLIN -> parseOOPNodes(sourceCode, filePath, language)
            Language.JAVASCRIPT, Language.TYPESCRIPT -> parseJSNodes(sourceCode, filePath, language)
            Language.PYTHON -> parsePythonNodes(sourceCode, filePath, language)
            else -> emptyList()
        }
    }
    
    override suspend fun parseNodesAndRelationships(
        sourceCode: String,
        filePath: String,
        language: Language
    ): Pair<List<CodeNode>, List<CodeRelationship>> {
        val nodes = parseNodes(sourceCode, filePath, language)
        val relationships = buildRelationships(nodes)
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
        
        return CodeGraph(
            nodes = allNodes,
            relationships = allRelationships,
            metadata = mapOf(
                "language" to language.name,
                "fileCount" to files.size.toString(),
                "platform" to "iOS"
            )
        )
    }
    
    /**
     * Parse Object-Oriented Programming language nodes (Java, Kotlin)
     */
    private fun parseOOPNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val lines = sourceCode.lines()
        
        // Extract package name
        val packageName = extractPackageName(sourceCode)
        
        // Find classes, interfaces, enums
        val classPattern = Regex("""(class|interface|enum|object)\s+(\w+)""")
        val methodPattern = Regex("""(fun|def|public|private|protected|static)?\s*\w+\s+(\w+)\s*\(""")
        val propertyPattern = Regex("""(val|var|private|public|protected)\s+(\w+)\s*[:=]""")
        
        var currentLine = 0
        for ((index, line) in lines.withIndex()) {
            currentLine = index + 1
            
            // Match class/interface/enum
            classPattern.find(line)?.let { match ->
                val type = when (match.groupValues[1]) {
                    "class", "object" -> CodeElementType.CLASS
                    "interface" -> CodeElementType.INTERFACE
                    "enum" -> CodeElementType.ENUM
                    else -> CodeElementType.CLASS
                }
                val name = match.groupValues[2]
                nodes.add(
                    createCodeNode(
                        name = name,
                        type = type,
                        packageName = packageName,
                        filePath = filePath,
                        startLine = currentLine,
                        endLine = findBlockEnd(lines, index),
                        language = language
                    )
                )
            }
            
            // Match methods/functions
            methodPattern.find(line)?.let { match ->
                val name = match.groupValues[2]
                if (name != "if" && name != "for" && name != "while") { // Filter out keywords
                    nodes.add(
                        createCodeNode(
                            name = name,
                            type = CodeElementType.METHOD,
                            packageName = packageName,
                            filePath = filePath,
                            startLine = currentLine,
                            endLine = findBlockEnd(lines, index),
                            language = language
                        )
                    )
                }
            }
            
            // Match properties
            propertyPattern.find(line)?.let { match ->
                val name = match.groupValues[2]
                nodes.add(
                    createCodeNode(
                        name = name,
                        type = CodeElementType.PROPERTY,
                        packageName = packageName,
                        filePath = filePath,
                        startLine = currentLine,
                        endLine = currentLine,
                        language = language
                    )
                )
            }
        }
        
        return nodes
    }
    
    /**
     * Parse JavaScript/TypeScript nodes
     */
    private fun parseJSNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val lines = sourceCode.lines()
        
        val functionPattern = Regex("""(function|const|let|var)\s+(\w+)\s*[=\(]""")
        val classPattern = Regex("""class\s+(\w+)""")
        
        for ((index, line) in lines.withIndex()) {
            val currentLine = index + 1
            
            // Match classes
            classPattern.find(line)?.let { match ->
                val name = match.groupValues[1]
                nodes.add(
                    createCodeNode(
                        name = name,
                        type = CodeElementType.CLASS,
                        packageName = "",
                        filePath = filePath,
                        startLine = currentLine,
                        endLine = findBlockEnd(lines, index),
                        language = language
                    )
                )
            }
            
            // Match functions
            functionPattern.find(line)?.let { match ->
                val name = match.groupValues[2]
                nodes.add(
                    createCodeNode(
                        name = name,
                        type = CodeElementType.FUNCTION,
                        packageName = "",
                        filePath = filePath,
                        startLine = currentLine,
                        endLine = findBlockEnd(lines, index),
                        language = language
                    )
                )
            }
        }
        
        return nodes
    }
    
    /**
     * Parse Python nodes
     */
    private fun parsePythonNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val lines = sourceCode.lines()
        
        val classPattern = Regex("""class\s+(\w+)""")
        val functionPattern = Regex("""def\s+(\w+)\s*\(""")
        
        for ((index, line) in lines.withIndex()) {
            val currentLine = index + 1
            
            // Match classes
            classPattern.find(line)?.let { match ->
                val name = match.groupValues[1]
                nodes.add(
                    createCodeNode(
                        name = name,
                        type = CodeElementType.CLASS,
                        packageName = "",
                        filePath = filePath,
                        startLine = currentLine,
                        endLine = findPythonBlockEnd(lines, index),
                        language = language
                    )
                )
            }
            
            // Match functions
            functionPattern.find(line)?.let { match ->
                val name = match.groupValues[1]
                nodes.add(
                    createCodeNode(
                        name = name,
                        type = CodeElementType.FUNCTION,
                        packageName = "",
                        filePath = filePath,
                        startLine = currentLine,
                        endLine = findPythonBlockEnd(lines, index),
                        language = language
                    )
                )
            }
        }
        
        return nodes
    }
    
    /**
     * Extract package name from source code
     */
    private fun extractPackageName(sourceCode: String): String {
        val packagePattern = Regex("""package\s+([\w.]+)""")
        return packagePattern.find(sourceCode)?.groupValues?.get(1) ?: ""
    }
    
    /**
     * Find the end line of a code block (based on brace matching)
     */
    private fun findBlockEnd(lines: List<String>, startIndex: Int): Int {
        var braceCount = 0
        var foundOpenBrace = false
        
        for (i in startIndex until lines.size) {
            val line = lines[i]
            for (char in line) {
                when (char) {
                    '{' -> {
                        braceCount++
                        foundOpenBrace = true
                    }
                    '}' -> braceCount--
                }
            }
            
            if (foundOpenBrace && braceCount == 0) {
                return i + 1
            }
        }
        
        // If no closing brace found, return a reasonable estimate
        return minOf(startIndex + 10, lines.size)
    }
    
    /**
     * Find the end line of a Python code block (based on indentation)
     */
    private fun findPythonBlockEnd(lines: List<String>, startIndex: Int): Int {
        if (startIndex >= lines.size - 1) return startIndex + 1
        
        val startLine = lines[startIndex]
        val baseIndent = startLine.takeWhile { it.isWhitespace() }.length
        
        for (i in (startIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            
            val indent = line.takeWhile { it.isWhitespace() }.length
            if (indent <= baseIndent && line.trim().isNotEmpty()) {
                return i
            }
        }
        
        return lines.size
    }
    
    /**
     * Create a CodeNode with the given parameters
     */
    private fun createCodeNode(
        name: String,
        type: CodeElementType,
        packageName: String,
        filePath: String,
        startLine: Int,
        endLine: Int,
        language: Language
    ): CodeNode {
        val qualifiedName = if (packageName.isNotEmpty()) {
            "$packageName.$name"
        } else {
            name
        }
        
        return CodeNode(
            id = qualifiedName.hashCode().toString(),
            type = type,
            name = name,
            packageName = packageName,
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            startColumn = 0,
            endColumn = 0,
            qualifiedName = qualifiedName,
            content = "",
            metadata = mapOf(
                "language" to language.name,
                "platform" to "iOS"
            )
        )
    }
    
    /**
     * Build relationships between nodes (simplified implementation)
     */
    private fun buildRelationships(nodes: List<CodeNode>): List<CodeRelationship> {
        val relationships = mutableListOf<CodeRelationship>()
        
        // Create MADE_OF relationships for methods inside classes
        val classesByFile = nodes.filter { 
            it.type == CodeElementType.CLASS || it.type == CodeElementType.INTERFACE 
        }.groupBy { it.filePath }
        
        val methodsByFile = nodes.filter { 
            it.type == CodeElementType.METHOD || it.type == CodeElementType.FUNCTION 
        }.groupBy { it.filePath }
        
        for ((filePath, classes) in classesByFile) {
            val methods = methodsByFile[filePath] ?: continue
            
            for (clazz in classes) {
                val classStartLine = clazz.startLine
                val classEndLine = clazz.endLine
                
                for (method in methods) {
                    if (method.startLine > classStartLine && method.endLine <= classEndLine) {
                        relationships.add(
                            CodeRelationship(
                                sourceId = clazz.id,
                                targetId = method.id,
                                type = RelationshipType.MADE_OF
                            )
                        )
                    }
                }
            }
        }
        
        return relationships
    }
}

