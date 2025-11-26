package cc.unitmesh.devins.document

import cc.unitmesh.codegraph.CodeGraphFactory
import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.model.CodeNode
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * JS Platform Code document parser using mpp-codegraph
 * 
 * Parses source code files (Java, Kotlin, Python, JavaScript, TypeScript, etc.)
 * and extracts code structure (classes, methods, functions) as a hierarchical
 * document structure compatible with DocumentFile/TOCItem model.
 * 
 * Supported Languages:
 * - Java (.java)
 * - Kotlin (.kt, .kts)
 * - JavaScript (.js)
 * - TypeScript (.ts, .tsx)
 * - Python (.py)
 * - Go (.go)
 * - Rust (.rs)
 * - C# (.cs)
 */
class CodeDocumentParser : DocumentParserService {
    private val codeParser: CodeParser = CodeGraphFactory.createParser()
    private var currentCodeNodes: List<CodeNode> = emptyList()
    private var currentContent: String? = null
    
    override fun getDocumentContent(): String? = currentContent
    
    /**
     * Parse source code file and build hierarchical structure
     */
    override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode {
        logger.info { "=== Starting Code Parse ===" }
        logger.info { "File: ${file.path}, Size: ${content.length} bytes" }
        
        try {
            currentContent = content
            
            // Detect language from file extension
            val language = detectLanguage(file.name)
            if (language == Language.UNKNOWN) {
                logger.warn { "Unknown language for file: ${file.name}" }
                return file.copy(
                    metadata = file.metadata.copy(
                        parseStatus = ParseStatus.PARSE_FAILED
                    )
                )
            }
            
            logger.info { "Detected language: $language" }
            
            // Parse code using mpp-codegraph
            currentCodeNodes = codeParser.parseNodes(content, file.path, language)
            logger.info { "Extracted ${currentCodeNodes.size} code nodes" }
            
            // Build TOC from code structure (classes -> methods -> fields)
            val toc = buildTOCFromCodeNodes(currentCodeNodes)
            logger.info { "Built TOC with ${toc.size} top-level items" }
            
            // Build entities (classes, functions)
            val entities = buildEntitiesFromCodeNodes(currentCodeNodes)
            logger.info { "Built ${entities.size} entities" }
            
            logger.info { "=== Parse Complete ===" }
            
            return file.copy(
                toc = toc,
                entities = entities,
                metadata = file.metadata.copy(
                    parseStatus = ParseStatus.PARSED,
                    chapterCount = toc.size,
                    language = language.name.lowercase()
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse code: ${e.message}" }
            return file.copy(
                metadata = file.metadata.copy(
                    parseStatus = ParseStatus.PARSE_FAILED
                )
            )
        }
    }
    
    /**
     * Parse from ByteArray (delegates to parse with String conversion)
     */
    override suspend fun parseBytes(file: DocumentFile, bytes: ByteArray): DocumentTreeNode {
        val content = bytes.decodeToString()
        return parse(file, content)
    }
    
    /**
     * Query code by heading (class/method name)
     */
    override suspend fun queryHeading(keyword: String): List<DocumentChunk> {
        if (currentContent == null) return emptyList()
        
        val matchingNodes = currentCodeNodes.filter { node ->
            node.name.contains(keyword, ignoreCase = true) ||
            node.qualifiedName.contains(keyword, ignoreCase = true)
        }
        
        return matchingNodes.map { node ->
            val nodeContent = extractNodeContent(node, currentContent!!)
            DocumentChunk(
                documentPath = node.filePath,
                chapterTitle = buildNodeTitle(node),
                content = nodeContent,
                anchor = "#${node.id}",
                startLine = node.startLine,
                endLine = node.endLine,
                position = PositionMetadata(
                    documentPath = node.filePath,
                    formatType = DocumentFormatType.SOURCE_CODE,
                    position = DocumentPosition.LineRange(
                        startLine = node.startLine,
                        endLine = node.endLine
                    )
                )
            )
        }
    }
    
    /**
     * Query code by chapter ID (node ID)
     */
    override suspend fun queryChapter(chapterId: String): DocumentChunk? {
        if (currentContent == null) return null
        
        val node = currentCodeNodes.find { it.id == chapterId || it.id == chapterId.removePrefix("#") }
            ?: return null
        
        val nodeContent = extractNodeContent(node, currentContent!!)
        return DocumentChunk(
            documentPath = node.filePath,
            chapterTitle = buildNodeTitle(node),
            content = nodeContent,
            anchor = "#${node.id}",
            startLine = node.startLine,
            endLine = node.endLine,
            position = PositionMetadata(
                documentPath = node.filePath,
                formatType = DocumentFormatType.SOURCE_CODE,
                position = DocumentPosition.LineRange(
                    startLine = node.startLine,
                    endLine = node.endLine
                )
            )
        )
    }
    
    /**
     * Build TOC from code nodes (hierarchical: packages -> classes -> methods -> fields)
     */
    private fun buildTOCFromCodeNodes(nodes: List<CodeNode>): List<TOCItem> {
        // Group nodes by type and hierarchy
        val packages = mutableMapOf<String, MutableList<CodeNode>>()
        val topLevelClasses = mutableListOf<CodeNode>()
        
        for (node in nodes) {
            when (node.type) {
                CodeElementType.PACKAGE -> {
                    // Skip packages in TOC, we'll use them for grouping
                }
                CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM -> {
                    if (node.packageName.isNotEmpty()) {
                        packages.getOrPut(node.packageName) { mutableListOf() }.add(node)
                    } else {
                        topLevelClasses.add(node)
                    }
                }
                else -> {
                    // Methods, fields, etc. will be nested under classes
                }
            }
        }
        
        val toc = mutableListOf<TOCItem>()
        
        // Add package groups (sorted by package name)
        val sortedPackageNames = packages.keys.sorted()
        for (packageName in sortedPackageNames) {
            val classNodes = packages[packageName] ?: continue
            val packageItem = TOCItem(
                level = 1,
                title = "Package $packageName",
                anchor = "#package-${packageName.replace('.', '-')}",
                lineNumber = classNodes.minOfOrNull { node -> node.startLine },
                children = classNodes.map { classNode ->
                    buildClassTOCItem(classNode, nodes, level = 2)
                }
            )
            toc.add(packageItem)
        }
        
        // Add top-level classes (no package)
        for (classNode in topLevelClasses) {
            toc.add(buildClassTOCItem(classNode, nodes, level = 1))
        }
        
        return toc
    }
    
    /**
     * Build TOC item for a class and its members
     */
    private fun buildClassTOCItem(classNode: CodeNode, allNodes: List<CodeNode>, level: Int): TOCItem {
        val classQualifiedName = classNode.qualifiedName
        
        // Find all members of this class (methods, fields)
        val members = allNodes.filter { node ->
            node.qualifiedName.startsWith("$classQualifiedName.") &&
            node.qualifiedName.count { it == '.' } == classQualifiedName.count { it == '.' } + 1
        }
        
        val children = members.map { member ->
            val icon = when (member.type) {
                CodeElementType.METHOD, CodeElementType.FUNCTION -> "fun"
                CodeElementType.FIELD, CodeElementType.PROPERTY -> "val"
                else -> ""
            }
            TOCItem(
                level = level + 1,
                title = "$icon ${member.name}",
                anchor = "#${member.id}",
                lineNumber = member.startLine
            )
        }
        
        val classIcon = when (classNode.type) {
            CodeElementType.CLASS -> "class"
            CodeElementType.INTERFACE -> "interface"
            CodeElementType.ENUM -> "enum"
            else -> ""
        }
        
        return TOCItem(
            level = level,
            title = "$classIcon ${classNode.name}",
            anchor = "#${classNode.id}",
            lineNumber = classNode.startLine,
            children = children
        )
    }
    
    /**
     * Build entities from code nodes
     */
    private fun buildEntitiesFromCodeNodes(nodes: List<CodeNode>): List<Entity> {
        return nodes.mapNotNull { node ->
            when (node.type) {
                CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM -> {
                    Entity.ClassEntity(
                        name = node.name,
                        packageName = node.packageName,
                        location = Location(
                            anchor = "#${node.id}",
                            line = node.startLine
                        )
                    )
                }
                CodeElementType.METHOD, CodeElementType.FUNCTION -> {
                    Entity.FunctionEntity(
                        name = node.name,
                        signature = extractSignature(node),
                        location = Location(
                            anchor = "#${node.id}",
                            line = node.startLine
                        )
                    )
                }
                else -> null
            }
        }
    }
    
    /**
     * Extract node content from source code
     */
    private fun extractNodeContent(node: CodeNode, sourceCode: String): String {
        return if (node.content.isNotEmpty()) {
            node.content
        } else {
            // Fallback: extract by line numbers
            val lines = sourceCode.lines()
            if (node.startLine <= lines.size && node.endLine <= lines.size) {
                lines.subList(node.startLine - 1, node.endLine).joinToString("\n")
            } else {
                ""
            }
        }
    }
    
    /**
     * Build display title for a node
     */
    private fun buildNodeTitle(node: CodeNode): String {
        return when (node.type) {
            CodeElementType.CLASS -> "class ${node.name}"
            CodeElementType.INTERFACE -> "interface ${node.name}"
            CodeElementType.ENUM -> "enum ${node.name}"
            CodeElementType.METHOD, CodeElementType.FUNCTION -> {
                val signature = extractSignature(node)
                if (signature != null) "$signature" else node.name
            }
            else -> node.name
        }
    }
    
    /**
     * Extract method/function signature from node metadata
     */
    private fun extractSignature(node: CodeNode): String? {
        return node.metadata["signature"] ?: node.metadata["parameters"]?.let { params ->
            "${node.name}($params)"
        }
    }
    
    /**
     * Detect programming language from file extension
     */
    private fun detectLanguage(fileName: String): Language {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "java" -> Language.JAVA
            "kt", "kts" -> Language.KOTLIN
            "js" -> Language.JAVASCRIPT
            "ts", "tsx" -> Language.TYPESCRIPT
            "py" -> Language.PYTHON
            "go" -> Language.GO
            "rs" -> Language.RUST
            "cs" -> Language.CSHARP
            else -> Language.UNKNOWN
        }
    }
}
