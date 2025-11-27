package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*

/**
 * DocQL Executor for source code files
 * Handles code structure queries (classes, functions, methods)
 */
class CodeDocQLExecutor(
    documentFile: DocumentFile?,
    parserService: DocumentParserService?
) : BaseDocQLExecutor(documentFile, parserService) {

    /**
     * Execute content query - delegates to code queries for source files
     * This allows queries like $.content.heading("ClassName") to work on code files
     */
    override suspend fun executeContentQuery(nodes: List<DocQLNode>): DocQLResult {
        if (parserService == null || documentFile == null) {
            return DocQLResult.Error("No parser service available")
        }

        // For code files, content queries can be used to search code structure
        // Look for function calls
        val functionNode = nodes.firstOrNull { it is DocQLNode.FunctionCall } as? DocQLNode.FunctionCall
            ?: return DocQLResult.Error("Content query requires function call for code files")

        return when (functionNode.name) {
            "heading", "grep", "query" -> {
                // Use heading query for flexible search in code
                val chunks = parserService.queryHeading(functionNode.argument)
                if (chunks.isNotEmpty()) {
                    DocQLResult.Chunks(mapOf(documentFile.path to chunks))
                } else {
                    DocQLResult.Empty
                }
            }
            "chunks", "all" -> {
                // Get all code chunks
                val allHeadings = documentFile.toc?.let { flattenToc(it) } ?: emptyList()
                if (allHeadings.isEmpty()) {
                    return DocQLResult.Empty
                }

                val chunks = mutableListOf<DocumentChunk>()
                for (heading in allHeadings) {
                    val chunkContent = parserService.queryChapter(heading.anchor.removePrefix("#"))
                    if (chunkContent != null) {
                        chunks.add(chunkContent)
                    }
                }

                if (chunks.isEmpty()) {
                    DocQLResult.Empty
                } else {
                    DocQLResult.Chunks(mapOf(documentFile.path to chunks))
                }
            }
            else -> DocQLResult.Error("Unknown content function '${functionNode.name}' for code files")
        }
    }

    /**
     * Execute code query: $.code.classes[*], $.code.class("Name"), $.code.functions[*], etc.
     * This is for querying source code structure (classes, methods, functions) parsed by CodeDocumentParser.
     * 
     * NOTE: This query only works on SOURCE_CODE files. For other document types (markdown, pdf, etc.),
     * it returns Empty result to avoid returning irrelevant results.
     */
    override suspend fun executeCodeQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }
        
        // $.code.* queries should only work on source code files
        // Skip non-source-code files (markdown, pdf, etc.) to avoid returning irrelevant results
        if (documentFile.metadata.formatType != DocumentFormatType.SOURCE_CODE) {
            return DocQLResult.Empty
        }
        
        if (nodes.isEmpty()) {
            return DocQLResult.Error("Code query requires function call or property (e.g., $.code.classes[*], $.code.class(\"Name\"), $.code.functions[*])")
        }
        
        return when (val firstNode = nodes.firstOrNull()) {
            is DocQLNode.Property -> {
                when (firstNode.name) {
                    "classes" -> executeCodeClassesQuery(nodes.drop(1))
                    "functions" -> executeCodeFunctionsQuery(nodes.drop(1))
                    "methods" -> executeCodeMethodsQuery(nodes.drop(1))
                    else -> DocQLResult.Error("Unknown code property '${firstNode.name}'. Use: classes, functions, methods")
                }
            }

            is DocQLNode.FunctionCall -> {
                when (firstNode.name) {
                    "class" -> executeCodeClassQuery(firstNode.argument)
                    "function" -> executeCodeFunctionQuery(firstNode.argument)
                    "method" -> executeCodeMethodQuery(firstNode.argument)
                    "query" -> executeCodeCustomQuery(firstNode.argument)
                    else -> DocQLResult.Error("Unknown code function '${firstNode.name}'. Use: class(name), function(name), method(name), query(keyword)")
                }
            }

            else -> DocQLResult.Error("Expected property or function call after $.code")
        }
    }

    /**
     * Execute frontmatter query - not applicable for code files
     * Returns Empty since code files don't typically have YAML frontmatter
     */
    override suspend fun executeFrontmatterQuery(nodes: List<DocQLNode>): DocQLResult {
        // Code files don't have frontmatter, return empty
        return DocQLResult.Empty
    }

    /**
     * Execute $.code.classes[*] or $.code.classes[filter]
     */
    private fun executeCodeClassesQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        // Get class entities
        var classes = documentFile.entities.filterIsInstance<Entity.ClassEntity>()

        // Apply filters if any
        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all classes
                }

                is DocQLNode.ArrayAccess.Index -> {
                    classes = if (node.index < classes.size) {
                        listOf(classes[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    classes = classes.filter { classEntity ->
                        when (node.condition) {
                            is FilterCondition.Equals -> {
                                when (node.condition.property) {
                                    "name" -> classEntity.name == node.condition.value
                                    "package" -> classEntity.packageName == node.condition.value
                                    else -> false
                                }
                            }

                            is FilterCondition.Contains -> {
                                when (node.condition.property) {
                                    "name" -> classEntity.name.contains(node.condition.value, ignoreCase = true)
                                    "package" -> (classEntity.packageName ?: "").contains(
                                        node.condition.value,
                                        ignoreCase = true
                                    )

                                    else -> false
                                }
                            }

                            else -> false
                        }
                    }
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for classes query")
                }
            }
        }

        return if (classes.isNotEmpty()) {
            DocQLResult.Entities(mapOf(documentFile.path to classes))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute $.code.functions[*] or $.code.functions[filter]
     */
    private fun executeCodeFunctionsQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        // Get function entities
        var functions = documentFile.entities.filterIsInstance<Entity.FunctionEntity>()

        // Apply filters if any
        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all functions
                }

                is DocQLNode.ArrayAccess.Index -> {
                    functions = if (node.index < functions.size) {
                        listOf(functions[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    functions = functions.filter { funcEntity ->
                        when (node.condition) {
                            is FilterCondition.Equals -> {
                                when (node.condition.property) {
                                    "name" -> funcEntity.name == node.condition.value
                                    "signature" -> (funcEntity.signature ?: "") == node.condition.value
                                    else -> false
                                }
                            }

                            is FilterCondition.Contains -> {
                                when (node.condition.property) {
                                    "name" -> funcEntity.name.contains(node.condition.value, ignoreCase = true)
                                    "signature" -> (funcEntity.signature ?: "").contains(
                                        node.condition.value,
                                        ignoreCase = true
                                    )

                                    else -> false
                                }
                            }

                            else -> false
                        }
                    }
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for functions query")
                }
            }
        }

        return if (functions.isNotEmpty()) {
            DocQLResult.Entities(mapOf(documentFile.path to functions))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute $.code.methods[*] - alias for functions query
     */
    private fun executeCodeMethodsQuery(nodes: List<DocQLNode>): DocQLResult {
        return executeCodeFunctionsQuery(nodes)
    }

    /**
     * Execute $.code.class("ClassName") - find specific class and its content
     * 
     * Supports wildcard: $.code.class("*") returns all classes (equivalent to $.code.classes[*])
     */
    private suspend fun executeCodeClassQuery(className: String): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        // Handle wildcard "*" - return all classes (equivalent to $.code.classes[*])
        if (className == "*" || className.isEmpty()) {
            return executeCodeClassesQuery(emptyList())
        }

        if (parserService == null) {
            return DocQLResult.Error("No parser service available")
        }

        // Use heading query to find the class - CodeDocumentParser supports this
        val chunks = parserService.queryHeading(className)

        // Filter to only class-level chunks (not methods)
        val classChunks = chunks.filter { chunk ->
            val title = chunk.chapterTitle ?: ""
            title.startsWith("class ") ||
                    title.startsWith("interface ") ||
                    title.startsWith("enum ")
        }

        return if (classChunks.isNotEmpty()) {
            DocQLResult.Chunks(mapOf(documentFile.path to classChunks))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute $.code.function("functionName") - find specific function/method
     * 
     * Supports wildcard: $.code.function("*") returns all functions (equivalent to $.code.functions[*])
     */
    private suspend fun executeCodeFunctionQuery(functionName: String): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        // Handle wildcard "*" - return all functions (equivalent to $.code.functions[*])
        if (functionName == "*" || functionName.isEmpty()) {
            return executeCodeFunctionsQuery(emptyList())
        }

        return executeCodeCustomQuery(functionName)
    }

    /**
     * Execute $.code.method("methodName") - alias for function query
     */
    private suspend fun executeCodeMethodQuery(methodName: String): DocQLResult {
        return executeCodeFunctionQuery(methodName)
    }

    /**
     * Execute $.code.query("keyword") - custom query for any code element
     * 
     * Supports wildcard: $.code.query("*") returns all code chunks
     */
    private suspend fun executeCodeCustomQuery(keyword: String): DocQLResult {
        if (parserService == null || documentFile == null) {
            return DocQLResult.Error("No parser service available")
        }

        // Handle wildcard "*" - return all code chunks
        if (keyword == "*") {
            val allHeadings = documentFile.toc?.let { flattenToc(it) } ?: emptyList()

            if (allHeadings.isEmpty()) {
                return DocQLResult.Empty
            }

            // Query each heading to get its content
            val chunks = mutableListOf<DocumentChunk>()
            for (heading in allHeadings) {
                val chunkContent = parserService.queryChapter(heading.anchor.removePrefix("#"))
                if (chunkContent != null) {
                    chunks.add(chunkContent)
                }
            }

            return if (chunks.isEmpty()) {
                DocQLResult.Empty
            } else {
                DocQLResult.Chunks(mapOf(documentFile.path to chunks))
            }
        }

        // Use heading query for flexible search
        val chunks = parserService.queryHeading(keyword)

        return if (chunks.isNotEmpty()) {
            DocQLResult.Chunks(mapOf(documentFile.path to chunks))
        } else {
            DocQLResult.Empty
        }
    }
}
